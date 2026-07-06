// Fire (daily) & Ice (abstinence) streak badge definitions.
// Must match DB check_streak_badges thresholds/tiers.
export type BadgeTier = "common" | "uncommon" | "rare" | "super_rare" | "epic" | "mythic" | "legendary";
export type BadgeCategory = "fire" | "ice" | "streak"; // legacy 'streak' kept for old rows

export type BadgeDef = {
  category: BadgeCategory;
  threshold: number;
  tier: BadgeTier;
  name: string;
  emoji: string;
  gradient: string;
  ring: string;
  shiny: boolean;
};

export const FIRE_BADGES: BadgeDef[] = [
  { category: "fire", threshold: 3, tier: "common", name: "Scintilla", emoji: "🔥", gradient: "linear-gradient(135deg,#9ca3af,#4b5563)", ring: "ring-slate-400/40", shiny: false },
  { category: "fire", threshold: 7, tier: "common", name: "Falò", emoji: "🔥", gradient: "linear-gradient(135deg,#a3a3a3,#525252)", ring: "ring-neutral-400/40", shiny: false },
  { category: "fire", threshold: 14, tier: "uncommon", name: "Braciere", emoji: "🔥", gradient: "linear-gradient(135deg,#4ade80,#16a34a)", ring: "ring-green-400/50", shiny: false },
  { category: "fire", threshold: 21, tier: "uncommon", name: "Camino", emoji: "🔥", gradient: "linear-gradient(135deg,#34d399,#059669)", ring: "ring-emerald-400/50", shiny: false },
  { category: "fire", threshold: 30, tier: "rare", name: "Fornace", emoji: "🔥", gradient: "linear-gradient(135deg,#38bdf8,#0284c7)", ring: "ring-sky-400/60", shiny: false },
  { category: "fire", threshold: 50, tier: "super_rare", name: "Vulcano", emoji: "🌋", gradient: "linear-gradient(135deg,#60a5fa,#2563eb,#1e40af)", ring: "ring-blue-400/60", shiny: true },
  { category: "fire", threshold: 75, tier: "epic", name: "Inferno", emoji: "⚡", gradient: "linear-gradient(135deg,#a78bfa,#7c3aed)", ring: "ring-violet-400/70", shiny: true },
  { category: "fire", threshold: 100, tier: "mythic", name: "Sole", emoji: "☀️", gradient: "linear-gradient(135deg,#f472b6,#a855f7,#facc15)", ring: "ring-fuchsia-400/70", shiny: true },
  { category: "fire", threshold: 150, tier: "mythic", name: "Cometa", emoji: "☄️", gradient: "linear-gradient(135deg,#f472b6,#c084fc,#38bdf8)", ring: "ring-pink-400/70", shiny: true },
  { category: "fire", threshold: 200, tier: "legendary", name: "Stella", emoji: "🌟", gradient: "linear-gradient(135deg,#fbbf24,#f59e0b,#d97706)", ring: "ring-amber-400/80", shiny: true },
  { category: "fire", threshold: 300, tier: "legendary", name: "Nova", emoji: "👑", gradient: "linear-gradient(135deg,#facc15,#f97316,#dc2626,#a855f7)", ring: "ring-yellow-400/80", shiny: true },
];

export const ICE_BADGES: BadgeDef[] = [
  { category: "ice", threshold: 1, tier: "rare", name: "Brina", emoji: "❄️", gradient: "linear-gradient(135deg,#bae6fd,#7dd3fc)", ring: "ring-sky-300/60", shiny: true },
  { category: "ice", threshold: 3, tier: "epic", name: "Gelo", emoji: "❄️", gradient: "linear-gradient(135deg,#7dd3fc,#0ea5e9)", ring: "ring-sky-400/70", shiny: true },
  { category: "ice", threshold: 7, tier: "epic", name: "Cristallo", emoji: "🧊", gradient: "linear-gradient(135deg,#a5f3fc,#22d3ee,#0891b2)", ring: "ring-cyan-400/70", shiny: true },
  { category: "ice", threshold: 14, tier: "mythic", name: "Iceberg", emoji: "🏔️", gradient: "linear-gradient(135deg,#e0f2fe,#38bdf8,#1e3a8a)", ring: "ring-cyan-300/80", shiny: true },
  { category: "ice", threshold: 30, tier: "mythic", name: "Ghiacciaio", emoji: "🧊", gradient: "linear-gradient(135deg,#c7d2fe,#818cf8,#4f46e5)", ring: "ring-indigo-300/80", shiny: true },
  { category: "ice", threshold: 60, tier: "legendary", name: "Polare", emoji: "❄️", gradient: "linear-gradient(135deg,#ffffff,#e0f2fe,#38bdf8,#6366f1)", ring: "ring-white/80", shiny: true },
  { category: "ice", threshold: 100, tier: "legendary", name: "Assoluto Zero", emoji: "🌌", gradient: "linear-gradient(135deg,#ffffff,#a5f3fc,#818cf8,#c026d3)", ring: "ring-fuchsia-300/80", shiny: true },
];

export const ALL_BADGES: BadgeDef[] = [...FIRE_BADGES, ...ICE_BADGES];

// Legacy: some older code refers to STREAK_BADGES.
export const STREAK_BADGES: BadgeDef[] = FIRE_BADGES;

export function badgeDef(category: BadgeCategory, threshold: number): BadgeDef | undefined {
  return ALL_BADGES.find((b) => b.category === category && b.threshold === threshold)
    || (category === "streak" ? FIRE_BADGES.find((b) => b.threshold === threshold) : undefined);
}
export function badgeByThreshold(threshold: number): BadgeDef | undefined {
  return FIRE_BADGES.find((b) => b.threshold === threshold);
}

export function tierLabel(tier: BadgeTier): string {
  return {
    common: "Comune",
    uncommon: "Non comune",
    rare: "Rara",
    super_rare: "Super rara",
    epic: "Epica",
    mythic: "Mitica",
    legendary: "Leggendaria",
  }[tier];
}

export function tierRank(tier: BadgeTier): number {
  return { common: 1, uncommon: 2, rare: 3, super_rare: 4, epic: 5, mythic: 6, legendary: 7 }[tier];
}
