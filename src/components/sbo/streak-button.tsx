import { motion, AnimatePresence } from "motion/react";
import { Plus } from "lucide-react";
import fireTex from "@/assets/fx-fire.png";
import iceTex from "@/assets/fx-ice.jpg";

/**
 * Main SBO button.
 * Fire and ice are rendered with photoreal textures (a real flame plate and a
 * cracked glacial-ice plate) driven by pure CSS compositor animations, so it
 * looks like burning/frozen material instead of vector polygons — and costs
 * almost nothing per frame.
 *
 * The fire only ignites from a 2-day fire streak upward, with a ramp-up
 * "ignition" animation.
 */
export function StreakButton({
  fireStreak,
  iceStreak,
  onPress,
  melting,
}: {
  fireStreak: number;
  iceStreak: number;
  onPress: () => void;
  melting?: boolean;
}) {
  const fire = Math.min(fireStreak / 30, 1);
  const ice = Math.min(iceStreak / 30, 1);
  const burning = fireStreak >= 2;
  const frozen = iceStreak >= 1;
  const mode: "fire" | "ice" | "neutral" = burning ? "fire" : frozen ? "ice" : "neutral";

  const background = burning
    ? `linear-gradient(160deg, oklch(${0.9 - fire * 0.08} ${0.07 + fire * 0.12} ${70 - fire * 20}), oklch(${0.62 - fire * 0.08} ${0.16 + fire * 0.07} ${40 - fire * 18}))`
    : frozen
      ? `linear-gradient(160deg, oklch(${0.97 - ice * 0.03} 0.03 220), oklch(${0.76 - ice * 0.06} ${0.09 + ice * 0.06} ${230 + ice * 15}))`
      : "var(--gradient-drop)";

  const glow = burning
    ? `0 20px 70px -12px rgba(249,115,22,${0.4 + fire * 0.45}), 0 0 ${50 + fire * 80}px -10px rgba(255,150,50,${0.35 + fire * 0.45})`
    : frozen
      ? `0 20px 70px -12px rgba(56,189,248,${0.3 + ice * 0.4}), 0 0 ${40 + ice * 70}px -10px rgba(180,235,255,${0.3 + ice * 0.4})`
      : "var(--shadow-drop)";

  return (
    <div className="relative flex h-64 w-64 items-center justify-center">
      {/* Heat / cold aura */}
      {burning && (
        <div
          className="pointer-events-none absolute -inset-6 rounded-full will-change-transform"
          style={{
            background: "radial-gradient(circle, rgba(255,120,20,0.42), transparent 65%)",
            animation: "sbo-pulse 2.2s ease-in-out infinite",
          }}
        />
      )}
      {frozen && !burning && (
        <div
          className="pointer-events-none absolute -inset-4 rounded-full will-change-transform"
          style={{
            background: "radial-gradient(circle, rgba(140,220,255,0.34), transparent 64%)",
            animation: "sbo-pulse 3.6s ease-in-out infinite",
          }}
        />
      )}

      {/* Photoreal flame plates above the button */}
      {burning && (
        <div
          className="pointer-events-none absolute left-1/2 z-10 -translate-x-1/2"
          style={{
            bottom: "42%",
            width: 250,
            height: 250 + fire * 90,
            animation: "sbo-ignite 1.1s ease-out both",
            transformOrigin: "bottom center",
          }}
        >
          <div
            className="absolute inset-0 will-change-transform"
            style={{
              backgroundImage: `url(${fireTex})`,
              backgroundSize: "100% 100%",
              mixBlendMode: "screen",
              transformOrigin: "bottom center",
              maskImage: "radial-gradient(120% 100% at 50% 100%, #000 55%, transparent 92%)",
              WebkitMaskImage: "radial-gradient(120% 100% at 50% 100%, #000 55%, transparent 92%)",
              animation: "sbo-flame-lick 1.6s ease-in-out infinite, sbo-flame-flicker 0.7s steps(4) infinite",
            }}
          />
          <div
            className="absolute inset-x-6 bottom-0 top-10 opacity-70 will-change-transform"
            style={{
              backgroundImage: `url(${fireTex})`,
              backgroundSize: "100% 100%",
              mixBlendMode: "screen",
              transform: "scaleX(-1)",
              transformOrigin: "bottom center",
              animation: "sbo-flame-lick 2.3s ease-in-out infinite reverse, sbo-flame-flicker 0.9s steps(3) infinite",
            }}
          />
        </div>
      )}

      <motion.button
        onClick={onPress}
        whileTap={{ scale: 0.9 }}
        whileHover={{ scale: 1.04 }}
        transition={{ type: "spring", stiffness: 400, damping: 15 }}
        className="group relative flex h-48 w-48 items-center justify-center overflow-hidden rounded-full will-change-transform"
        style={{ background, boxShadow: glow }}
        aria-label="Aggiungi 1 al contatore"
      >
        {/* Frozen shell: real cracked-ice photo, tinted and shimmering */}
        {frozen && !burning && (
          <>
            <div
              className="absolute inset-0 rounded-full"
              style={{
                backgroundImage: `url(${iceTex})`,
                backgroundSize: "cover",
                backgroundPosition: "center",
                opacity: 0.55 + ice * 0.35,
                mixBlendMode: "screen",
              }}
            />
            <div
              className="absolute inset-0 rounded-full will-change-transform"
              style={{
                background:
                  "linear-gradient(115deg, transparent 30%, rgba(255,255,255,.75) 48%, transparent 62%)",
                animation: "sbo-ice-shimmer 4.5s ease-in-out infinite",
              }}
            />
            <div
              className="absolute inset-0 rounded-full"
              style={{ boxShadow: "inset 0 0 40px rgba(160,225,255,.85), inset 0 8px 20px rgba(255,255,255,.5)" }}
            />
          </>
        )}

        {/* Burning surface: charred + glowing embers under the flames */}
        {burning && (
          <>
            <div
              className="absolute inset-0 rounded-full"
              style={{
                backgroundImage: `url(${fireTex})`,
                backgroundSize: "160% 160%",
                backgroundPosition: "center 70%",
                mixBlendMode: "screen",
                opacity: 0.55 + fire * 0.35,
              }}
            />
            <div
              className="absolute inset-0 rounded-full will-change-transform"
              style={{
                background: "radial-gradient(circle at 50% 85%, rgba(255,210,120,.85), transparent 62%)",
                animation: "sbo-flame-flicker 0.8s steps(4) infinite",
              }}
            />
            <div
              className="absolute inset-0 rounded-full"
              style={{ boxShadow: "inset 0 0 45px rgba(255,140,40,.9), inset 0 -12px 30px rgba(120,20,0,.6)" }}
            />
          </>
        )}

        <div className="absolute inset-2 rounded-full bg-gradient-to-br from-white/35 to-transparent" />
        <Plus className="relative h-16 w-16 text-primary-foreground drop-shadow" strokeWidth={2.5} />
      </motion.button>

      {/* Melting effect when an ice streak just broke */}
      <AnimatePresence>
        {melting && (
          <motion.div className="pointer-events-none absolute inset-0 z-20" initial={{ opacity: 1 }} exit={{ opacity: 0 }}>
            {Array.from({ length: 12 }, (_, i) => (
              <motion.span
                key={i}
                className="absolute h-3 w-1.5 rounded-full bg-sky-200/90"
                style={{ left: `${8 + i * 7}%`, top: "55%" }}
                initial={{ y: 0, opacity: 1, scaleY: 0.5 }}
                animate={{ y: 120, opacity: 0, scaleY: 1.6 }}
                transition={{ duration: 1.1, delay: i * 0.05, ease: "easeIn" }}
              />
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
