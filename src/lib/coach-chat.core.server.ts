// Server-only core of the server function (also called by the native app route).
import { z } from "zod";
import { LEAGUE_LABEL, localKey, planForDay, weekStart, weeklyLimitFor, type League, type ZeroAnswers } from "./sbo-zero";

export const Input = z.object({ text: z.string().trim().min(1).max(1000), tzOffset: z.number().min(-900).max(900) });

export const CHAT_PER_DAY = 30;


export async function coachChatCore(context: { supabase: unknown; userId: string }, data: z.infer<typeof Input>) {
    const { supabase, userId } = context;
    const { supabaseAdmin } = await import("@/integrations/supabase/client.server");
    const { geminiReply, fallbackReply } = await import("./coach-chat.server");
    type Mood = "FURIOSO" | "SEVERO" | "NORMALE" | "FIERO";
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
      ctx += " Il Progetto Zero NON è attivo in questo momento (vedi DATI REALI): se dice di averlo acceso non è vero; invitalo ad attivarlo dalle impostazioni se ha senso.";
    }
    // the real numbers, fresh from the database: the coach trusts these over what the user says
    const { coachFacts, TRUTH_RULE } = await import("./coach-facts.server");
    ctx += `\n\n${TRUTH_RULE}\n${await coachFacts(admin, userId, data.tzOffset)}\n`;
    const hhmm = now.toISOString().slice(11, 16);
    ctx += ` Ora locale: ${hhmm}, ${now.toLocaleDateString("it-IT", { weekday: "long", timeZone: "UTC" })}.`;

    // Recent conversation (automatic coach messages included, so it knows what it already said)
    const { data: hist } = await admin.from("agent_messages").select("kind,content,created_at").eq("user_id", userId)
      .order("created_at", { ascending: false }).limit(30);
    const recent = (hist ?? []).reverse() as { kind: string; content: string; created_at: string }[];
    const history = recent.map((m) => ({ role: m.kind === "user" ? "user" as const : "model" as const, text: m.content }));

    // Mood carries over between messages: saved after each reply; otherwise derived from the coach's last automatic messages.
    const { data: st } = await admin.from("agent_state").select("flags").eq("user_id", userId).maybeSingle();
    const flags = { ...(st?.flags ?? {}) } as Record<string, unknown>;
    const lastCoach = [...recent].reverse().find((m) => m.kind !== "user");
    const angryKinds = ["rant", "quit"];
    const severeKinds = ["limit", "missed"];
    let mood: Mood = "NORMALE";
    let why = "nessun motivo particolare";
    const saved = flags.chatMood as { mood?: Mood; at?: string; why?: string } | undefined;
    const lastCoachAt = lastCoach ? new Date(lastCoach.created_at).getTime() : 0;
    if (saved?.mood && saved.at && new Date(saved.at).getTime() >= lastCoachAt - 2000) {
      mood = saved.mood; why = saved.why || "come eri rimasto nell'ultimo messaggio";
    } else if (lastCoach && angryKinds.includes(lastCoach.kind)) {
      mood = "FURIOSO"; why = "gli hai appena fatto una sfuriata nei messaggi qui sopra (ha sgarrato) e non ti ha ancora dato nessun motivo per calmarti";
    } else if (lastCoach && severeKinds.includes(lastCoach.kind)) {
      mood = "SEVERO"; why = "l'ultimo tuo messaggio era un richiamo (limite raggiunto o allenamento saltato)";
    } else if (lastCoach && lastCoach.kind === "praise") {
      mood = "FIERO"; why = "ha appena completato l'allenamento";
    }
    // A mood older than a day fades one step towards normal.
    if (lastCoachAt && Date.now() - lastCoachAt > 24 * 3600e3 && (mood === "FURIOSO" || mood === "SEVERO")) {
      mood = mood === "FURIOSO" ? "SEVERO" : "NORMALE"; why += " (è passato più di un giorno, sei un po' meno caldo ma non hai dimenticato)";
    }
    ctx += `\nIl tuo umore attuale: ${mood}. Motivo: ${why}.`;

    const key = process.env.GEMINI_API_KEY;
    const r = key ? await geminiReply(key, ctx, history, mood) : { ok: false as const, status: 0 };
    if (!r.ok) console.warn("[coach-chat] gemini failed", r.status);
    const newMood: Mood = r.ok ? r.mood : mood;
    const replies = r.ok ? r.messages : [fallbackReply()];
    const kind = newMood === "FURIOSO" ? "rant" : "chat";
    const base = Date.now() + 500;
    await admin.from("agent_messages").insert(replies.map((content, i) => {
      const at = new Date(base + i * 1200).toISOString();
      return { user_id: userId, kind, content, created_at: at, read_at: at };
    }));
    if (r.ok) {
      flags.chatMood = { mood: newMood, at: new Date(base + replies.length * 1200).toISOString(), why: newMood === mood ? why : `nell'ultimo scambio sei passato da ${mood} a ${newMood}` };
      await admin.from("agent_state").upsert({ user_id: userId, flags, updated_at: new Date().toISOString() });
    }
    return { ok: true as const, ai: r.ok, left: CHAT_PER_DAY - (sentToday ?? 0) - 1 };
}
