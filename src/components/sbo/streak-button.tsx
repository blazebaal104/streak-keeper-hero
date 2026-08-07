import { motion, AnimatePresence } from "motion/react";
import { Plus } from "lucide-react";
import { useMemo } from "react";
import fireMp4 from "@/assets/sbo/fire-loop.mp4.asset.json";
import fireWebm from "@/assets/sbo/fire-loop.webm.asset.json";
import shardsSheet from "@/assets/sbo/ice-shards.png.asset.json";
import ice1 from "@/assets/sbo/ice-1.webp.asset.json";
import ice2 from "@/assets/sbo/ice-2.webp.asset.json";
import ice3 from "@/assets/sbo/ice-3.webp.asset.json";
import ice4 from "@/assets/sbo/ice-4.webp.asset.json";
import ice5 from "@/assets/sbo/ice-5.webp.asset.json";

/** Texture evolution: each ice badge threshold unlocks a denser, purer ice. */
const ICE_STAGES = [
  { min: 0, url: ice1.url },
  { min: 3, url: ice2.url },
  { min: 7, url: ice3.url },
  { min: 14, url: ice4.url },
  { min: 30, url: ice5.url },
];

function iceStage(streak: number) {
  let s = ICE_STAGES[0];
  for (const st of ICE_STAGES) if (streak >= st.min) s = st;
  return s;
}

/**
 * Main SBO button.
 * - fire streak → real fireplace footage (looping, pre-bundled, screen-blended)
 *   burning inside the disc and licking out past the rim.
 * - ice streak  → real photographic ice that evolves stage by stage with the streak.
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
  const stage = iceStage(iceStreak);

  const background =
    mode === "fire"
      ? "radial-gradient(circle at 50% 70%, oklch(0.42 0.14 45), oklch(0.22 0.08 35))"
      : mode === "ice"
        ? "oklch(0.35 0.06 230)"
        : "var(--gradient-drop)";

  const glow =
    mode === "fire"
      ? `0 24px 80px -14px rgba(249,115,22,${0.4 + fire * 0.45}), 0 0 ${50 + fire * 80}px -12px rgba(255,150,50,${0.35 + fire * 0.4})`
      : mode === "ice"
        ? `0 24px 80px -14px rgba(56,189,248,${0.3 + ice * 0.4}), 0 0 ${45 + ice * 70}px -12px rgba(190,240,255,${0.35 + ice * 0.4})`
        : "var(--shadow-drop)";

  return (
    <div className="relative flex h-64 w-64 items-center justify-center">
      {/* Real flames licking OUT of the rim (black frames removed by screen blend) */}
      {mode === "fire" && (
        <div
          className="pointer-events-none absolute inset-0 overflow-hidden"
          style={{
            mixBlendMode: "screen",
            opacity: 0.55 + fire * 0.45,
            maskImage: "radial-gradient(circle at 50% 58%, #000 34%, rgba(0,0,0,0.55) 52%, transparent 72%)",
            WebkitMaskImage: "radial-gradient(circle at 50% 58%, #000 34%, rgba(0,0,0,0.55) 52%, transparent 72%)",
          }}
        >
          <FireVideo className="absolute left-1/2 top-1/2 h-[112%] w-[112%] -translate-x-1/2 -translate-y-1/2 object-cover" />
        </div>
      )}

      {/* Ice aura */}
      {mode === "ice" && (
        <motion.div
          className="pointer-events-none absolute inset-0 rounded-full"
          style={{ background: "radial-gradient(circle, rgba(140,220,255,0.26), transparent 62%)" }}
          animate={{ scale: [1, 1.04, 1], opacity: [0.45, 0.85, 0.45] }}
          transition={{ duration: 3.6, repeat: Infinity, ease: "easeInOut" }}
        />
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
        {/* Fire: the real footage burning inside the disc */}
        {mode === "fire" && (
          <FireVideo className="absolute inset-0 h-full w-full scale-125 object-cover" style={{ mixBlendMode: "screen" }} />
        )}

        {/* Ice: real photographic ice, crossfaded when the streak reaches a new stage */}
        {mode === "ice" && (
          <AnimatePresence mode="sync">
            <motion.img
              key={stage.url}
              src={stage.url}
              alt=""
              aria-hidden
              width={512}
              height={512}
              initial={{ opacity: 0, scale: 1.12 }}
              animate={{ opacity: 1, scale: 1.02 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.9, ease: "easeOut" }}
              className="absolute inset-0 h-full w-full object-cover"
              style={{ filter: `saturate(${1 + ice * 0.5}) contrast(${1 + ice * 0.25}) brightness(${1.05 - ice * 0.1})` }}
            />
          </AnimatePresence>
        )}

        {/* Glass dome highlight */}
        <div className="pointer-events-none absolute inset-0 rounded-full bg-gradient-to-br from-white/30 via-transparent to-black/35" />
        {mode === "ice" && (
          <div
            className="pointer-events-none absolute inset-0 rounded-full"
            style={{ boxShadow: "inset 0 0 40px rgba(255,255,255,0.45), inset 0 -20px 50px rgba(8,47,73,0.55)" }}
          />
        )}

        <Plus
          className="relative h-16 w-16 drop-shadow-[0_2px_8px_rgba(0,0,0,0.6)]"
          style={{ color: "white" }}
          strokeWidth={2.5}
        />
      </motion.button>

      {/* Ice shatter: real shard fragments blasted out, then pulled down by gravity */}
      <AnimatePresence>{melting && <IceShatter key="shatter" />}</AnimatePresence>
    </div>
  );
}

