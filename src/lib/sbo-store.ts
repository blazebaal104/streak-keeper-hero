import { useEffect, useState, useCallback } from "react";

export type SboEvent = { ts: number };
export type SboState = {
  nickname: string;
  events: SboEvent[];
  friends: { nickname: string; streak: number; avatar: string }[];
  dailyLimit: number;
  onboarded: boolean;
};

const KEY = "sbo-state-v1";

const DEFAULT_FRIENDS = [
  { nickname: "moonwalker", streak: 14, avatar: "🌙" },
  { nickname: "iron_will", streak: 7, avatar: "⚔️" },
  { nickname: "zenmaster", streak: 32, avatar: "🧘" },
  { nickname: "phoenix", streak: 3, avatar: "🔥" },
];

const defaultState: SboState = {
  nickname: "",
  events: [],
  friends: DEFAULT_FRIENDS,
  dailyLimit: 3,
  onboarded: false,
};

function read(): SboState {
  if (typeof window === "undefined") return defaultState;
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return defaultState;
    return { ...defaultState, ...JSON.parse(raw) };
  } catch {
    return defaultState;
  }
}

export function useSbo() {
  const [state, setState] = useState<SboState>(defaultState);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    setState(read());
    setReady(true);
  }, []);

  const update = useCallback((patch: Partial<SboState> | ((s: SboState) => SboState)) => {
    setState((prev) => {
      const next = typeof patch === "function" ? patch(prev) : { ...prev, ...patch };
      localStorage.setItem(KEY, JSON.stringify(next));
      return next;
    });
  }, []);

  const addEvent = useCallback(() => {
    update((s) => ({ ...s, events: [...s.events, { ts: Date.now() }] }));
  }, [update]);

  const undoLast = useCallback(() => {
    update((s) => ({ ...s, events: s.events.slice(0, -1) }));
  }, [update]);

  return { state, update, addEvent, undoLast, ready };
}

// ---------- derivations ----------
const DAY_MS = 86_400_000;

export function startOfDay(d: Date | number) {
  const x = new Date(d);
  x.setHours(0, 0, 0, 0);
  return x;
}

export function countToday(events: SboEvent[]) {
  const start = startOfDay(new Date()).getTime();
  return events.filter((e) => e.ts >= start).length;
}

export function countThisMonth(events: SboEvent[]) {
  const now = new Date();
  return events.filter((e) => {
    const d = new Date(e.ts);
    return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
  }).length;
}

export function eventsByDay(events: SboEvent[]): Map<string, number> {
  const m = new Map<string, number>();
  for (const e of events) {
    const k = startOfDay(e.ts).toISOString().slice(0, 10);
    m.set(k, (m.get(k) ?? 0) + 1);
  }
  return m;
}

/** Current clean streak = consecutive days with 0 events ending yesterday (or today if no event today). */
export function computeStreak(events: SboEvent[]): number {
  if (events.length === 0) {
    // start counting from first-ever day — arbitrary 0
    return 0;
  }
  const byDay = eventsByDay(events);
  const today = startOfDay(new Date()).getTime();
  const todayKey = new Date(today).toISOString().slice(0, 10);
  let streak = 0;
  let cursor = today;
  // If today has events, streak is 0
  if (byDay.get(todayKey)) return 0;
  // Count back day by day until we hit an event
  while (true) {
    const key = new Date(cursor).toISOString().slice(0, 10);
    if (cursor === today) {
      // today has no events — count as day 1 only if there's at least one prior event (started streak)
      // Actually just start counting from today
      streak += 1;
      cursor -= DAY_MS;
      continue;
    }
    if (byDay.get(key)) break;
    streak += 1;
    cursor -= DAY_MS;
    // safety cap
    if (streak > 3650) break;
  }
  return streak;
}

/** Adaptive daily limit: rolling 14-day average, rounded, min 1. */
export function adaptiveLimit(events: SboEvent[], fallback = 3): number {
  const now = Date.now();
  const cutoff = now - 14 * DAY_MS;
  const recent = events.filter((e) => e.ts >= cutoff);
  if (recent.length < 5) return fallback;
  const avg = recent.length / 14;
  return Math.max(1, Math.round(avg));
}

