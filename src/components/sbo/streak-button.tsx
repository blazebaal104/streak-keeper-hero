import { motion, AnimatePresence } from "motion/react";
import { Plus } from "lucide-react";

/**
 * Main SBO button.
 * - fire streak → warm gradient + stylized flame tongues that grow OUT of the
 *   button rim (no separate overlay artwork sitting on top of the circle).
 * - ice streak  → frozen shell whose surface cracks get denser as the streak grows.
 * - neutral     → the plain water-drop look.
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
        ? `0 20px 70px -12px rgba(56,189,248,${0.3 + ice * 0.4}), 0 0 ${40 + ice * 70}px -10px rgba(180,235,255,${0.35 + ice * 0.4})`
        : "var(--shadow-drop)";

  return (
    <div className="relative flex h-64 w-64 items-center justify-center">
      {/* Fire aura */}
      {mode === "fire" && (
        <motion.div
          className="pointer-events-none absolute inset-0 rounded-full"
          style={{ background: "radial-gradient(circle, rgba(255,140,40,0.32), transparent 62%)" }}
          animate={{ scale: [1, 1.05 + fire * 0.05, 1], opacity: [0.55, 0.95, 0.55] }}
          transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
        />
      )}
      {/* Ice aura */}
      {mode === "ice" && (
        <motion.div
          className="pointer-events-none absolute inset-0 rounded-full"
          style={{ background: "radial-gradient(circle, rgba(140,220,255,0.28), transparent 62%)" }}
          animate={{ scale: [1, 1.04, 1], opacity: [0.45, 0.85, 0.45] }}
          transition={{ duration: 3.6, repeat: Infinity, ease: "easeInOut" }}
        />
      )}

      {/* Stylized flame tongues licking out of the button rim */}
      {mode === "fire" && <FlameRing intensity={fire} />}

      <motion.button
        onClick={onPress}
        whileTap={{ scale: 0.9 }}
        whileHover={{ scale: 1.04 }}
        transition={{ type: "spring", stiffness: 400, damping: 15 }}
        className="group relative flex h-48 w-48 items-center justify-center overflow-hidden rounded-full will-change-transform"
        style={{ background, boxShadow: glow }}
        aria-label="Aggiungi 1 al contatore"
      >
        <div className="absolute inset-2 rounded-full bg-gradient-to-br from-white/45 to-transparent" />

        {/* Ice: surface cracks that get denser with the streak */}
        {mode === "ice" && <IceCracks intensity={ice} />}

        {/* Fire: heat shimmer inside the disc */}
        {mode === "fire" && (
          <motion.div
            className="pointer-events-none absolute inset-0"
            style={{ background: "radial-gradient(circle at 50% 85%, rgba(255,210,120,0.55), transparent 62%)" }}
            animate={{ opacity: [0.45, 0.9, 0.45] }}
            transition={{ duration: 2.1, repeat: Infinity, ease: "easeInOut" }}
          />
        )}

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

/**
 * Flame tongues placed around the circle rim, pointing outwards.
 * They start *behind* the button (their base is under the disc) so they read as
 * flames coming out of the button instead of an image pasted on top.
 */
function FlameRing({ intensity }: { intensity: number }) {
  const count = 7 + Math.round(intensity * 5);
  // Only the upper half of the circle (from -150° to -30°) plus small side licks.
  const tongues = Array.from({ length: count }, (_, i) => {
    const t = count === 1 ? 0.5 : i / (count - 1);
    const angle = -160 + t * 140; // degrees
    const centerness = 1 - Math.abs(t - 0.5) * 2; // 0 at edges, 1 in the middle
    const len = 26 + centerness * (34 + intensity * 46);
    const w = 12 + centerness * (8 + intensity * 8);
    return { angle, len, w, delay: (i % 5) * 0.17, i };
  });

  return (
    <div className="pointer-events-none absolute inset-0">
      {tongues.map((f) => (
        <div
          key={f.i}
          className="absolute left-1/2 top-1/2"
          style={{ transform: `rotate(${f.angle + 90}deg)` }}
        >
          <motion.div
            style={{ transformOrigin: "50% 100%", position: "absolute", left: -f.w / 2, top: -96 - f.len }}
            animate={{ scaleY: [0.86, 1.08, 0.94, 0.86], scaleX: [1, 0.92, 1.04, 1], opacity: [0.75, 1, 0.85, 0.75] }}
            transition={{ duration: 1 + (f.i % 3) * 0.25, delay: f.delay, repeat: Infinity, ease: "easeInOut" }}
          >
            <svg width={f.w} height={f.len + 6} viewBox="0 0 20 100" preserveAspectRatio="none">
              <defs>
                <linearGradient id={`ft-${f.i}`} x1="0" y1="1" x2="0" y2="0">
                  <stop offset="0%" stopColor="#f97316" stopOpacity="0.95" />
                  <stop offset="45%" stopColor="#fb923c" stopOpacity="0.85" />
                  <stop offset="100%" stopColor="#fde68a" stopOpacity="0.15" />
                </linearGradient>
              </defs>
              <path d="M10 100 C 20 70, 18 46, 10 0 C 2 46, 0 70, 10 100 Z" fill={`url(#ft-${f.i})`} />
            </svg>
          </motion.div>
        </div>
      ))}

      {/* Embers drifting up from the rim */}
      {Array.from({ length: 5 + Math.round(intensity * 7) }, (_, i) => (
        <motion.span
          key={`e${i}`}
          className="absolute h-1 w-1 rounded-full bg-amber-200"
          style={{ left: `${18 + ((i * 29) % 64)}%`, top: "34%" }}
          animate={{ y: [0, -60 - intensity * 50], opacity: [0, 0.9, 0], scale: [0.5, 1, 0.3] }}
          transition={{ duration: 1.8 + (i % 4) * 0.4, delay: (i % 6) * 0.32, repeat: Infinity, ease: "easeOut" }}
        />
      ))}
    </div>
  );
}