function FireVideo({ className, style }: { className?: string; style?: React.CSSProperties }) {
  return (
    <video
      className={className}
      style={style}
      autoPlay
      loop
      muted
      playsInline
      preload="auto"
      disablePictureInPicture
      aria-hidden
    >
      <source src={fireWebm.url} type="video/webm" />
      <source src={fireMp4.url} type="video/mp4" />
    </video>
  );
}

const SHARD_COLS = 6;
const SHARD_ROWS = 2;


/** Explosive shatter: shards fly outwards fast, then fall under gravity and spin. */
function IceShatter() {
  const shards = useMemo(() => {
    const n = 42;
    return Array.from({ length: n }, (_, i) => {
      const angle = (i / n) * Math.PI * 2 + (Math.random() - 0.5) * 0.5;
      const speed = 90 + Math.random() * 170;
      const vx = Math.cos(angle) * speed;
      const vy = Math.sin(angle) * speed - 60; // slight upward bias
      const size = 12 + Math.random() * 30;
      const cell = Math.floor(Math.random() * (SHARD_COLS * SHARD_ROWS));
      const g = 900 + Math.random() * 400;
      const T = 1.15;
      // sample a parabolic trajectory
      const steps = 6;
      const xs: number[] = [];
      const ys: number[] = [];
      const times: number[] = [];
      for (let s = 0; s <= steps; s++) {
        const t = (s / steps) * T;
        xs.push(vx * t);
        ys.push(vy * t + 0.5 * g * t * t);
        times.push(s / steps);
      }
      return {
        i,
        xs,
        ys,
        times,
        size,
        cell,
        spin: (Math.random() - 0.5) * 900,
        delay: Math.random() * 0.06,
      };
    });
  }, []);

  return (
    <motion.div className="pointer-events-none absolute inset-0" exit={{ opacity: 0 }}>
      {/* burst flash */}
      <motion.div
        className="absolute inset-0 rounded-full"
        style={{ background: "radial-gradient(circle, rgba(220,245,255,0.85), transparent 60%)" }}
        initial={{ opacity: 0.9, scale: 0.6 }}
        animate={{ opacity: 0, scale: 1.5 }}
        transition={{ duration: 0.35, ease: "easeOut" }}
      />
      {shards.map((s) => (
        <motion.div
          key={s.i}
          className="absolute left-1/2 top-1/2 will-change-transform"
          style={{
            width: s.size,
            height: s.size,
            marginLeft: -s.size / 2,
            marginTop: -s.size / 2,
            backgroundImage: `url(${shardsSheet.url})`,
            backgroundSize: `${SHARD_COLS * 100}% ${SHARD_ROWS * 100}%`,
            backgroundPosition: `${(s.cell % SHARD_COLS) * (100 / (SHARD_COLS - 1))}% ${Math.floor(s.cell / SHARD_COLS) * 100}%`,
            filter: "drop-shadow(0 2px 4px rgba(8,47,73,0.5))",
          }}
          initial={{ x: 0, y: 0, opacity: 1, rotate: 0, scale: 0.5 }}
          animate={{
            x: s.xs,
            y: s.ys,
            rotate: [0, s.spin],
            scale: [0.5, 1, 1, 1, 1, 0.95, 0.85],
            opacity: [1, 1, 1, 1, 1, 0.7, 0],
          }}
          transition={{
            duration: 1.15,
            delay: s.delay,
            ease: "linear",
            times: s.times,
            rotate: { duration: 1.15, delay: s.delay, ease: "linear" },
          }}
        />
      ))}
    </motion.div>
  );
}

