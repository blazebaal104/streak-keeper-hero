import { motion, AnimatePresence } from "motion/react";
import { Plus } from "lucide-react";
import { useMemo } from "react";
import fireMp4 from "@/assets/sbo/fire-blaze.mp4.asset.json";
import fireWebm from "@/assets/sbo/fire-blaze.webm.asset.json";
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

  // In fire mode the disc is a dark ember bed: the screen-blended real footage on
  // top reads at full saturation and the gaps between flames glow like coals.
  const background =
    mode === "fire"
      ? "radial-gradient(circle at 50% 72%, #5a1600, #1a0400 78%)"
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

      <motion.button
        onClick={onPress}
        whileTap={{ scale: 0.9 }}
        whileHover={{ scale: 1.04 }}
        transition={{ type: "spring", stiffness: 400, damping: 15 }}
        className="group relative flex h-48 w-48 items-center justify-center overflow-hidden rounded-full will-change-transform"
        style={{ background, boxShadow: glow }}
        aria-label="Aggiungi 1 al contatore"
      >
        {/* Fire: the disc is pitch black; the screen-blended video layer on top of
            the button (rendered after it) sets the whole surface ablaze. */}



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

      {/* Real fire, ON TOP of the black disc (single decoder): the whole button
          burns and the flames rise above the rim. Black frames vanish with `screen`. */}
      {mode === "fire" && (
        <div
          className="pointer-events-none absolute inset-x-[-16px] bottom-[24px] top-[-56px] overflow-hidden"
          style={{
            mixBlendMode: "screen",
            maskImage:
              "radial-gradient(ellipse 58% 60% at 50% 82%, #000 66%, rgba(0,0,0,0.85) 82%, transparent 100%)",
            WebkitMaskImage:
              "radial-gradient(ellipse 58% 60% at 50% 82%, #000 66%, rgba(0,0,0,0.85) 82%, transparent 100%)",
          }}
        >
          <FireVideo
            className="absolute inset-0 h-full w-full object-cover"
            style={{
              objectPosition: "50% 100%",
              filter: `saturate(${1.3 + fire * 0.35}) contrast(1.25) brightness(${1.1 + fire * 0.15})`,
            }}
          />
        </div>
      )}



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

