import { tierRank, type BadgeDef } from "@/lib/sbo-badges";

/** Deterministic pseudo-random from a seed. */
const rnd = (seed: number, i: number) => {
  const x = Math.sin(seed * 17.3 + i * 91.7) * 10000;
  return x - Math.floor(x);
};

/**
 * Unified badge artwork used everywhere (slot, inventory, friend profile).
 * The visual richness scales with the badge tier: higher rarity = more layers.
 * All motion is CSS-driven (transform/opacity only) so long lists of badges stay
 * cheap even on low-end phones.
 */
export function BadgeArt({
  def,
  size = 64,
  locked = false,
  animated = true,
  className = "",
}: {
  def: BadgeDef;
  size?: number;
  locked?: boolean;
  animated?: boolean;
  className?: string;
}) {
  const rank = tierRank(def.tier); // 1..7
  const seed = def.threshold * (def.category === "ice" ? 7 : 3);
  const sparkles = !animated ? 0 : rank <= 2 ? 0 : rank === 3 ? 2 : rank === 4 ? 3 : rank === 5 ? 4 : rank === 6 ? 5 : 6;
  const isIce = def.category === "ice";

  return (
    <div
      className={`relative overflow-hidden rounded-2xl ring-2 ${def.ring} ${className}`}
      style={{ width: size, height: size, background: def.gradient, contain: "paint" }}
    >
      {/* Rarity aura: rotating conic sheen for epic and above */}
      {rank >= 5 && animated && (
        <div
          className="absolute -inset-1/2 opacity-40 will-change-transform"
          style={{
            background: isIce
              ? "conic-gradient(from 0deg, transparent, rgba(255,255,255,0.9), transparent 40%)"
              : "conic-gradient(from 0deg, transparent, rgba(255,240,180,0.9), transparent 40%)",
            animation: `badge-spin ${rank >= 7 ? 4 : 7}s linear infinite`,
          }}
        />
      )}

      {/* Glass highlight */}
      <div className="absolute inset-0 bg-gradient-to-br from-white/35 via-transparent to-black/20" />

      {/* Shine sweep for rare and above */}
      {rank >= 3 && animated && (
        <div
          className="absolute inset-y-0 w-1/3 bg-white/35 blur-[2px] will-change-transform"
          style={{ animation: `badge-sheen ${rank >= 6 ? 3 : 5}s ease-in-out infinite` }}
        />
      )}

      {/* Ice frost crystals */}
      {isIce && (
        <svg viewBox="0 0 100 100" className="absolute inset-0 h-full w-full opacity-40">
          <g stroke="white" strokeWidth="1.2" fill="none">
            <path d="M50 6 L50 94 M6 50 L94 50 M18 18 L82 82 M82 18 L18 82" />
            <path d="M50 22 L40 32 M50 22 L60 32 M50 78 L40 68 M50 78 L60 68" />
          </g>
        </svg>
      )}

      {/* Fire ember glow */}
      {!isIce && rank >= 4 && animated && (
        <div
          className="absolute inset-0"
          style={{
            background: "radial-gradient(circle at 50% 80%, rgba(255,200,80,0.55), transparent 60%)",
            animation: "badge-ember 2.2s ease-in-out infinite",
          }}
        />
      )}

      {/* Emblem */}
      <div className="absolute inset-0 grid place-items-center">
        <span
          className={`drop-shadow ${locked ? "grayscale opacity-50" : ""}`}
          style={{
            fontSize: size * 0.46,
            lineHeight: 1,
            animation: animated && rank >= 4 ? "badge-bob 3.4s ease-in-out infinite" : undefined,
          }}
        >
          {def.emoji}
        </span>
      </div>

      {/* Sparkles */}
      {Array.from({ length: sparkles }, (_, i) => (
        <span
          key={i}
          className={isIce ? "absolute text-white will-change-transform" : "absolute text-yellow-100 will-change-transform"}
          style={{
            left: `${rnd(seed, i) * 88}%`,
            top: `${rnd(seed, i + 40) * 84}%`,
            fontSize: Math.max(6, size * (0.08 + rnd(seed, i + 80) * 0.08)),
            animation: `badge-sparkle ${(1.4 + rnd(seed, i + 120) * 1.8).toFixed(2)}s ease-in-out ${(rnd(seed, i + 160) * 2).toFixed(2)}s infinite`,
          }}
        >
          ✦
        </span>
      ))}

      {/* Locked veil */}
      {locked && <div className="absolute inset-0 bg-black/55" />}
    </div>
  );
}
