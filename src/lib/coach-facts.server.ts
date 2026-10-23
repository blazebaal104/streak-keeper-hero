// Server-only: the real numbers Coach Zero is given before every reply, read from the database right now.
// The coach must trust these over anything the user claims ("l'ho riacceso", "oggi zero", ...).
import { badgeDef } from "./sbo-badges";
import { LEAGUE_LABEL, localKey, weekStart, weeklyLimitFor, type League } from "./sbo-zero";

const DAY = 86400000;

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export async function coachFacts(admin: any, userId: string, tzOffsetMin: number): Promise<string> {
  const tz = tzOffsetMin * 60000;
  const shift = (t: number | string) => new Date(new Date(t).getTime() - tz);
  const now = shift(Date.now());
  const todayKey = localKey(now);
  const keyOf = (ts: string) => localKey(shift(ts));
  const dayKey = (d: number) => localKey(new Date(d));

  const [{ data: prof }, { data: pz }, { data: evs }, { data: badges }, { data: wds }] = await Promise.all([
    admin.from("profiles").select("username,daily_limit,use_adaptive_limit,created_at").eq("id", userId).maybeSingle(),
    admin.from("project_zero").select("active,started_at,updated_at,start_limit,limit_set_at,league").eq("user_id", userId).maybeSingle(),
    admin.from("events").select("ts").eq("user_id", userId).order("ts", { ascending: false }).limit(5000),
    admin.from("badges").select("category,threshold").eq("user_id", userId),
    admin.from("workout_days").select("day,completed,custom").eq("user_id", userId).order("day", { ascending: false }).limit(400),
  ]);
  const ts: string[] = (evs ?? []).map((e: { ts: string }) => e.ts);
  const byDay = new Map<string, number>();
  ts.forEach((t) => { const k = keyOf(t); byDay.set(k, (byDay.get(k) ?? 0) + 1); });
  const n = (k: string) => byDay.get(k) ?? 0;
  const base = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();

  // fire: consecutive days with >= 1 event, ending today or yesterday (24h grace, like the app)
  let fire = 0;
  let cur = n(todayKey) ? base : n(dayKey(base - DAY)) ? base - DAY : -1;
  while (cur >= 0 && n(dayKey(cur)) && fire < 3650) { fire++; cur -= DAY; }
  // ice: days clean after the 24h grace
  let ice = 0;
  if (ts.length) {
    const last = shift(ts[0]);
    const lastDay = new Date(last.getFullYear(), last.getMonth(), last.getDate()).getTime();
    const since = Math.round((base - lastDay) / DAY);
    ice = since <= 1 ? 0 : since - 1;
  }
  const ws = weekStart(now).getTime();
  const sum = (from: number, to: number) => { let s = 0; for (let d = from; d < to; d += DAY) s += n(dayKey(d)); return s; };
  const thisWeek = sum(ws, base + DAY);
  const lastWeek = sum(ws - 7 * DAY, ws);
  const last7 = sum(base - 6 * DAY, base + DAY);
  const last30 = sum(base - 29 * DAY, base + DAY);
  const recent14 = sum(base - 13 * DAY, base + DAY);
  const adaptive = recent14 < 5 ? (prof?.daily_limit ?? 3) : Math.max(1, Math.round(recent14 / 14));
  const dailyLimit = prof?.use_adaptive_limit ? adaptive : (prof?.daily_limit ?? 3);
  const lastAgo = ts.length ? Math.round((Date.now() - new Date(ts[0]).getTime()) / 60000) : null;
  const ago = (m: number) => (m < 60 ? `${m} minuti fa` : m < 48 * 60 ? `${Math.round(m / 60)} ore fa` : `${Math.round(m / 1440)} giorni fa`);

  const medals = (badges ?? []).map((b: { category: string; threshold: number }) => badgeDef(b.category as never, b.threshold)?.name).filter(Boolean);
  const workoutsTotal = (wds ?? []).filter((w: { completed: boolean; custom?: unknown[] }) => w.completed || (w.custom?.length ?? 0) > 0).length;
  const workoutsWeek = (wds ?? []).filter((w: { day: string; completed: boolean; custom?: unknown[] }) => w.day >= localKey(new Date(ws)) && (w.completed || (w.custom?.length ?? 0) > 0)).length;

  const lines: string[] = [];
  if (pz?.active && pz.started_at) {
    const started = shift(pz.started_at);
    const day = Math.floor((base - new Date(started.getFullYear(), started.getMonth(), started.getDate()).getTime()) / DAY) + 1;
    const limit = weeklyLimitFor(pz.start_limit, started, now, pz.limit_set_at ? shift(pz.limit_set_at) : null);
    lines.push(`Progetto Zero: ATTIVO (giorno ${day}, ${LEAGUE_LABEL[(pz.league ?? "low") as League]}). Limite settimanale ${limit}, questa settimana ${thisWeek}/${limit}${thisWeek > limit ? ` (SFORATO di ${thisWeek - limit})` : thisWeek === limit ? " (limite raggiunto)" : ""}.`);
  } else if (pz?.started_at) {
    lines.push(`Progetto Zero: SPENTO (disattivato${pz.updated_at ? " " + ago(Math.round((Date.now() - new Date(pz.updated_at).getTime()) / 60000)) : ""}). Non è stato riacceso: se dice di averlo riacceso, NON è vero.`);
  } else {
    lines.push("Progetto Zero: mai attivato.");
  }
  lines.push(`Eventi: oggi ${n(todayKey)} (limite giornaliero ${dailyLimit}${prof?.use_adaptive_limit ? ", adattivo" : ""}), ieri ${n(dayKey(base - DAY))}, questa settimana ${thisWeek}, settimana scorsa ${lastWeek}, ultimi 7 giorni ${last7}, ultimi 30 giorni ${last30}, totale ${ts.length}.`);
  lines.push(`Ultimo evento: ${lastAgo === null ? "mai" : ago(lastAgo)}.`);
  lines.push(`Streak di fuoco (giorni di fila con almeno un evento): ${fire}. Streak di ghiaccio (giorni puliti di fila): ${ice}.`);
  lines.push(`Allenamenti: ${workoutsWeek} questa settimana, ${workoutsTotal} in totale.`);
  lines.push(`Medaglie: ${medals.length}${medals.length ? " (" + medals.slice(-6).join(", ") + ")" : ""}.`);
  return lines.join("\n");
}

export const TRUTH_RULE = `DATI REALI: i numeri qui sotto sono letti dal database in questo preciso momento, dopo ogni messaggio. Sono l'unica verità.
- Se l'utente dice qualcosa che li contraddice ("l'ho riacceso", "oggi non ho fatto niente", "sono a 10 giorni di streak"), non credergli: smentiscilo coi numeri, col tuo tono. Se dice di averlo appena fatto, rispondi che dai tuoi dati non risulta.
- Non dire mai che ha fatto, attivato o raggiunto qualcosa se i dati non lo dicono. Usa i numeri veri quando ti servono (confronti con ieri e con la settimana scorsa, streak, limite).
- Quello che hai scritto tu nei messaggi precedenti può essere superato: conta lo stato attuale qui sotto.`;
