// Progetto Zero — pure, isomorphic logic (used by client UI and by the server agent).

export type League = "low" | "mid" | "high";
export type ZeroAnswers = {
  age: number;
  pushups: number; // max consecutive
  plank: number; // seconds
  pullups: number; // max consecutive
  squats: number; // max consecutive
  weeklyNow: number; // how many times per week right now
  goal?: string;
  realName?: string; // how the coach calls you
  dumbbellKg?: number; // 0 = no dumbbell
  hasBar?: boolean; // pull-up bar at home
  gymDays?: number[]; // JS weekdays (0=Sun) when you go to gym/park
  _redo?: boolean;
};

export const LEAGUE_LABEL: Record<League, string> = { low: "Lega Bassa", mid: "Lega Media", high: "Lega Alta" };

/**
 * Reference values for an average 15–20 year old (fitness test norms):
 * push-ups ≈ 15–35, plank ≈ 45–120s, pull-ups ≈ 3–10, bodyweight squats ≈ 30–60.
 * Below the band → 0 points, inside → 1, above → 2.
 */
export function computeLeague(a: ZeroAnswers): League {
  const band = (v: number, lo: number, hi: number) => (v >= hi ? 2 : v >= lo ? 1 : 0);
  const canPull = a.hasBar || (a.gymDays?.length ?? 0) > 0;
  const parts = [band(a.pushups, 15, 35), band(a.plank, 45, 120), band(a.squats, 30, 60)];
  if (canPull) parts.push(band(a.pullups, 3, 10));
  const score = (parts.reduce((x, y) => x + y, 0) / parts.length) * 4;
  if (score <= 2.5) return "low";
  if (score <= 5.5) return "mid";
  return "high";
}

export function startLimitFrom(weeklyNow: number) {
  return Math.max(3, Math.min(5, Math.round(weeklyNow)));
}

