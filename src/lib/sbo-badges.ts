// Streak badge definitions (must match DB check_streak_badges thresholds).
export type BadgeTier = "common" | "rare" | "epic" | "legendary" | "mythic";

export type BadgeDef = {
  threshold: number;
  tier: BadgeTier;
  name: string;
  emoji: string;
  gradient: string; // css gradient
  ring: string; // tailwind ring
  shiny: boolean; // sparkles + glow
};

export const STREAK_BADGES: BadgeDef[] = [
  { threshold: 10, tier: "common", name: "Prima goccia", emoji: "💧", gradient: "linear-gradient(135deg,#9ca3af,#4b5563)", ring: "ring-slate-400/40", shiny: false },
  { threshold: 25, tier: "rare", name: "Ruscello", emoji: "🌊", gradient: "linear-gradient(135deg,#38bdf8,#0284c7)", ring: "ring-sky-400/50", shiny: false },
  { threshold: 50, tier: "epic", name: "Cascata", emoji: "⚡", gradient: "linear-gradient(135deg,#a78bfa,#7c3aed)", ring: "ring-violet-400/60", shiny: true },
  { threshold: 100, tier: "legendary", name: "Oceano", emoji: "👑", gradient: "linear-gradient(135deg,#fbbf24,#f59e0b,#d97706)", ring: "ring-amber-400/70", shiny: true },
  { threshold: 150, tier: "mythic", name: "Mito", emoji: "🌟", gradient: "linear-gradient(135deg,#f472b6,#a855f7,#facc15)", ring: "ring-fuchsia-400/80", shiny: true },
];

export function badgeByThreshold(threshold: number): BadgeDef | undefined {
  return STREAK_BADGES.find((b) => b.threshold === threshold);
}

export function tierLabel(tier: BadgeTier): string {
  return { common: "Comune", rare: "Rara", epic: "Epica", legendary: "Leggendaria", mythic: "Mitica" }[tier];
}