// ---------- encouragement (30+ per bucket) ----------
const ENCOURAGE = [
  "{name}, ogni goccia trattenuta è una vittoria. 💧",
  "Sei più forte di ieri, {name}. Continua così.",
  "{name}, la tua streak è la prova del tuo carattere.",
  "Un respiro. Un passo. Un giorno in più, {name}.",
  "{name}, il fuoco che non alimenti si spegne.",
  "Disciplina oggi, libertà domani. Forza {name}!",
  "{name}, hai già fatto la parte più dura. Non mollare.",
  "La mente comanda il corpo, {name}. Sei tu al volante.",
  "{name}, ogni giorno pulito ti riprende un pezzo di te.",
  "Testa alta, {name}. Il tuo io futuro ti ringrazia.",
  "{name}, la noia è il prezzo della libertà. Pagala.",
  "Un impulso è solo un'onda, {name}. Lascialo passare.",
  "{name}, ricorda perché hai iniziato.",
  "Fai una flessione invece, {name}. 💪",
  "{name}, l'energia trattenuta diventa carburante.",
  "Un giorno alla volta, {name}. Basta questo.",
  "{name}, il tuo focus oggi vale doppio.",
  "Ogni 'no' è un 'sì' a te stesso, {name}.",
  "{name}, non sei le tue voglie. Sei chi decide.",
  "Bevi acqua, {name}. Uscì a camminare. 🌿",
  "{name}, sei ufficialmente in modalità Sigma.",
  "La pazienza è il tuo superpotere, {name}.",
  "{name}, testosterone alle stelle. 🚀",
  "Piccoli passi, grande cambiamento. Avanti {name}.",
  "{name}, oggi hai già vinto se stai leggendo qui.",
  "Non contare i giorni, {name}. Fai contare i giorni.",
  "{name}, la disciplina pesa grammi, il rimpianto tonnellate.",
  "Respira profondo, {name}. È solo un momento.",
  "{name}, sei un guerriero. Comportati come tale. ⚔️",
  "Il vero flex è la costanza, {name}.",
  "{name}, ogni tentazione superata rafforza la mente.",
  "Sei il CEO della tua vita, {name}. Sii esigente.",
];

const SLOWDOWN = [
  "{name}, forse è il momento di fare una pausa. 🌊",
  "Il contatore è alto oggi, {name}. Respira.",
  "{name}, il tuo corpo ti ringrazierà se ti fermi ora.",
  "Ehi {name}, prova una camminata invece. 🚶",
  "{name}, hai già fatto abbastanza per oggi. Basta.",
  "Doccia fredda, {name}. Serve davvero. 🚿",
  "{name}, chiudi il telefono. Vai fuori. 🌤️",
  "Il piacere di oggi è la stanchezza di domani, {name}.",
  "{name}, guarda quanto hai fatto oggi. Prendi fiato.",
  "Fermati, {name}. Non sei obbligato a continuare.",
  "{name}, alza gli occhi dallo schermo per 10 minuti.",
  "Reset {name}: acqua, aria, movimento.",
  "{name}, il tuo cervello vuole dopamina pulita. Trovala altrove.",
  "Ogni volta in più oggi pesa doppio, {name}.",
  "{name}, disciplina non è dire sì. Disciplina è dire basta.",
  "Fai 20 flessioni ora, {name}. Vedrai. 💪",
  "{name}, sei sopra il tuo limite. È un segnale.",
  "Chiama un amico, {name}. Fai qualcosa di vero.",
  "{name}, il corpo protesta. Ascoltalo.",
  "Ok {name}, per oggi hai finito. Domani ricominciamo.",
];

const RESTART = [
  "Ok {name}, si ricomincia da capo. Domani è un nuovo giorno.",
  "{name}, cadere fa parte del percorso. Rialzati.",
  "Nuovo tentativo, {name}. Le migliori streak nascono dopo un reset.",
  "{name}, non è una sconfitta se impari qualcosa.",
  "Reset. Respira. Riparti, {name}.",
  "{name}, i campioni cadono e si rialzano. Fallo.",
  "Zero giorni, mille possibilità, {name}.",
  "{name}, il passato è passato. Focus su ora.",
  "Ogni maestro è stato un principiante, {name}.",
  "{name}, la prossima streak sarà più forte. Promesso.",
  "Non giudicarti, {name}. Riprova.",
  "{name}, siamo umani. Domani è un altro giorno.",
  "Un piccolo scivolone non ti definisce, {name}.",
  "{name}, prendi appunti mentali su cos'è successo. Poi avanti.",
  "Nuova pagina, {name}. Scrivi meglio.",
  "{name}, il coraggio è ricominciare senza drammi.",
  "Zero è un ottimo punto di partenza, {name}.",
  "{name}, obiettivo domani: un giorno pulito. Uno solo.",
  "Ogni ricaduta è un dato, {name}. Impara.",
  "{name}, sei ancora nel gioco. Continua a giocare.",
];

const STREAK_MILESTONE = [
  "🔥 {name}, streak di {days} giorni! Sei una macchina.",
  "💎 {days} giorni, {name}. Roba seria.",
  "👑 {name}, {days} giorni. Corona meritata.",
  "🚀 {days} giorni, {name}. Non fermarti ora.",
];

function pick(arr: string[], name: string) {
  return arr[Math.floor(Math.random() * arr.length)].replaceAll("{name}", name || "amico");
}

export const encourage = (name: string) => pick(ENCOURAGE, name);
export const slowdown = (name: string) => pick(SLOWDOWN, name);
export const restart = (name: string) => pick(RESTART, name);
export const milestone = (name: string, days: number) =>
  pick(STREAK_MILESTONE, name).replaceAll("{days}", String(days));

// ---------- friend codes ----------
export function generateFriendCode(): string {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let out = "";
  for (let i = 0; i < 6; i++) out += chars[Math.floor(Math.random() * chars.length)];
  return out;
}

