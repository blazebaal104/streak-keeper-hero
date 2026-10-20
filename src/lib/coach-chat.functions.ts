import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { requireSupabaseAuth } from "@/integrations/supabase/auth-middleware";
import { LEAGUE_LABEL, localKey, planForDay, weekStart, weeklyLimitFor, type League, type ZeroAnswers } from "./sbo-zero";

const Input = z.object({ text: z.string().trim().min(1).max(1000), tzOffset: z.number().min(-900).max(900) });

export const CHAT_PER_DAY = 30;

export const coachChat = createServerFn({ method: "POST" })
  .middleware([requireSupabaseAuth])
  .inputValidator((d) => Input.parse(d))
  .handler(async ({ data, context }) => {
    const { supabase, userId } = context;
    const { supabaseAdmin } = await import("@/integrations/supabase/client.server");
    const { geminiReply, fallbackReply } = await import("./coach-chat.server");
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const admin = supabaseAdmin as any;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const sb = supabase as any;

    const tz = data.tzOffset * 60000;
    const shift = (t: number | string) => new Date(new Date(t).getTime() - tz);
    const now = shift(Date.now());
    const dayStartUtc = new Date(new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime() + tz).toISOString();

    // Daily cap per person, so a burst of messages can't eat everyone's free quota.
    const { count: sentToday } = await admin.from("agent_messages").select("id", { count: "exact", head: true })
      .eq("user_id", userId).eq("kind", "user").gte("created_at", dayStartUtc);
    if ((sentToday ?? 0) >= CHAT_PER_DAY) {
      return { ok: false as const, reason: "limit" as const, left: 0 };
    }

    const nowIso = new Date().toISOString();
    await admin.from("agent_messages").insert({ user_id: userId, kind: "user", content: data.text, created_at: nowIso, read_at: nowIso });

    // Context for the coach
    const { data: pz } = await sb.from("project_zero").select("*").eq("user_id", userId).maybeSingle();
    const { data: prof } = await sb.from("profiles").select("username").eq("id", userId).maybeSingle();
    const answers = (pz?.answers ?? {}) as ZeroAnswers;
    const name: string = answers?.realName?.trim() || prof?.username || "campione";
    let ctx = `Nome: ${name}.`;
    if (pz?.active && pz.started_at) {
      const started = shift(pz.started_at);
      const days = Math.floor((now.getTime() - new Date(started.getFullYear(), started.getMonth(), started.getDate()).getTime()) / 86400000) + 1;
      const ws = weekStart(now);
      const weekKey = localKey(ws);
      const limit = weeklyLimitFor(pz.start_limit, started, now, pz.limit_set_at ? shift(pz.limit_set_at) : null);
      const { data: evs } = await sb.from("events").select("ts").eq("user_id", userId).gte("ts", new Date(ws.getTime() + tz).toISOString());
      const count = (evs ?? []).length;
      const { data: wds } = await sb.from("workout_days").select("day,completed,custom").eq("user_id", userId).gte("day", weekKey);
      const workoutsWeek = (wds ?? []).filter((w: { completed: boolean; custom: unknown[] }) => w.completed || (w.custom?.length ?? 0) > 0).length;
      const todayDone = (wds ?? []).some((w: { day: string; completed: boolean }) => w.day === localKey(now) && w.completed);
      const league = (pz.league ?? "low") as League;
      const plan = planForDay(now, league, answers, pz.last_assessment_at ? shift(pz.last_assessment_at) : started, userId);
      const planText = plan.rest ? "oggi è giorno di riposo" : `oggi: ${plan.title} (${plan.exercises.map((e) => `${e.name} ${e.sets}x${e.amount}${e.unit === "rip" ? "" : " " + e.unit}`).join(", ")})${todayDone ? ", già completato" : ", non ancora fatto"}`;
      ctx += ` Giorno ${days} del Progetto Zero. ${LEAGUE_LABEL[league]}. Eventi questa settimana: ${count} su limite ${limit}. Allenamenti fatti questa settimana: ${workoutsWeek}. Piano: ${planText}.`;
    } else {
      ctx += " Il Progetto Zero non è attivo: invitalo ad attivarlo dalle impostazioni se ha senso.";
    }
    const hhmm = now.toISOString().slice(11, 16);
    ctx += ` Ora locale: ${hhmm}, ${now.toLocaleDateString("it-IT", { weekday: "long", timeZone: "UTC" })}.`;

    // Recent conversation (automatic coach messages included, so it knows what it already said)
    const { data: hist } = await admin.from("agent_messages").select("kind,content,created_at").eq("user_id", userId)
      .order("created_at", { ascending: false }).limit(24);
    const history = (hist ?? []).reverse().map((m: { kind: string; content: string }) => ({ role: m.kind === "user" ? "user" as const : "model" as const, text: m.content }));

    const key = process.env.GEMINI_API_KEY;
    const r = key ? await geminiReply(key, ctx, history) : { ok: false as const, status: 0 };
    if (!r.ok) console.warn("[coach-chat] gemini failed", r.status);
    const reply = r.ok ? r.text : fallbackReply();
    const replyIso = new Date(Date.now() + 500).toISOString();
    await admin.from("agent_messages").insert({ user_id: userId, kind: "chat", content: reply, created_at: replyIso, read_at: replyIso });
    return { ok: true as const, ai: r.ok, left: CHAT_PER_DAY - (sentToday ?? 0) - 1 };
  });
