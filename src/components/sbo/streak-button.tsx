import { motion, AnimatePresence } from "motion/react";
import { Plus } from "lucide-react";
import { useMemo } from "react";
// One fire source drives the whole effect. The disc and the flame above it are
// two visible areas of the same video surface, so they can never drift apart.
const fireMp4 = { url: "/fire-loop.mp4" };
const fireWebm = { url: "/fire-loop.webm" };
import shardsSheet from "@/assets/sbo/ice-shards.png.asset.json";
import ice1 from "@/assets/sbo/ice-1.webp.asset.json";
import ice2 from "@/assets/sbo/ice-2.webp.asset.json";
import ice3 from "@/assets/sbo/ice-3.webp.asset.json";
import ice4 from "@/assets/sbo/ice-4.webp.asset.json";
import ice5 from "@/assets/sbo/ice-5.webp.asset.json";

/**
 * Texture evolution. The five photographic plates are the anchors, but the look
 * is interpolated DAY BY DAY: every single day the next plate bleeds a little
 * more through and the frost gets denser, so progress is visible daily.
 */
const ICE_PLATES = [
  { day: 0, url: ice1.url },
  { day: 3, url: ice2.url },
  { day: 7, url: ice3.url },
  { day: 14, url: ice4.url },
  { day: 30, url: ice5.url },
];

function iceBlend(streak: number) {
  const last = ICE_PLATES.length - 1;
  if (streak >= ICE_PLATES[last].day) return { base: ICE_PLATES[last].url, next: ICE_PLATES[last].url, f: 1 };
  let i = 0;
  for (let k = 0; k < last; k++) if (streak >= ICE_PLATES[k].day) i = k;
  const a = ICE_PLATES[i];
  const b = ICE_PLATES[i + 1];
  const f = Math.min(1, Math.max(0, (streak - a.day) / (b.day - a.day)));
  return { base: a.url, next: b.url, f };
}

