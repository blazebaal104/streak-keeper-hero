import { motion, AnimatePresence } from "motion/react";
import { Plus } from "lucide-react";

/**
 * Main SBO button. Its look reacts to the two streaks:
 * - fire streak → warm gradient, a live flame that grows with the streak, embers
 * - ice streak  → frozen shell, frost crystals, cold glow
 * When both are 0 it falls back to the neutral water-drop look.
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
  const mode: "fire" | "ice" | "neutral" = fireStreak > 0 ? "fire" : iceStreak > 0 ? "ice" : "neutral";

  const background =
    mode === "fire"
      ? `linear-gradient(160deg, oklch(${0.92 - fire * 0.06} ${0.06 + fire * 0.12} ${70 - fire * 20}), oklch(${0.7 - fire * 0.08} ${0.15 + fire * 0.07} ${45 - fire * 20}))`
      : mode === "ice"
        ? `linear-gradient(160deg, oklch(${0.97 - ice * 0.03} 0.03 220), oklch(${0.78 - ice * 0.06} ${0.09 + ice * 0.06} ${230 + ice * 15}))`
        : "var(--gradient-drop)";

  const glow =
    mode === "fire"
      ? `0 20px 70px -12px rgba(249,115,22,${0.35 + fire * 0.45}), 0 0 ${40 + fire * 70}px -10px rgba(255,160,60,${0.3 + fire * 0.4})`
      : mode === "ice"
        ? `0 20px 70px -12px rgba(56,189,248,${0.3 + ice * 0.4}), 0 0 ${40 + ice * 70}px -10px rgba(180,235,255,${0.3 + ice * 0.4})`
        : "var(--shadow-drop)";

  return (
    <div className="relative flex h-64 w-64 items-center justify-center">
      {/* Fire aura */}
      {mode === "fire" && (
        <motion.div
          className="pointer-events-none absolute inset-0 rounded-full"
          style={{ background: "radial-gradient(circle, rgba(255,140,40,0.35), transparent 62%)" }}
          animate={{ scale: [1, 1.06 + fire * 0.06, 1], opacity: [0.6, 1, 0.6] }}
          transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
        />
      )}
      {/* Ice aura */}
      {mode === "ice" && (
        <motion.div
          className="pointer-events-none absolute inset-0 rounded-full"
          style={{ background: "radial-gradient(circle, rgba(140,220,255,0.32), transparent 62%)" }}
          animate={{ scale: [1, 1.05, 1], opacity: [0.5, 0.95, 0.5] }}
          transition={{ duration: 3.4, repeat: Infinity, ease: "easeInOut" }}
        />
      )}

      {/* Flame that grows with the fire streak */}
      {mode === "fire" && <Flame intensity={fire} />}

      {/* Ice crown crystals */}
      {mode === "ice" && <IceCrown intensity={ice} />}

      <motion.button
        onClick={onPress}
        whileTap={{ scale: 0.9 }}
        whileHover={{ scale: 1.04 }}
        transition={{ type: "spring", stiffness: 400, damping: 15 }}
        className="group relative flex h-48 w-48 items-center justify-center rounded-full will-change-transform"
        style={{ background, boxShadow: glow }}
        aria-label="Aggiungi 1 al contatore"
      >
        <div className="absolute inset-2 rounded-full bg-gradient-to-br from-white/45 to-transparent" />

        {/* Frost shell */}
        {mode === "ice" && (
          <svg viewBox="0 0 100 100" className="absolute inset-0 h-full w-full opacity-70">
            <g stroke="white" strokeWidth="0.9" fill="none" opacity={0.5 + ice * 0.4}>
              <path d="M50 4 L50 96 M4 50 L96 50 M16 16 L84 84 M84 16 L16 84" />
              <path d="M50 20 L38 30 M50 20 L62 30 M50 80 L38 70 M50 80 L62 70 M20 50 L30 38 M20 50 L30 62 M80 50 L70 38 M80 50 L70 62" />
            </g>
          </svg>
        )}

        {/* Embers rising from the surface */}
        {mode === "fire" &&
          Array.from({ length: 6 + Math.round(fire * 8) }, (_, i) => (
            <motion.span
              key={i}
              className="absolute bottom-6 h-1.5 w-1.5 rounded-full bg-amber-200"
              style={{ left: `${12 + ((i * 37) % 76)}%` }}
              animate={{ y: [-4, -70 - fire * 40], opacity: [0, 1, 0], scale: [0.6, 1, 0.3] }}
              transition={{ duration: 1.8 + (i % 4) * 0.4, delay: (i % 6) * 0.35, repeat: Infinity, ease: "easeOut" }}
            />
          ))}

        <Plus className="relative h-16 w-16 text-primary-foreground" strokeWidth={2.5} />
      </motion.button>

      {/* Melting effect when an ice streak just broke */}
      <AnimatePresence>
        {melting && (
          <motion.div className="pointer-events-none absolute inset-0" initial={{ opacity: 1 }} exit={{ opacity: 0 }}>
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

function Flame({ intensity }: { intensity: number }) {
  const scale = 0.75 + intensity * 0.75;
  return (
    <motion.div
      className="pointer-events-none absolute"
      style={{ top: -18 - intensity * 26, transformOrigin: "bottom center" }}
      animate={{ scale: [scale, scale * 1.08, scale * 0.96, scale], rotate: [-2, 2, -1, -2] }}
      transition={{ duration: 1.2, repeat: Infinity, ease: "easeInOut" }}
    >
      <svg width={96} height={130} viewBox="0 0 96 130">
        <defs>
          <linearGradient id="flameOuter" x1="0" y1="1" x2="0" y2="0">
            <stop offset="0%" stopColor="#f97316" stopOpacity="0.95" />
            <stop offset="55%" stopColor="#fb923c" stopOpacity="0.9" />
            <stop offset="100%" stopColor="#fde68a" stopOpacity="0.6" />
          </linearGradient>
          <linearGradient id="flameInner" x1="0" y1="1" x2="0" y2="0">
            <stop offset="0%" stopColor="#fde047" />
            <stop offset="100%" stopColor="#fff7ed" />
          </linearGradient>
        </defs>
        <motion.path
          d="M48 4 C 66 34, 86 52, 86 78 A 38 38 0 0 1 10 78 C 10 52, 30 40, 48 4 Z"
          fill="url(#flameOuter)"
          animate={{ scaleY: [1, 1.08, 0.97, 1] }}
          transition={{ duration: 0.9, repeat: Infinity, ease: "easeInOut" }}
          style={{ transformOrigin: "50% 100%" }}
        />
        <motion.path
          d="M48 36 C 58 54, 68 64, 68 80 A 20 20 0 0 1 28 80 C 28 64, 40 56, 48 36 Z"
          fill="url(#flameInner)"
          animate={{ scaleY: [1, 0.9, 1.1, 1], opacity: [0.9, 1, 0.85, 0.9] }}
          transition={{ duration: 0.7, repeat: Infinity, ease: "easeInOut" }}
          style={{ transformOrigin: "50% 100%" }}
        />
      </svg>
    </motion.div>
  );
}

function IceCrown({ intensity }: { intensity: number }) {
  return (
    <motion.div
      className="pointer-events-none absolute"
      style={{ top: -10 - intensity * 18 }}
      animate={{ y: [0, -4, 0], opacity: [0.85, 1, 0.85] }}
      transition={{ duration: 3.2, repeat: Infinity, ease: "easeInOut" }}
    >
      <svg width={120} height={70} viewBox="0 0 120 70">
        <defs>
          <linearGradient id="iceShard" x1="0" y1="1" x2="0" y2="0">
            <stop offset="0%" stopColor="#38bdf8" stopOpacity="0.8" />
            <stop offset="100%" stopColor="#ffffff" stopOpacity="0.95" />
          </linearGradient>
        </defs>
        {[
          "M18 68 L28 26 L38 68 Z",
          "M42 68 L60 4 L78 68 Z",
          "M82 68 L92 26 L102 68 Z",
        ].map((d, i) => (
          <motion.path
            key={i}
            d={d}
            fill="url(#iceShard)"
            animate={{ opacity: [0.6, 1, 0.6] }}
            transition={{ duration: 2.4, delay: i * 0.4, repeat: Infinity, ease: "easeInOut" }}
          />
        ))}
      </svg>
    </motion.div>
  );
}
