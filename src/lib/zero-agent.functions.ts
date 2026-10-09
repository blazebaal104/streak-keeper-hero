import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { requireSupabaseAuth } from "@/integrations/supabase/auth-middleware";
import {
  computeLeague, isTrainingDay, localKey, planForDay, weekStart, weeklyLimitFor,
  LEAGUE_LABEL, type League, type ZeroAnswers,
} from "./sbo-zero";

const Input = z.object({ tzOffset: z.number().min(-900).max(900), reason: z.enum(["open", "event", "quit", "workout"]) });

type Flags = Record<string, unknown> & {
  dailyDay?: string; reportWeek?: string; atLimitWeek?: string; rant?: Record<string, number>;
  overStreak?: number; missedDay?: string; reassessDay?: string; weekOne?: boolean; tipCounter?: number;
  aiWeek?: string; aiCount?: number;
};

const AI_PER_USER_WEEK = 6;
const AI_GLOBAL_DAY = 150;

export const agentTick = createServerFn({ method: "POST" })
  .middleware([requireSupabaseAuth])
  .inputValidator((d) => Input.parse(d))
  .handler(async ({ data, context }) => {
    const { supabase, userId } = context;
    const { supabaseAdmin } = await import("@/integrations/supabase/client.server");
    const { phrases, aiWrite } = await import("./zero-agent.server");
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const admin = supabaseAdmin as any;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const sb = supabase as any;

    const { data: pz } = await sb.from("project_zero").select("*").eq("user_id", userId).maybeSingle();
    if (!pz || !pz.started_at) return { sent: 0 };
    if (!pz.active && data.reason !== "quit") return { sent: 0 };

    const tz = data.tzOffset * 60000;
    const shift = (t: number | string) => new Date(new Date(t).getTime() - tz);
    const now = shift(Date.now());
    const today = localKey(now);
    const ws = weekStart(now);
    const weekKey = localKey(ws);
    const started = shift(pz.started_at);
    const days = Math.floor((now.getTime() - new Date(started.getFullYear(), started.getMonth(), started.getDate()).getTime()) / 86400000) + 1;
    const answers = pz.answers as ZeroAnswers;
    const league = (pz.league ?? computeLeague(answers)) as League;
    const limit = weeklyLimitFor(pz.start_limit, started, now);

    const { data: prof } = await sb.from("profiles").select("username").eq("id", userId).maybeSingle();
    const name: string = (pz.answers as ZeroAnswers)?.realName?.trim() || prof?.username || "campione";

    const since = new Date(ws.getTime() - 7 * 86400000 + tz).toISOString();
    const { data: evs } = await sb.from("events").select("ts").eq("user_id", userId).gte("ts", since);
    const evKeys: string[] = (evs ?? []).map((e: { ts: string }) => localKey(shift(e.ts)));
    const count = evKeys.filter((k) => k >= weekKey).length;
    const prevWs = new Date(ws.getTime() - 7 * 86400000);
    const prevKey = localKey(prevWs);
    const prevCount = evKeys.filter((k) => k >= prevKey && k < weekKey).length;

    const { data: wds } = await sb.from("workout_days").select("day,completed").eq("user_id", userId).gte("day", prevKey);
    const doneSet = new Set((wds ?? []).filter((w: { completed: boolean }) => w.completed).map((w: { day: string }) => w.day));

    const { data: st } = await admin.from("agent_state").select("*").eq("user_id", userId).maybeSingle();
    const used = new Set<string>(st?.used_keys ?? []);
    const flags: Flags = { ...(st?.flags ?? {}) };
    const out: { kind: string; content: string }[] = [];

    const plan = planForDay(now, league, answers, pz.last_assessment_at ? shift(pz.last_assessment_at) : started, userId);
    const ctx = {
      name, day: days, focus: plan.title.toLowerCase(), n: plan.exercises.length, league: LEAGUE_LABEL[league],
      rest: plan.rest, count, limit, overBy: Math.max(0, count - limit), overStreak: Number(flags.overStreak ?? 0), days,
      done: 0, planned: 0,
    };

    async function tryAi(prompt: string): Promise<string[] | null> {
      const aiCount = flags.aiWeek === weekKey ? Number(flags.aiCount ?? 0) : 0;
      if (aiCount >= AI_PER_USER_WEEK) return null;
      const utcDay = new Date().toISOString().slice(0, 10);
      const { data: b } = await admin.from("ai_budget").select("*").eq("id", 1).maybeSingle();
      if (b?.paused_until && new Date(b.paused_until).getTime() > Date.now()) return null;
      const calls = b?.day === utcDay ? b.calls : 0;
      if (calls >= AI_GLOBAL_DAY) return null;
      const key = process.env.LOVABLE_API_KEY;
      if (!key) return null;
      await admin.from("ai_budget").upsert({ id: 1, day: utcDay, calls: calls + 1, paused_until: null });
      flags.aiWeek = weekKey;
      flags.aiCount = aiCount + 1;
      const r = await aiWrite(prompt, key);
      if (r.ok) return r.messages;
      // Credits/policy → pause a day; rate limit → pause an hour. Phrases take over meanwhile.
      const pauseMs = r.status === 402 || r.status === 403 ? 24 * 3600e3 : r.status === 429 ? 3600e3 : 0;
      if (pauseMs) await admin.from("ai_budget").update({ paused_until: new Date(Date.now() + pauseMs).toISOString() }).eq("id", 1);
      return null;
    }
    const facts = `Utente: ${name}. Giorno ${days} del Progetto Zero. ${LEAGUE_LABEL[league]}. Limite settimanale attuale: ${limit}. Questa settimana: ${count}. Settimane consecutive sopra il limite prima di questa: ${ctx.overStreak}.`;

    if (data.reason === "workout") {
      out.push({ kind: "praise", content: phrases.praise(used, ctx) });
    } else if (data.reason === "quit") {
      const ai = await tryAi(`${facts}\nL'utente ha appena DISATTIVATO il Progetto Zero dopo ${days} giorni. Sei furioso: scrivi 2-3 messaggi in maiuscolo, volgari, per convincerlo a riattivarlo subito.`);
      (ai ?? [phrases.quit(ctx)]).forEach((c) => out.push({ kind: "quit", content: c }));
    } else {
      // Weekly report for the previous week
      if (flags.reportWeek !== weekKey) {
        if (started < ws) {
          let planned = 0, done = 0;
          for (let i = 0; i < 7; i++) {
            const d = new Date(prevWs.getTime() + i * 86400000);
            if (d < new Date(started.getFullYear(), started.getMonth(), started.getDate())) continue;
            if (isTrainingDay(league, d, answers)) { planned++; if (doneSet.has(localKey(d))) done++; }
          }
          const prevLimit = weeklyLimitFor(pz.start_limit, started, prevWs);
          flags.overStreak = prevCount > prevLimit ? Number(flags.overStreak ?? 0) + 1 : 0;
          const rc = { ...ctx, count: prevCount, limit: prevLimit, done, planned };
          const ai = await tryAi(`${facts}\nResoconto della settimana appena finita: ${prevCount} volte su limite ${prevLimit}; allenamenti fatti ${done} su ${planned}. Nuovo limite per questa settimana: ${limit}. Scrivi 1-3 messaggi di resoconto personale. Se ha sforato il limite sii furioso in maiuscolo; se è stato sotto il limite e si è allenato, complimentati ma alza l'asticella; se ha raggiunto esattamente il limite, rimproveralo un po'.`);
          (ai ?? [phrases.report(rc)]).forEach((c) => out.push({ kind: "report", content: c }));
        }
        flags.reportWeek = weekKey;
      }
      // Morning briefing
      if (flags.dailyDay !== today) {
        out.push({ kind: "daily", content: phrases.daily(used, ctx) });
        flags.tipCounter = Number(flags.tipCounter ?? 0) + 1;
        if (flags.tipCounter % 2 === 0) { const t = phrases.tip(used); if (t) out.push({ kind: "tip", content: t }); }
        flags.dailyDay = today;
        // Missed yesterday?
        const y = new Date(now.getTime() - 86400000);
        const yk = localKey(y);
        if (flags.missedDay !== yk && y >= new Date(started.getFullYear(), started.getMonth(), started.getDate()) && isTrainingDay(league, y, answers) && !doneSet.has(yk)) {
          out.push({ kind: "missed", content: phrases.missed(used, ctx) });
          flags.missedDay = yk;
        }
        if (days >= 8 && !flags.weekOne) { out.push({ kind: "milestone", content: phrases.weekOne(ctx) }); flags.weekOne = true; }
        const assessed = pz.last_assessment_at ? new Date(pz.last_assessment_at).getTime() : 0;
        if (Date.now() - assessed > 30 * 86400e3 && (!flags.reassessDay || Date.now() - new Date(String(flags.reassessDay)).getTime() > 3 * 86400e3)) {
          out.push({ kind: "reassess", content: phrases.reassess(ctx) });
          flags.reassessDay = new Date().toISOString();
        }
      }
      // Limit reached / exceeded
      const rant = { ...(flags.rant ?? {}) };
      const alc = (flags.atLimitWeek === weekKey ? Number(flags.alCount ?? 0) : 0);
      if (data.reason === "event" && count === limit && alc < 3) {
        flags.alCount = alc + 1;
        [phrases.atLimit(used, ctx)].forEach((c) => out.push({ kind: "limit", content: c }));
        flags.atLimitWeek = weekKey;
      }
      if (data.reason === "event" && count > limit) {
        const firstRant = (rant[weekKey] ?? 0) === 0;
        const ai = firstRant ? await tryAi(`${facts}\nHa SUPERATO il limite: ${count} su ${limit} (sforato di ${count - limit}). Sei incazzatissimo: spammalo con 3-5 messaggi brevi TUTTI IN MAIUSCOLO, volgari, come una persona vera furiosa. Chiudi dicendo che la prossima settimana non vuoi vederlo o torni il doppio più incazzato.${ctx.overStreak > 0 ? " È recidivo: sii il doppio più incazzato e ricordaglielo." : ""}`) : null;
        (ai ?? phrases.rant(used, ctx)).forEach((c) => out.push({ kind: "rant", content: c }));
        rant[weekKey] = (rant[weekKey] ?? 0) + 1;
        flags.rant = Object.fromEntries(Object.entries(rant).slice(-6));
      }
    }

    if (out.length) {
      const base = Date.now();
      await admin.from("agent_messages").insert(out.map((m, i) => ({ user_id: userId, kind: m.kind, content: m.content, created_at: new Date(base + i * 1000).toISOString() })));
    }
    await admin.from("agent_state").upsert({ user_id: userId, used_keys: Array.from(used).slice(-5000), flags, updated_at: new Date().toISOString() });
    return { sent: out.length };
  });
