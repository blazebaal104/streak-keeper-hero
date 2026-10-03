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
};

export const LEAGUE_LABEL: Record<League, string> = { low: "Lega Bassa", mid: "Lega Media", high: "Lega Alta" };

/**
 * Reference values for an average 15–20 year old (fitness test norms):
 * push-ups ≈ 15–35, plank ≈ 45–120s, pull-ups ≈ 3–10, bodyweight squats ≈ 30–60.
 * Below the band → 0 points, inside → 1, above → 2.
 */
export function computeLeague(a: ZeroAnswers): League {
  const band = (v: number, lo: number, hi: number) => (v >= hi ? 2 : v >= lo ? 1 : 0);
  const score = band(a.pushups, 15, 35) + band(a.plank, 45, 120) + band(a.pullups, 3, 10) + band(a.squats, 30, 60);
  if (score <= 2) return "low";
  if (score <= 5) return "mid";
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
export function weeklyLimitFor(startLimit: number, startedAt: Date | null, now = new Date()) {
  const s = Math.max(3, Math.min(5, startLimit));
  if (!startedAt) return s;
  return Math.max(3, s - Math.floor(weeksBetween(startedAt, now) / 2));
}

// ---------- workout plan ----------
export type Exercise = { id: string; name: string; sets: number; amount: number; unit: "rip" | "sec"; tip: string };
export type DayPlan = { key: string; rest: boolean; focus: string; title: string; exercises: Exercise[]; restTip?: string };

const TRAIN_DAYS: Record<League, number[]> = {
  low: [1, 2, 4, 6],
  mid: [1, 2, 4, 5, 6],
  high: [1, 2, 3, 5, 6],
};
const FOCUS = ["push", "legs", "core", "pull", "full"] as const;
type Focus = (typeof FOCUS)[number];
const FOCUS_TITLE: Record<Focus, string> = {
  push: "Petto & spinta",
  legs: "Gambe di ferro",
  core: "Core d'acciaio",
  pull: "Schiena & trazione",
  full: "Total body",
};
const REST_TIPS = [
  "Giorno di riposo: i muscoli crescono adesso, non durante l'allenamento. Cammina 20 minuti e bevi tanta acqua.",
  "Riposo. Stretching leggero 10 minuti prima di dormire e niente schermo nell'ultima mezz'ora.",
  "Oggi si recupera. Vai a scuola, lavora, cammina — ma niente sforzi. Domani si spinge.",
  "Riposo attivo: una passeggiata all'aria aperta e 8 ore di sonno. È parte del piano, non una pausa dal piano.",
  "Giornata off. Mangia proteine, dormi bene. Il recupero è allenamento invisibile.",
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

type Tpl = { name: string; unit: "rip" | "sec"; base: (a: ZeroAnswers, L: League) => number; tip: string; min?: League };
const LV = (L: League, lo: number, mid: number, hi: number) => (L === "low" ? lo : L === "mid" ? mid : hi);
const POOL: Record<Focus, Tpl[]> = {
  push: [
    { name: "Flessioni", unit: "rip", base: (a, L) => Math.max(LV(L, 5, 12, 20), a.pushups * 0.6), tip: "Corpo dritto come una tavola, petto quasi a terra." },
    { name: "Flessioni sulle ginocchia", unit: "rip", base: (a) => Math.max(8, a.pushups * 0.9), tip: "Perfette per finire il muscolo senza perdere la forma." },
    { name: "Dip su sedia", unit: "rip", base: (a, L) => Math.max(LV(L, 6, 10, 15), a.pushups * 0.5), tip: "Gomiti indietro, scendi fino a 90°." },
    { name: "Flessioni diamante", unit: "rip", base: (a) => Math.max(6, a.pushups * 0.4), tip: "Mani a diamante sotto il petto: tricipiti in fiamme.", min: "mid" },
    { name: "Pike push-up", unit: "rip", base: (a) => Math.max(6, a.pushups * 0.3), tip: "Sedere in alto, testa verso terra: spalle.", min: "high" },
    { name: "Flessioni esplosive", unit: "rip", base: (a) => Math.max(5, a.pushups * 0.3), tip: "Spingi così forte che le mani si staccano.", min: "high" },
  ],
  legs: [
    { name: "Squat", unit: "rip", base: (a, L) => Math.max(LV(L, 12, 20, 30), a.squats * 0.6), tip: "Talloni a terra, sedere indietro, petto alto." },
    { name: "Affondi alternati", unit: "rip", base: (a, L) => Math.max(LV(L, 8, 12, 16), a.squats * 0.35), tip: "Ginocchio posteriore che sfiora il pavimento." },
    { name: "Ponte glutei", unit: "rip", base: (_a, L) => LV(L, 15, 20, 25), tip: "Stringi i glutei in alto per un secondo." },
    { name: "Wall sit", unit: "sec", base: (_a, L) => LV(L, 30, 45, 75), tip: "Schiena al muro, cosce parallele al pavimento." },
    { name: "Jump squat", unit: "rip", base: (a) => Math.max(10, a.squats * 0.3), tip: "Atterra morbido e riparti subito.", min: "mid" },
    { name: "Squat bulgaro", unit: "rip", base: (a) => Math.max(8, a.squats * 0.2), tip: "Piede dietro su una sedia. Per gamba.", min: "high" },
  ],
  core: [
    { name: "Plank", unit: "sec", base: (a, L) => Math.max(LV(L, 20, 40, 60), a.plank * 0.7), tip: "Addome contratto, sedere in linea." },
    { name: "Crunch", unit: "rip", base: (_a, L) => LV(L, 15, 25, 35), tip: "Sali con l'addome, non tirare il collo." },
    { name: "Mountain climber", unit: "sec", base: (_a, L) => LV(L, 20, 30, 45), tip: "Ginocchia al petto veloci, bacino basso." },
    { name: "Plank laterale", unit: "sec", base: (a, L) => Math.max(LV(L, 15, 25, 40), a.plank * 0.4), tip: "Per lato. Fianco alto, non cedere." },
    { name: "Leg raise", unit: "rip", base: (_a, L) => LV(L, 10, 15, 20), tip: "Gambe tese, scendi lento.", min: "mid" },
    { name: "Hollow hold", unit: "sec", base: (_a) => 40, tip: "Schiena incollata a terra, braccia e gambe sollevate.", min: "high" },
  ],
  pull: [
    { name: "Trazioni", unit: "rip", base: (a) => Math.max(2, a.pullups * 0.6), tip: "Mento sopra la sbarra, scendi controllato." },
    { name: "Trazioni negative", unit: "rip", base: (_a, L) => LV(L, 4, 6, 8), tip: "Salta su e scendi in 4-5 secondi." },
    { name: "Rematore con zaino", unit: "rip", base: (_a, L) => LV(L, 12, 15, 20), tip: "Zaino pieno di libri, gomiti vicino al corpo." },
    { name: "Superman", unit: "rip", base: (_a, L) => LV(L, 12, 15, 20), tip: "A pancia in giù, solleva braccia e gambe." },
    { name: "Dead hang", unit: "sec", base: (_a, L) => LV(L, 20, 35, 60), tip: "Appeso alla sbarra, spalle attive." },
  ],
  full: [
    { name: "Burpees", unit: "rip", base: (_a, L) => LV(L, 6, 10, 15), tip: "Giù, flessione, su, salto. Senza pause." },
    { name: "Jumping jack", unit: "sec", base: (_a, L) => LV(L, 30, 45, 60), tip: "Ritmo costante, respira." },
    { name: "Squat + flessione", unit: "rip", base: (_a, L) => LV(L, 6, 10, 14), tip: "Uno squat, giù, una flessione. È una ripetizione." },
    { name: "Plank", unit: "sec", base: (a, L) => Math.max(LV(L, 20, 40, 60), a.plank * 0.6), tip: "Chiudi con il core." },
    { name: "Corsa sul posto", unit: "sec", base: (_a, L) => LV(L, 45, 60, 90), tip: "Ginocchia alte." },
  ],
};
const RANK: Record<League, number> = { low: 0, mid: 1, high: 2 };

export function isTrainingDay(league: League, d: Date) {
  return TRAIN_DAYS[league].includes(d.getDay());
}

/** Deterministic daily plan: same day → same plan, changes every day, adapts to league + progress. */
export function planForDay(d: Date, league: League, a: ZeroAnswers, assessedAt: Date | null, seed: string): DayPlan {
  const key = localKey(d);
  if (!isTrainingDay(league, d)) {
    const r = rng(`${seed}:${key}:rest`);
    return { key, rest: true, focus: "rest", title: "Riposo", exercises: [], restTip: REST_TIPS[Math.floor(r() * REST_TIPS.length)] };
  }
  const ws = weekStart(d);
  const wr = rng(`${seed}:${localKey(ws)}`);
  const order = [...FOCUS].sort(() => wr() - 0.5);
  const idx = TRAIN_DAYS[league].indexOf(d.getDay());
  const focus = order[idx % order.length];
  const r = rng(`${seed}:${key}`);
  const weeks = assessedAt ? weeksBetween(assessedAt, d) : 0;
  const prog = 1 + Math.min(0.4, weeks * 0.06); // +6% a settimana fino a +40%
  let pool = POOL[focus].filter((t) => RANK[t.min ?? "low"] <= RANK[league]);
  if (focus === "pull" && a.pullups < 1) pool = pool.filter((t) => t.name !== "Trazioni");
  if (focus === "push" && league !== "low") pool = pool.filter((t) => t.name !== "Flessioni sulle ginocchia");
  pool = [...pool].sort(() => r() - 0.5);
  const n = league === "low" ? 3 : 4;
  const sets = league === "low" ? 3 : league === "mid" ? 3 : 4;
  const exercises = pool.slice(0, n).map((t, i) => {
    const jitter = 0.9 + r() * 0.2;
    const raw = t.base(a, league) * prog * jitter;
    const amount = t.unit === "sec" ? Math.max(10, Math.round(raw / 5) * 5) : Math.max(2, Math.round(raw));
    return { id: `${key}:${i}`, name: t.name, sets, amount, unit: t.unit, tip: t.tip };
  });
  return { key, rest: false, focus, title: FOCUS_TITLE[focus], exercises };
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