/**
 * Main SBO button.
 * - fire streak → real fireplace footage burning strictly INSIDE the disc, with
 *   just a small plume licking above the top rim. It ignites progressively
 *   (~2s) every time the fire starts.
 * - ice streak  → real photographic ice whose density grows every single day.
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
  const blend = iceBlend(iceStreak);

  const background =
    mode === "fire"
      ? "radial-gradient(circle at 50% 72%, rgba(90,22,0,0.32), rgba(26,4,0,0.48) 78%)"
      : mode === "ice"
        ? "oklch(0.35 0.06 230)"
        : "var(--gradient-drop)";

  const glow =
    mode === "fire"
      ? `0 18px 70px -10px rgba(249,115,22,${0.6 + fire * 0.4}), 0 0 ${70 + fire * 90}px -8px rgba(255,120,20,${0.55 + fire * 0.45})`
      : mode === "ice"
        ? `0 24px 80px -14px rgba(56,189,248,${0.3 + ice * 0.4}), 0 0 ${45 + ice * 70}px -12px rgba(190,240,255,${0.35 + ice * 0.4})`
        : "var(--shadow-drop)";

  return (
    <div className="relative flex h-64 w-64 items-center justify-center">
      {/* Ice aura */}
      {mode === "ice" && (
        <motion.div
          className="pointer-events-none absolute inset-0 rounded-full"
          style={{ background: "radial-gradient(circle, rgba(140,220,255,0.26), transparent 62%)" }}
          animate={{ scale: [1, 1.04, 1], opacity: [0.45, 0.85, 0.45] }}
          transition={{ duration: 3.6, repeat: Infinity, ease: "easeInOut" }}
        />
      )}

      {/* A single continuous fire surface spans both the disc and its upper
          plume. The clip path only reveals those areas; there is no second
          player, duplicated loop, or independently advancing timestamp. */}
      {mode === "fire" && (
        <motion.div
          key="fire-surface"
          className="pointer-events-none absolute inset-0 z-10 overflow-hidden will-change-transform"
          style={{
            clipPath:
              "path('M 128 0 C 111 10 99 20 95 37 C 58 51 32 86 32 128 A 96 96 0 1 0 224 128 C 224 86 198 51 161 37 C 157 20 145 10 128 0 Z')",
            mixBlendMode: "screen",
            transformOrigin: "50% 87.5%",
          }}
          initial={{ opacity: 0, scaleY: 0.18, filter: "brightness(0.5)" }}
          animate={{ opacity: 1, scaleY: 1, filter: "brightness(1)" }}
          transition={{ duration: 2, ease: [0.22, 0.8, 0.3, 1] }}
        >
          <FireVideo
            className="absolute inset-0 h-full w-full object-cover"
            style={{
              objectPosition: "50% 100%",
              transform: "scale(1.08)",
              filter: `saturate(${1.35 + fire * 0.35}) contrast(1.3) brightness(${1.12 + fire * 0.15})`,
            }}
          />
        </motion.div>
      )}

      <motion.button
        onClick={onPress}
        whileTap={{ scale: 0.9 }}
        whileHover={{ scale: 1.04 }}
        transition={{ type: "spring", stiffness: 400, damping: 15 }}
        className="group relative z-20 flex h-48 w-48 items-center justify-center overflow-hidden rounded-full will-change-transform"
        style={{ background, boxShadow: glow }}
        aria-label="Aggiungi 1 al contatore"
      >
        {/* Ice: two photographic plates crossfaded day by day */}
        {mode === "ice" && (
          <>
            <img
              src={blend.base}
              alt=""
              aria-hidden
              width={512}
              height={512}
              className="absolute inset-0 h-full w-full object-cover"
              style={{ filter: `saturate(${1 + ice * 0.5}) contrast(${1 + ice * 0.25}) brightness(${1.05 - ice * 0.1})` }}
            />
            <motion.img
              key={blend.next}
              src={blend.next}
              alt=""
              aria-hidden
              width={512}
              height={512}
              initial={{ opacity: 0 }}
              animate={{ opacity: blend.f }}
              transition={{ duration: 0.8, ease: "easeOut" }}
              className="absolute inset-0 h-full w-full object-cover"
              style={{ filter: `saturate(${1 + ice * 0.5}) contrast(${1 + ice * 0.25}) brightness(${1.05 - ice * 0.1})` }}
            />
            {/* Daily frost build-up: grows a notch every single day */}
            <div
              className="pointer-events-none absolute inset-0"
              style={{
                background: `radial-gradient(circle at 42% 34%, rgba(255,255,255,${0.06 + ice * 0.3}), transparent 62%)`,
                opacity: 0.5 + ice * 0.5,
              }}
            />
          </>
        )}

        {/* Glass dome highlight (kept subtle over the fire so the footage stays vivid) */}
        <div
          className={`pointer-events-none absolute inset-0 rounded-full bg-gradient-to-br ${
            mode === "fire" ? "from-white/10 via-transparent to-black/10" : "from-white/30 via-transparent to-black/35"
          }`}
        />

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
    const n = 46;
    return Array.from({ length: n }, (_, i) => {
      const angle = (i / n) * Math.PI * 2 + (Math.random() - 0.5) * 0.6;
      const speed = 260 + Math.random() * 420;
      const vx = Math.cos(angle) * speed;
      const vy = Math.sin(angle) * speed - 110; // slight upward bias
      const r0 = 30 + Math.random() * 45; // start on the rim, not in one point
      const size = 7 + Math.random() * 22;
      const cell = Math.floor(Math.random() * (SHARD_COLS * SHARD_ROWS));
      const g = 1500 + Math.random() * 700;
      const T = 1.05;
      // sample a parabolic trajectory (ballistic, no easing)
      const steps = 8;
      const xs: number[] = [];
      const ys: number[] = [];
      const times: number[] = [];
      for (let s = 0; s <= steps; s++) {
        const t = (s / steps) * T;
        xs.push(Math.cos(angle) * r0 + vx * t);
        ys.push(Math.sin(angle) * r0 + vy * t + 0.5 * g * t * t);
        times.push(s / steps);
      }
      return {
        i,
        xs,
        ys,
        times,
        size,
        cell,
        spin: (Math.random() - 0.5) * 1200,
        delay: Math.random() * 0.05,
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
            filter: "drop-shadow(0 2px 4px rgba(8,47,73,0.5)) hue-rotate(-10deg) saturate(1.4)",
          }}
          initial={{ x: 0, y: 0, opacity: 1, rotate: 0, scale: 1 }}
          animate={{
            x: s.xs,
            y: s.ys,
            rotate: [0, s.spin],
            opacity: [1, 1, 1, 1, 1, 1, 0.9, 0.6, 0],
          }}
          transition={{
            duration: 1.05,
            delay: s.delay,
            ease: "linear",
            times: s.times,
            rotate: { duration: 1.05, delay: s.delay, ease: "linear" },
          }}
        />
      ))}
    </motion.div>
  );
}