// ---------- dates (local keys) ----------
export function dayKeyFromParts(y: number, m: number, d: number) {
  return `${y}-${String(m + 1).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
}
export function localKey(d: Date) {
  return dayKeyFromParts(d.getFullYear(), d.getMonth(), d.getDate());
}
export function parseKey(k: string) {
  const [y, m, d] = k.split("-").map(Number);
  return new Date(y, m - 1, d);
}
/** Monday of the week containing `d` (local). */
export function weekStart(d: Date) {
  const x = new Date(d.getFullYear(), d.getMonth(), d.getDate());
  const wd = (x.getDay() + 6) % 7;
  x.setDate(x.getDate() - wd);
  return x;
}
export function weeksBetween(a: Date, b: Date) {
  return Math.max(0, Math.floor((weekStart(b).getTime() - weekStart(a).getTime()) / (7 * 86400000)));
}
/** Weekly limit: starts at 3–5 and drops by one every two weeks until 3. */
export function weeklyLimitFor(startLimit: number, startedAt: Date | null, now = new Date(), limitSetAt: Date | string | null = null) {
  const s = Math.max(0, Math.min(5, Math.round(startLimit)));
  // Chosen by hand in settings: that's the limit, full stop (from the moment it was set).
  if (limitSetAt && now >= new Date(new Date(limitSetAt).setHours(0, 0, 0, 0))) return s;
  if (!startedAt) return s;
  return Math.max(Math.min(3, s), s - Math.floor(weeksBetween(startedAt, now) / 2));
}

// ---------- workout plan ----------
// Simple, teen-friendly exercises only: push-ups, abs, plank, dumbbell basics, a few squats.
// Pull-ups / dips / running only when the user has a bar at home or on their gym/park days.
export type Exercise = { id: string; name: string; sets: number; amount: number; unit: "rip" | "sec" | "min" | "km"; tip: string };
export type DayPlan = { key: string; rest: boolean; focus: string; title: string; exercises: Exercise[]; restTip?: string; intensity?: "leggero" | "normale" | "tosto"; gym?: boolean; autoRest?: string };

const BASE_DAYS: Record<League, number[]> = { low: [1, 3, 5], mid: [1, 3, 5], high: [1, 2, 4, 6] };
const REST_TIPS = [
  "Oggi riposo. I muscoli crescono adesso, non mentre ti alleni. Bevi tanto e dormi presto.",
  "Giorno off. Una passeggiata va benissimo, il resto lo fa il recupero.",
  "Riposo. Vai a scuola, esci con gli amici, vivi. Domani si torna a spingere.",
  "Oggi niente scheda. Se proprio vuoi muoverti, 10 minuti di stretching e basta.",
  "Recupero. Mangia bene, proteine a pranzo e cena, e stanotte niente telefono a letto.",
];

function rng(seed: string) {
  let h = 2166136261;
  for (let i = 0; i < seed.length; i++) { h ^= seed.charCodeAt(i); h = Math.imul(h, 16777619); }
  return () => {
    h += 0x6d2b79f5;
    let t = h;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/** Training weekdays (JS getDay): base days + gym days, never 3 in a row, max 5. */
export function trainingDays(league: League, a: Partial<ZeroAnswers>): number[] {
  const gym = (a.gymDays ?? []).filter((d) => d >= 0 && d <= 6);
  const set = new Set<number>([...BASE_DAYS[league], ...gym]);
  const isOn = (d: number) => set.has(((d % 7) + 7) % 7);
  for (let pass = 0; pass < 3; pass++) {
    for (let d = 0; d < 7; d++) {
      if (isOn(d) && isOn(d + 1) && isOn(d + 2)) {
        const cand = [d + 1, d, d + 2].map((x) => x % 7).find((x) => !gym.includes(x));
        if (cand !== undefined) set.delete(cand);
      }
    }
  }
  const max = league === "high" ? 5 : 4;
  for (const d of [0, 6, 3, 2, 4, 5, 1]) { if (set.size <= max) break; if (!gym.includes(d)) set.delete(d); }
  return Array.from(set).sort();
}
export function isTrainingDay(league: League, d: Date, a: Partial<ZeroAnswers> = {}) {
  return trainingDays(league, a).includes(d.getDay());
}
export function isGymDay(a: Partial<ZeroAnswers>, d: Date) { return (a.gymDays ?? []).includes(d.getDay()); }

type Tpl = { name: string; unit: Exercise["unit"]; amt: (a: ZeroAnswers, L: League) => number; tip: string };
const LV = (L: League, lo: number, mid: number, hi: number) => (L === "low" ? lo : L === "mid" ? mid : hi);
const clamp = (v: number, lo: number, hi: number) => Math.max(lo, Math.min(hi, v));

const PUSH: Tpl[] = [
  { name: "Flessioni", unit: "rip", amt: (a, L) => clamp(a.pushups * 0.5, LV(L, 5, 8, 12), LV(L, 12, 18, 25)), tip: "Corpo dritto, petto quasi a terra." },
  { name: "Flessioni larghe", unit: "rip", amt: (a, L) => clamp(a.pushups * 0.4, LV(L, 4, 6, 10), LV(L, 10, 15, 20)), tip: "Mani più larghe delle spalle: più petto." },
  { name: "Flessioni strette", unit: "rip", amt: (a, L) => clamp(a.pushups * 0.3, LV(L, 3, 5, 8), LV(L, 8, 12, 16)), tip: "Mani sotto il petto: lavorano i tricipiti." },
];
const ABS: Tpl[] = [
  { name: "Plank", unit: "sec", amt: (a, L) => clamp(a.plank * 0.5, LV(L, 20, 30, 40), LV(L, 40, 50, 70)), tip: "Addome duro, sedere in linea con la schiena." },
  { name: "Crunch", unit: "rip", amt: (_a, L) => LV(L, 12, 15, 20), tip: "Sali con la pancia, non tirare il collo." },
  { name: "Sit-up", unit: "rip", amt: (_a, L) => LV(L, 8, 12, 15), tip: "Piedi a terra, sali fino a sederti." },
  { name: "Leg raise", unit: "rip", amt: (_a, L) => LV(L, 8, 10, 14), tip: "Sdraiato, gambe tese su e giù piano." },
  { name: "Plank laterale", unit: "sec", amt: (a, L) => clamp(a.plank * 0.3, LV(L, 15, 20, 25), LV(L, 25, 35, 45)), tip: "Per lato. Fianco alto." },
];
const LEGS: Tpl[] = [
  { name: "Squat", unit: "rip", amt: (a, L) => clamp(a.squats * 0.4, LV(L, 10, 12, 15), LV(L, 15, 20, 25)), tip: "Talloni a terra, sedere indietro." },
  { name: "Affondi", unit: "rip", amt: (_a, L) => LV(L, 8, 10, 12), tip: "Per gamba. Ginocchio dietro che sfiora terra." },
];
/** Reps for a dumbbell exercise given the weight (kg), from common beginner tables. */
function dbReps(kg: number, light: number) {
  const r = kg <= light ? 15 : kg <= light * 1.6 ? 12 : kg <= light * 2.4 ? 10 : kg <= light * 3.2 ? 8 : 6;
  return r;
}
const DUMB: Tpl[] = [
  { name: "Curl bicipiti con manubrio", unit: "rip", amt: (a) => dbReps(a.dumbbellKg ?? 0, 5), tip: "Per braccio. Gomito fermo vicino al fianco." },
  { name: "Lento avanti con manubrio", unit: "rip", amt: (a) => dbReps(a.dumbbellKg ?? 0, 4), tip: "Per braccio. Spingi sopra la testa, schiena dritta." },
  { name: "Rematore con manubrio", unit: "rip", amt: (a) => dbReps(a.dumbbellKg ?? 0, 7), tip: "Per braccio. Mano e ginocchio sulla sedia, tira al fianco." },
  { name: "Squat con manubrio", unit: "rip", amt: (a) => dbReps(a.dumbbellKg ?? 0, 8), tip: "Manubrio al petto, scendi lento." },
];
const PULL: Tpl[] = [
  { name: "Trazioni", unit: "rip", amt: (a) => clamp(Math.round(a.pullups * 0.6), 1, 12), tip: "Mento sopra la sbarra, scendi controllato. Se non sali, salta su e scendi piano." },
];
const OUTDOOR: Tpl[] = [
  { name: "Corsa", unit: "min", amt: (_a, L) => LV(L, 12, 18, 25), tip: "Ritmo che ti permette di parlare. Niente sprint." },
  { name: "Corsa", unit: "km", amt: (_a, L) => LV(L, 2, 3, 4), tip: "Distanza, non velocità. Cammina se serve, ma finisci." },
  { name: "Dip alle parallele", unit: "rip", amt: (a, L) => clamp(a.pushups * 0.25, LV(L, 3, 5, 8), LV(L, 6, 10, 12)), tip: "Scendi fino a 90° coi gomiti." },
];

const INTENSITY = { leggero: 0.8, normale: 1, tosto: 1.2 } as const;

/** Deterministic daily plan + auto-rest when the user already trained a lot. */
export function planForDay(d: Date, league: League, a: ZeroAnswers, assessedAt: Date | null, seed: string, doneKeys?: Set<string>): DayPlan {
  const key = localKey(d);
  const r0 = rng(`${seed}:${key}:rest`);
  const restPlan = (autoRest?: string): DayPlan => ({ key, rest: true, focus: "rest", title: "Riposo", exercises: [], restTip: autoRest ?? REST_TIPS[Math.floor(r0() * REST_TIPS.length)], autoRest });
  if (doneKeys) {
    const back = (n: number) => localKey(new Date(d.getFullYear(), d.getMonth(), d.getDate() - n));
    if (doneKeys.has(back(1)) && doneKeys.has(back(2))) return restPlan("Ti sei allenato due giorni di fila: oggi riposo obbligato. Il recupero è metà dell'allenamento.");
    let w = 0; for (let i = 1; i <= 6; i++) if (doneKeys.has(back(i))) w++;
    if (w >= 5) return restPlan("Cinque allenamenti negli ultimi giorni. Oggi stop, senza discutere.");
  }
  if (!isTrainingDay(league, d, a)) return restPlan();
  const r = rng(`${seed}:${key}`);
  const gym = isGymDay(a, d);
  const hasDb = (a.dumbbellKg ?? 0) > 0;
  const canPull = gym || !!a.hasBar;
  const roll = r();
  const intensity: DayPlan["intensity"] = roll < 0.3 ? "leggero" : roll < 0.8 ? "normale" : "tosto";
  const weeks = assessedAt ? weeksBetween(assessedAt, d) : 0;
  const prog = 1 + Math.min(0.3, weeks * 0.04);
  const pick = <T,>(arr: T[]) => arr[Math.floor(r() * arr.length)];
  const shuffled = <T,>(arr: T[]) => [...arr].sort(() => r() - 0.5);

  let title: string; let list: Tpl[];
  if (gym) {
    title = "Parco / Palestra";
    list = [PULL[0], pick(OUTDOOR.slice(0, 2)), OUTDOOR[2], pick(ABS)];
  } else {
    const types = ["push", "abs", "mix", ...(hasDb ? ["arms"] : [])];
    const t = types[Math.floor(r() * types.length)];
    if (t === "push") { title = "Flessioni & addome"; list = [...shuffled(PUSH).slice(0, 2), ...shuffled(ABS).slice(0, 2)]; }
    else if (t === "abs") { title = "Addome & core"; list = [...shuffled(ABS).slice(0, 3), pick(LEGS)]; }
    else if (t === "arms") { title = "Pesi"; list = [...shuffled(DUMB).slice(0, 3), pick(PUSH)]; }
    else { title = "Total body"; list = [pick(PUSH), pick(ABS), pick(LEGS), hasDb ? pick(DUMB) : pick(ABS.filter((x) => x.name !== "Plank"))]; }
    if (canPull && r() < 0.35) list[list.length - 1] = PULL[0];
  }
  const n = intensity === "leggero" ? 3 : intensity === "tosto" ? (league === "low" ? 4 : 5) : (league === "low" ? 3 : 4);
  if (intensity === "tosto" && list.length < n) list.push(pick(PUSH));
  list = list.slice(0, n);
  const sets = league === "low" ? 2 : 3;
  const mult = INTENSITY[intensity] * prog;
  const seen = new Set<string>();
  const exercises = list.filter((t) => { const k = t.name + t.unit; if (seen.has(k)) return false; seen.add(k); return true; }).map((t, i) => {
    const raw = t.amt(a, league) * (t.unit === "min" || t.unit === "km" ? INTENSITY[intensity] : mult);
    const amount = t.unit === "sec" ? Math.max(15, Math.round(raw / 5) * 5) : t.unit === "km" ? Math.max(1, Math.round(raw * 2) / 2) : Math.max(1, Math.round(raw));
    const s = t.unit === "min" || t.unit === "km" ? 1 : sets;
    return { id: `${key}:${i}`, name: t.name, sets: s, amount, unit: t.unit, tip: t.tip };
  });
  return { key, rest: false, focus: title, title, exercises, intensity, gym };
}

// ---------- themes ----------
export const ZERO_THEMES = [
  { id: "obsidian", name: "Ossidiana", colors: ["#0b0b0d", "#1a1a1f", "#e9c46a", "#f4f1e8"] },
  { id: "forest", name: "Foresta", colors: ["#07140f", "#10261c", "#7ee08a", "#e7f5ea"] },
  { id: "abyss", name: "Abisso", colors: ["#04101a", "#0b2232", "#3ee0d0", "#e6f7f9"] },
  { id: "ember", name: "Brace", colors: ["#140806", "#2a110c", "#ff7a45", "#fbeee8"] },
  { id: "steel", name: "Acciaio", colors: ["#0e1114", "#1c2126", "#c7d2dc", "#f2f5f7"] },
] as const;
export type ZeroThemeId = (typeof ZERO_THEMES)[number]["id"];
