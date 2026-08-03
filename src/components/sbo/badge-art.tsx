import { motion } from "motion/react";
import { tierRank, type BadgeDef } from "@/lib/sbo-badges";

/** Deterministic pseudo-random from a seed. */
const rnd = (seed: number, i: number) => {
  const x = Math.sin(seed * 17.3 + i * 91.7) * 10000;
  return x - Math.floor(x);
};

/**
 * Unified badge artwork used everywhere (slot, inventory, friend profile).
 * The visual richness scales with the badge tier: higher rarity = more layers.
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
  const sparkles = rank <= 2 ? 0 : rank === 3 ? 3 : rank === 4 ? 5 : rank === 5 ? 7 : rank === 6 ? 9 : 12;
  const isIce = def.category === "ice";

  return (
    <div
      className={`relative overflow-hidden rounded-2xl ring-2 ${def.ring} ${className}`}
      style={{ width: size, height: size, background: def.gradient }}
    >
      {/* Rarity aura: rotating conic sheen for epic and above */}
      {rank >= 5 && animated && (
        <motion.div
          className="absolute -inset-1/2 opacity-40"
          style={{
            background: isIce
              ? "conic-gradient(from 0deg, transparent, rgba(255,255,255,0.9), transparent 40%)"
              : "conic-gradient(from 0deg, transparent, rgba(255,240,180,0.9), transparent 40%)",
          }}
          animate={{ rotate: 360 }}
          transition={{ duration: rank >= 7 ? 4 : 7, repeat: Infinity, ease: "linear" }}
        />
      )}

      {/* Glass highlight */}
      <div className="absolute inset-0 bg-gradient-to-br from-white/35 via-transparent to-black/20" />

      {/* Shine sweep for rare and above */}
      {rank >= 3 && animated && (
        <motion.div
          className="absolute inset-y-0 w-1/3 -skew-x-12 bg-white/35 blur-[2px]"
          initial={{ x: -size }}
          animate={{ x: size * 1.6 }}
          transition={{ duration: 1.4, repeat: Infinity, repeatDelay: rank >= 6 ? 1.6 : 3.6, ease: "easeInOut" }}
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
        <motion.div
          className="absolute inset-0"
          style={{ background: "radial-gradient(circle at 50% 80%, rgba(255,200,80,0.55), transparent 60%)" }}
          animate={{ opacity: [0.4, 0.9, 0.4] }}
          transition={{ duration: 2.2, repeat: Infinity, ease: "easeInOut" }}
        />
      )}

      {/* Emblem */}
      <div className="absolute inset-0 grid place-items-center">
        {animated && rank >= 4 ? (
          <motion.span
            className={`drop-shadow ${locked ? "grayscale opacity-50" : ""}`}
            style={{ fontSize: size * 0.46, lineHeight: 1 }}
            animate={{ y: [0, -size * 0.04, 0], rotate: [0, 4, -4, 0] }}
            transition={{ duration: 3.4, repeat: Infinity, ease: "easeInOut" }}
          >
            {def.emoji}
          </motion.span>
        ) : (
          <span
            className={`drop-shadow ${locked ? "grayscale opacity-50" : ""}`}
            style={{ fontSize: size * 0.46, lineHeight: 1 }}
          >
            {def.emoji}
          </span>
        )}
      </div>

      {/* Sparkles */}
      {animated &&
        Array.from({ length: sparkles }, (_, i) => (
          <motion.span
            key={i}
            className={isIce ? "absolute text-white" : "absolute text-yellow-100"}
            style={{
              left: `${rnd(seed, i) * 88}%`,
              top: `${rnd(seed, i + 40) * 84}%`,
              fontSize: Math.max(6, size * (0.08 + rnd(seed, i + 80) * 0.08)),
            }}
            animate={{ opacity: [0, 1, 0], scale: [0.4, 1, 0.4] }}
            transition={{
              duration: 1.4 + rnd(seed, i + 120) * 1.8,
              delay: rnd(seed, i + 160) * 2,
              repeat: Infinity,
              ease: "easeInOut",
            }}
          >
            ✦
          </motion.span>
        ))}

      {/* Locked veil */}
      {locked && <div className="absolute inset-0 bg-black/55 backdrop-blur-[1px]" />}
    </div>
  );
}
