import { memo } from "react";
import { tierRank, type BadgeDef } from "@/lib/sbo-badges";

/** Deterministic pseudo-random from a seed. */
const rnd = (seed: number, i: number) => {
  const x = Math.sin(seed * 17.3 + i * 91.7) * 10000;
  return x - Math.floor(x);
};

/**
 * Unified badge artwork used everywhere (slot, inventory, friend profile).
 *
 * Performance: every effect is a pure CSS animation running on the compositor
 * (transform/opacity/filter only). No JS runs per frame, so a full inventory
 * grid stays smooth even with dozens of badges on screen.
 *
 * Each rarity tier gets its OWN signature effect — they no longer look alike:
 *  1 common     → matte metal, static gloss
 *  2 uncommon   → slow gloss sweep
 *  3 rare       → fast shine sweep + breathing rim light
 *  4 super rare → rotating conic sheen behind the emblem
 *  5 epic       → energy ring + pulsing aura + orbiting light motes
 *  6 mythic     → counter-rotating nebula + hue drift + orbiting motes
 *  7 legendary  → prismatic hue cycle + radiating crown rays + full FX stack
 */
function BadgeArtBase({
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
  const isIce = def.category === "ice";
  const fx = animated && !locked ? true : animated; // locked badges keep ambient motion (teaser)
  const accent = isIce ? "rgba(190,240,255,0.95)" : "rgba(255,225,160,0.95)";

  const motes = rank >= 7 ? 4 : rank >= 5 ? 3 : 0;

  return (
    <div
      className={`relative isolate overflow-hidden rounded-2xl ring-2 ${def.ring} ${className}`}
      style={{ width: size, height: size, background: def.gradient, contain: "paint" }}
    >
      {/* T6/T7 — nebula: two counter-rotating conic fields */}
      {fx && rank >= 6 && (
        <>
          <div
            className="absolute -inset-1/2 opacity-45 will-change-transform"
            style={{
              background: `conic-gradient(from 0deg, transparent, ${accent}, transparent 35%, ${isIce ? "rgba(120,200,255,.8)" : "rgba(255,120,60,.8)"}, transparent 70%)`,
              animation: `sbo-spin ${rank >= 7 ? 6 : 9}s linear infinite`,
            }}
          />
          <div
            className="absolute -inset-1/3 opacity-30 mix-blend-screen will-change-transform"
            style={{
              background: "conic-gradient(from 180deg, transparent, rgba(255,255,255,.9), transparent 45%)",
              animation: `sbo-spin-rev ${rank >= 7 ? 9 : 14}s linear infinite`,
            }}
          />
        </>
      )}

      {/* T4/T5 — single rotating conic sheen */}
      {fx && (rank === 4 || rank === 5) && (
        <div
          className="absolute -inset-1/2 opacity-35 will-change-transform"
          style={{
            background: `conic-gradient(from 0deg, transparent, ${accent}, transparent 40%)`,
            animation: `sbo-spin ${rank === 5 ? 8 : 12}s linear infinite`,
          }}
        />
      )}

      {/* T7 — radiating crown rays */}
      {fx && rank >= 7 && (
        <div
          className="absolute -inset-1/2 opacity-25 mix-blend-screen will-change-transform"
          style={{
            background:
              "repeating-conic-gradient(from 0deg, rgba(255,255,255,.85) 0deg 3deg, transparent 3deg 18deg)",
            animation: "sbo-spin 22s linear infinite",
          }}
        />
      )}

      {/* T5+ — pulsing aura */}
      {fx && rank >= 5 && (
        <div
          className="absolute inset-0 will-change-transform"
          style={{
            background: isIce
              ? "radial-gradient(circle at 50% 55%, rgba(190,240,255,.55), transparent 65%)"
              : "radial-gradient(circle at 50% 75%, rgba(255,190,90,.6), transparent 65%)",
            animation: "sbo-pulse 2.6s ease-in-out infinite",
          }}
        />
      )}

      {/* Glass body */}
      <div className="absolute inset-0 bg-gradient-to-br from-white/35 via-transparent to-black/25" />

      {/* T3+ — shine sweep */}
      {fx && rank >= 3 && (
        <div
          className="absolute inset-y-0 w-1/3 bg-white/40 blur-[2px] will-change-transform"
          style={{ animation: `sbo-sheen ${rank >= 6 ? 2.4 : 3.6}s ease-in-out infinite` }}
        />
      )}
      {/* T2 — slow, subtle gloss */}
      {fx && rank === 2 && (
        <div
          className="absolute inset-y-0 w-1/4 bg-white/25 blur-[3px] will-change-transform"
          style={{ animation: "sbo-sheen 7s ease-in-out infinite" }}
        />
      )}
      {/* T1 — static gloss only */}
      {rank === 1 && <div className="absolute inset-0 bg-gradient-to-tr from-transparent via-white/12 to-transparent" />}

      {/* T3+ — breathing rim light */}
      {fx && rank >= 3 && (
        <div
          className="absolute inset-0 rounded-2xl"
          style={{
            boxShadow: `inset 0 0 ${size * 0.22}px ${accent}`,
            animation: "sbo-pulse 3.2s ease-in-out infinite",
          }}
        />
      )}

      {/* Ice frost lattice */}
      {isIce && (
        <svg
          viewBox="0 0 100 100"
          className="absolute inset-0 h-full w-full"
          style={fx ? { animation: "sbo-frost 4s ease-in-out infinite" } : { opacity: 0.35 }}
        >
          <g stroke="white" strokeWidth="1.2" fill="none">
            <path d="M50 6 L50 94 M6 50 L94 50 M18 18 L82 82 M82 18 L18 82" />
            <path d="M50 22 L40 32 M50 22 L60 32 M50 78 L40 68 M50 78 L60 68" />
          </g>
        </svg>
      )}

      {/* Fire embers (rank 4+) */}
      {fx && !isIce && rank >= 4 &&
        Array.from({ length: rank >= 6 ? 5 : 3 }, (_, i) => (
          <span
            key={`e${i}`}
            className="absolute bottom-0 rounded-full bg-amber-200 will-change-transform"
            style={{
              left: `${14 + rnd(seed, i) * 70}%`,
              width: Math.max(2, size * 0.045),
              height: Math.max(2, size * 0.045),
              animation: `sbo-ember ${1.8 + rnd(seed, i + 9) * 1.4}s ease-out ${rnd(seed, i + 20) * 1.6}s infinite`,
            }}
          />
        ))}

      {/* T5+ — orbiting light motes (real orbit, not random stars) */}
      {fx &&
        Array.from({ length: motes }, (_, i) => (
          <span
            key={`m${i}`}
            className="absolute left-1/2 top-1/2 rounded-full will-change-transform"
            style={
              {
                width: Math.max(3, size * 0.07),
                height: Math.max(3, size * 0.07),
                marginLeft: -size * 0.035,
                marginTop: -size * 0.035,
                background: accent,
                boxShadow: `0 0 ${size * 0.16}px ${accent}`,
                "--r": `${size * (0.3 + (i % 2) * 0.08)}px`,
                animation: `sbo-orbit ${4 + i * 1.3}s linear ${i * 0.5}s infinite`,
              } as React.CSSProperties
            }
          />
        ))}

      {/* Emblem */}
      <div className="absolute inset-0 grid place-items-center">
        <span
          className={`drop-shadow ${locked ? "grayscale opacity-50" : ""}`}
          style={{
            fontSize: size * 0.46,
            lineHeight: 1,
            animation: fx && rank >= 4 ? "sbo-float 3.4s ease-in-out infinite" : undefined,
            willChange: fx && rank >= 4 ? "transform" : undefined,
          }}
        >
          {def.emoji}
        </span>
      </div>

      {/* T7 — prismatic hue cycle over the whole badge */}
      {fx && rank >= 7 && (
        <div
          className="pointer-events-none absolute inset-0 mix-blend-overlay opacity-40"
          style={{
            background: "linear-gradient(120deg, #f0f, #0ff, #ff0, #f0f)",
            animation: "sbo-hue 6s linear infinite",
          }}
        />
      )}

      {/* Locked veil */}
      {locked && <div className="absolute inset-0 bg-black/55" />}
    </div>
  );
}

export const BadgeArt = memo(BadgeArtBase);