/** Deterministic crack network: more (and deeper) cracks as the ice streak grows. */
function IceCracks({ intensity }: { intensity: number }) {
  const rnd = (i: number) => {
    const x = Math.sin(i * 127.1 + 311.7) * 43758.5453;
    return x - Math.floor(x);
  };
  const main = 3 + Math.round(intensity * 7); // 3 → 10 main cracks
  const cracks = Array.from({ length: main }, (_, i) => {
    const a = rnd(i) * Math.PI * 2;
    const startR = 6 + rnd(i + 20) * 12;
    const endR = 30 + rnd(i + 40) * 18;
    const x1 = 50 + Math.cos(a) * startR;
    const y1 = 50 + Math.sin(a) * startR;
    const bend = (rnd(i + 60) - 0.5) * 0.9;
    const mx = 50 + Math.cos(a + bend * 0.5) * ((startR + endR) / 2);
    const my = 50 + Math.sin(a + bend * 0.5) * ((startR + endR) / 2);
    const x2 = 50 + Math.cos(a + bend) * endR;
    const y2 = 50 + Math.sin(a + bend) * endR;
    // small branch
    const bx = x2 + Math.cos(a + bend + 1.1) * (5 + rnd(i + 80) * 8);
    const by = y2 + Math.sin(a + bend + 1.1) * (5 + rnd(i + 80) * 8);
    return { d: `M${x1} ${y1} Q${mx} ${my} ${x2} ${y2}`, branch: `M${x2} ${y2} L${bx} ${by}`, i };
  });

  return (
    <svg viewBox="0 0 100 100" className="pointer-events-none absolute inset-0 h-full w-full">
      {/* frosted surface */}
      <circle cx="50" cy="50" r="49" fill="rgba(255,255,255,0.10)" />
      <g strokeLinecap="round" fill="none">
        {cracks.map((c) => (
          <g key={c.i}>
            {/* dark fissure */}
            <path d={c.d} stroke="rgba(12,60,95,0.45)" strokeWidth={1.6 + intensity * 0.9} />
            <path d={c.branch} stroke="rgba(12,60,95,0.35)" strokeWidth={1.1 + intensity * 0.6} />
            {/* bright highlight edge */}
            <motion.path
              d={c.d}
              stroke="rgba(255,255,255,0.9)"
              strokeWidth={0.7 + intensity * 0.5}
              animate={{ opacity: [0.35, 0.85, 0.35] }}
              transition={{ duration: 3 + (c.i % 4) * 0.6, repeat: Infinity, ease: "easeInOut" }}
            />
            <path d={c.branch} stroke="rgba(255,255,255,0.6)" strokeWidth={0.5 + intensity * 0.3} />
          </g>
        ))}
      </g>
      {/* fine hairline fractures appear only on long streaks */}
      {intensity > 0.35 && (
        <g stroke="rgba(255,255,255,0.35)" strokeWidth="0.4" fill="none">
          {Array.from({ length: Math.round(intensity * 14) }, (_, i) => {
            const a = rnd(i + 200) * Math.PI * 2;
            const r = 18 + rnd(i + 220) * 26;
            const x = 50 + Math.cos(a) * r;
            const y = 50 + Math.sin(a) * r;
            const l = 4 + rnd(i + 240) * 7;
            return <path key={i} d={`M${x} ${y} l${Math.cos(a + 1) * l} ${Math.sin(a + 1) * l}`} />;
          })}
        </g>
      )}
    </svg>
  );
}
