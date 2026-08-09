// Titoli onorifici basati sul TOTALE complessivo (non sulle streak).
// - fuoco: numero totale di eventi registrati
// - ghiaccio: numero totale di giorni puliti (senza eventi) accumulati
export type TitleTier = "common" | "uncommon" | "rare" | "super_rare" | "epic" | "mythic" | "legendary";

export type TitleDef = {
  category: "fire" | "ice";
  threshold: number;
  tier: TitleTier;
  name: string;
  emoji: string;
  gradient: string;
};

export const FIRE_TITLES: TitleDef[] = [
  { category: "fire", threshold: 5, tier: "common", name: "Apprendista della Forgia", emoji: "🪓", gradient: "linear-gradient(135deg,#9ca3af,#4b5563)" },
  { category: "fire", threshold: 15, tier: "common", name: "Guardiano della Brace", emoji: "🪔", gradient: "linear-gradient(135deg,#a3a3a3,#525252)" },
  { category: "fire", threshold: 30, tier: "uncommon", name: "Il Fabbro Modello", emoji: "🔨", gradient: "linear-gradient(135deg,#4ade80,#16a34a)" },
  { category: "fire", threshold: 60, tier: "uncommon", name: "Maestro delle Fiamme", emoji: "🔥", gradient: "linear-gradient(135deg,#34d399,#059669)" },
  { category: "fire", threshold: 100, tier: "rare", name: "Domatore di Fornaci", emoji: "⚒️", gradient: "linear-gradient(135deg,#38bdf8,#0284c7)" },
  { category: "fire", threshold: 200, tier: "super_rare", name: "Signore del Magma", emoji: "🌋", gradient: "linear-gradient(135deg,#60a5fa,#2563eb)" },
  { category: "fire", threshold: 350, tier: "epic", name: "Arcano dell'Incendio Eterno", emoji: "⚡", gradient: "linear-gradient(135deg,#a78bfa,#7c3aed)" },
  { category: "fire", threshold: 500, tier: "mythic", name: "Titano della Combustione", emoji: "☄️", gradient: "linear-gradient(135deg,#f472b6,#a855f7,#facc15)" },
  { category: "fire", threshold: 750, tier: "mythic", name: "Imperatore del Sole Nero", emoji: "🌑", gradient: "linear-gradient(135deg,#f472b6,#c084fc,#38bdf8)" },
  { category: "fire", threshold: 1000, tier: "legendary", name: "Divinità Incandescente Primordiale", emoji: "👑", gradient: "linear-gradient(135deg,#fbbf24,#f59e0b,#d97706)" },
  { category: "fire", threshold: 2000, tier: "legendary", name: "Cataclisma Vivente dei Mille Soli", emoji: "🌞", gradient: "linear-gradient(135deg,#facc15,#f97316,#dc2626,#a855f7)" },
];

export const ICE_TITLES: TitleDef[] = [
  { category: "ice", threshold: 3, tier: "common", name: "Novizio della Brina", emoji: "🌨️", gradient: "linear-gradient(135deg,#bae6fd,#7dd3fc)" },
  { category: "ice", threshold: 10, tier: "uncommon", name: "Custode del Gelo", emoji: "❄️", gradient: "linear-gradient(135deg,#7dd3fc,#0ea5e9)" },
  { category: "ice", threshold: 25, tier: "rare", name: "Scultore di Cristalli", emoji: "💎", gradient: "linear-gradient(135deg,#a5f3fc,#22d3ee)" },
  { category: "ice", threshold: 50, tier: "super_rare", name: "Sentinella dell'Iceberg", emoji: "🏔️", gradient: "linear-gradient(135deg,#e0f2fe,#38bdf8,#1e3a8a)" },
  { category: "ice", threshold: 100, tier: "epic", name: "Sovrano del Ghiacciaio Perenne", emoji: "🧊", gradient: "linear-gradient(135deg,#c7d2fe,#818cf8,#4f46e5)" },
  { category: "ice", threshold: 200, tier: "mythic", name: "Colosso dell'Inverno Infinito", emoji: "🐻‍❄️", gradient: "linear-gradient(135deg,#ffffff,#e0f2fe,#38bdf8,#6366f1)" },
  { category: "ice", threshold: 365, tier: "legendary", name: "Monarca dello Zero Assoluto", emoji: "🌌", gradient: "linear-gradient(135deg,#ffffff,#a5f3fc,#818cf8,#c026d3)" },
  { category: "ice", threshold: 730, tier: "legendary", name: "Entità Glaciale Oltre il Tempo", emoji: "🪐", gradient: "linear-gradient(135deg,#ffffff,#bae6fd,#6366f1,#0f172a)" },
];

export const ALL_TITLES: TitleDef[] = [...FIRE_TITLES, ...ICE_TITLES];

export function currentTitle(category: "fire" | "ice", total: number): TitleDef | undefined {
  const list = category === "fire" ? FIRE_TITLES : ICE_TITLES;
  return [...list].reverse().find((t) => total >= t.threshold);
}

export function nextTitle(category: "fire" | "ice", total: number): TitleDef | undefined {
  const list = category === "fire" ? FIRE_TITLES : ICE_TITLES;
  return list.find((t) => total < t.threshold);
}

export function titleTierLabel(tier: TitleTier): string {
  return {
    common: "Comune",
    uncommon: "Non comune",
    rare: "Raro",
    super_rare: "Super raro",
    epic: "Epico",
    mythic: "Mitico",
    legendary: "Leggendario",
  }[tier];
}

export function titleId(t: TitleDef): string {
  return `${t.category}:${t.threshold}`;
}

export function titleById(id: string | null | undefined): TitleDef | undefined {
  if (!id) return undefined;
  return ALL_TITLES.find((t) => titleId(t) === id);
}

/** Every title the user has already earned, ordered by rarity. */
export function unlockedTitles(fireTotal: number, iceTotal: number): TitleDef[] {
  return ALL_TITLES.filter((t) => (t.category === "fire" ? fireTotal : iceTotal) >= t.threshold);
}
