import { motion } from "motion/react";
import { useEffect, useMemo } from "react";
import logo from "@/assets/sbo-logo.png";
import { useHoliday } from "@/lib/sbo-holiday";

const BAT = "M0 10C4 4 9 4 12 8C13 5 15 4 16 6L17 3L18 6C19 4 21 5 22 8C25 4 30 4 34 10C30 9 28 11 27 14C25 11 22 12 21 14C20 12 18 12 17 15C16 12 14 12 13 14C12 12 9 11 7 14C6 11 4 9 0 10Z";

/** Sets html[data-holiday] and draws the night behind the app: moon, bats, cobwebs, fog. */
export function HolidayLayer() {
  const holiday = useHoliday();
  useEffect(() => {
    const el = document.documentElement;
    if (holiday) el.dataset.holiday = holiday;
    else delete el.dataset.holiday;
  }, [holiday]);
  if (holiday !== "halloween") return null;
  return <HalloweenNight />;
}

function HalloweenNight() {
  const bats = useMemo(
    () => Array.from({ length: 7 }, (_, i) => ({
      i, top: 6 + Math.random() * 45, size: 18 + Math.random() * 26, dur: 14 + Math.random() * 14,
      delay: i * 2.6 + Math.random() * 2, dir: i % 2 ? 1 : -1, bob: 10 + Math.random() * 25,
    })),
    [],
  );
  return (
    <div aria-hidden className="pointer-events-none fixed inset-0 -z-10 overflow-hidden">
      {/* moon */}
      <div className="absolute -right-10 top-10 h-44 w-44 rounded-full"
        style={{ background: "radial-gradient(circle at 40% 40%, #fff7d6, #f5d58a 45%, #d9a441 70%)", boxShadow: "0 0 90px 30px rgba(245,213,138,0.22)", opacity: 0.85 }} />
      {/* cobwebs in the top corners */}
      <Cobweb className="absolute -left-2 -top-2 h-40 w-40" />
      <Cobweb className="absolute -right-2 -top-2 h-32 w-32 -scale-x-100" />
      {/* bats crossing the sky */}
      {bats.map((b) => (
        <motion.svg key={b.i} viewBox="0 0 34 16" className="absolute"
          style={{ top: `${b.top}%`, width: b.size, left: b.dir > 0 ? "-10%" : "110%", fill: "#120a1c", filter: "drop-shadow(0 0 2px rgba(180,120,255,0.35))" }}
          animate={{ x: b.dir > 0 ? ["0vw", "125vw"] : ["0vw", "-125vw"], y: [0, -b.bob, b.bob, 0], scaleY: [1, 0.55, 1] }}
          transition={{
            x: { duration: b.dur, delay: b.delay, repeat: Infinity, ease: "linear" },
            y: { duration: 3.2, repeat: Infinity, ease: "easeInOut" },
            scaleY: { duration: 0.28, repeat: Infinity, ease: "easeInOut" },
          }}>
          <path d={BAT} />
        </motion.svg>
      ))}
      {/* a witch on her broom, crossing in front of the moon now and then */}
      <motion.svg viewBox="0 0 120 60" className="absolute w-24" style={{ top: "9%", left: "-20%", fill: "#120a1c" }}
        animate={{ x: ["0vw", "140vw"], y: [40, -10, 25, -5] }}
        transition={{ x: { duration: 11, repeat: Infinity, repeatDelay: 16, ease: "easeInOut" }, y: { duration: 11, repeat: Infinity, repeatDelay: 16, ease: "easeInOut" } }}>
        <Witch />
      </motion.svg>
      {/* graveyard and abandoned church along the bottom */}
      <Graveyard />
      {/* low graveyard fog */}
      <motion.div className="absolute inset-x-[-20%] bottom-0 h-56"
        style={{ background: "radial-gradient(ellipse 60% 100% at 30% 100%, rgba(190,170,230,0.20), transparent 70%), radial-gradient(ellipse 60% 100% at 75% 100%, rgba(160,200,180,0.14), transparent 70%)" }}
        animate={{ x: ["-4%", "4%", "-4%"] }} transition={{ duration: 18, repeat: Infinity, ease: "easeInOut" }} />
    </div>
  );
}

function Witch() {
  return (
    <g>
      {/* broom */}
      <line x1="8" y1="44" x2="92" y2="30" stroke="#120a1c" strokeWidth="2.6" strokeLinecap="round" />
      <path d="M88 31 L112 22 L116 30 L114 38 L90 34 Z" />
      <path d="M96 31 L118 26 M96 33 L119 33 M95 35 L116 39" stroke="#120a1c" strokeWidth="1.2" />
      {/* cape flapping behind */}
      <motion.path d="M48 20 Q30 26 22 44 Q36 38 50 36 Z" animate={{ d: ["M48 20 Q30 26 22 44 Q36 38 50 36 Z", "M48 20 Q28 22 16 36 Q34 36 50 36 Z", "M48 20 Q30 26 22 44 Q36 38 50 36 Z"] }}
        transition={{ duration: 0.7, repeat: Infinity, ease: "easeInOut" }} />
      {/* body, legs, head and hat */}
      <path d="M46 36 Q52 18 60 18 Q66 22 64 36 Z" />
      <path d="M56 35 L48 46 L52 47 L60 36 Z" />
      <circle cx="60" cy="14" r="5" />
      <path d="M52 11 L70 11 L72 13 L50 13 Z" />
      <path d="M56 11 L64 11 L58 -2 Z" />
      {/* nose and hair */}
      <path d="M64 14 L69 16 L64 16 Z" />
      <path d="M56 15 Q50 20 52 26 Q54 20 58 17 Z" />
    </g>
  );
}

/** Silhouette skyline: hills, tombstones, crosses, a dead tree, an iron fence and a ruined church. */
function Graveyard() {
  const stones = [
    { x: 40, w: 22, h: 30 }, { x: 96, w: 18, h: 24 }, { x: 330, w: 20, h: 28 }, { x: 372, w: 16, h: 20 },
    { x: 600, w: 22, h: 30 }, { x: 650, w: 18, h: 22 }, { x: 860, w: 24, h: 32 }, { x: 905, w: 16, h: 20 },
  ];
  const crosses = [{ x: 140, h: 34 }, { x: 410, h: 30 }, { x: 700, h: 36 }, { x: 950, h: 30 }];
  return (
    <svg viewBox="0 0 1000 220" preserveAspectRatio="xMidYMax slice" className="absolute inset-x-0 bottom-0 h-48 w-full sm:h-56">
      <defs>
        <linearGradient id="hw-ground" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#1a0f29" />
          <stop offset="1" stopColor="#07040c" />
        </linearGradient>
        <radialGradient id="hw-window" cx="50%" cy="50%" r="50%">
          <stop offset="0" stopColor="#ffd27a" />
          <stop offset="1" stopColor="#f97316" />
        </radialGradient>
      </defs>
      {/* far hill */}
      <path d="M0 150 Q160 110 330 140 T680 128 T1000 140 L1000 220 L0 220 Z" fill="#1d1230" opacity="0.9" />
      {/* ruined church on the far hill */}
      <g transform="translate(470 58)" fill="#140b22">
        <rect x="0" y="40" width="90" height="70" />
        <path d="M-6 42 L45 6 L96 42 Z" />
        <rect x="64" y="-6" width="24" height="48" />
        <path d="M60 -4 L76 -34 L92 -4 Z" />
        <rect x="74" y="-50" width="4" height="18" />
        <rect x="68" y="-44" width="16" height="4" />
        {/* broken roof gap */}
        <path d="M18 30 L30 22 L26 34 Z" fill="#1d1230" />
        {/* glowing windows */}
        <motion.g animate={{ opacity: [0.55, 1, 0.7, 0.95, 0.55] }} transition={{ duration: 4, repeat: Infinity }}>
          <path d="M14 70 Q14 58 22 58 Q30 58 30 70 L30 88 L14 88 Z" fill="url(#hw-window)" />
          <path d="M58 70 Q58 58 66 58 Q74 58 74 70 L74 88 L58 88 Z" fill="url(#hw-window)" opacity="0.8" />
          <circle cx="76" cy="12" r="5" fill="url(#hw-window)" />
        </motion.g>
        <path d="M36 110 L36 84 Q45 74 54 84 L54 110 Z" fill="#0b0613" />
      </g>
      {/* dead tree */}
      <g stroke="#0c0716" strokeLinecap="round" fill="none">
        <path d="M800 160 Q806 120 798 92" strokeWidth="9" />
        <path d="M800 118 Q830 100 850 78 M846 82 Q856 76 866 78 M802 104 Q772 90 760 66 M764 72 Q752 66 744 70 M799 94 Q802 74 818 60" strokeWidth="4" />
      </g>
      {/* near hill */}
      <path d="M0 180 Q120 156 260 172 T560 168 T820 174 T1000 166 L1000 220 L0 220 Z" fill="url(#hw-ground)" />
      {/* tombstones */}
      {stones.map((t, i) => (
        <path key={i} d={`M${t.x} ${190} L${t.x} ${190 - t.h + t.w / 2} Q${t.x} ${190 - t.h} ${t.x + t.w / 2} ${190 - t.h} Q${t.x + t.w} ${190 - t.h} ${t.x + t.w} ${190 - t.h + t.w / 2} L${t.x + t.w} 190 Z`}
          fill="#120a1c" transform={`rotate(${(i % 3) * 4 - 4} ${t.x + t.w / 2} 190)`} />
      ))}
      {crosses.map((c, i) => (
        <g key={i} fill="#120a1c" transform={`rotate(${i % 2 ? 6 : -5} ${c.x} 190)`}>
          <rect x={c.x - 2.5} y={190 - c.h} width="5" height={c.h} />
          <rect x={c.x - 10} y={190 - c.h + 8} width="20" height="5" />
        </g>
      ))}
      {/* iron fence */}
      <g fill="#0e0818">
        <rect x="190" y="176" width="120" height="3" />
        <rect x="190" y="190" width="120" height="3" />
        {Array.from({ length: 13 }, (_, i) => (
          <path key={i} d={`M${192 + i * 9.5} 196 L${192 + i * 9.5} 168 L${190.5 + i * 9.5} 168 L${193.5 + i * 9.5} 162 L${196.5 + i * 9.5} 168 L${195 + i * 9.5} 168 L${195 + i * 9.5} 196 Z`} />
        ))}
      </g>
      {/* little jack-o'-lanterns glowing on the ground */}
      {[{ x: 70, s: 1 }, { x: 640, s: 0.8 }, { x: 930, s: 0.9 }].map((p, i) => (
        <motion.g key={i} transform={`translate(${p.x} 196) scale(${p.s})`} animate={{ opacity: [0.8, 1, 0.85] }} transition={{ duration: 1.6 + i * 0.4, repeat: Infinity }}>
          <ellipse cx="0" cy="-8" rx="13" ry="10" fill="#ea580c" />
          <rect x="-1.5" y="-21" width="3" height="5" fill="#3f6212" />
          <path d="M-7 -11 L-3 -11 L-5 -15 Z M3 -11 L7 -11 L5 -15 Z M-7 -5 L7 -5 L4 -2 L0 -4 L-4 -2 Z" fill="#ffd27a" />
        </motion.g>
      ))}
    </svg>
  );
}

function Cobweb({ className }: { className?: string }) {
  const rays = [0, 15, 30, 45, 60, 75, 90];
  return (
    <svg viewBox="0 0 100 100" className={className} fill="none" stroke="rgba(230,220,255,0.22)" strokeWidth="0.8">
      {rays.map((a) => <line key={a} x1="0" y1="0" x2={100 * Math.cos((a * Math.PI) / 180)} y2={100 * Math.sin((a * Math.PI) / 180)} />)}
      {[22, 40, 58, 76].map((r) => (
        <path key={r} d={rays.slice(0, -1).map((a, i) => {
          const b = rays[i + 1];
          const p = (deg: number) => `${(r * Math.cos((deg * Math.PI) / 180)).toFixed(1)} ${(r * Math.sin((deg * Math.PI) / 180)).toFixed(1)}`;
          const mid = (a + b) / 2;
          const q = `${(r * 0.86 * Math.cos((mid * Math.PI) / 180)).toFixed(1)} ${(r * 0.86 * Math.sin((mid * Math.PI) / 180)).toFixed(1)}`;
          return `${i === 0 ? "M" : ""}${p(a)} Q${q} ${p(b)}`;
        }).join(" ")} />
      ))}
    </svg>
  );
}

/** The SBO droplet dressed up for Halloween: witch hat and a little pumpkin. */
export function HalloweenDrop({ size = 72 }: { size?: number }) {
  return (
    <div className="relative" style={{ width: size, height: size }}>
      <img src={logo} alt="SBO" width={size} height={size} className="h-full w-full drop-shadow-[0_0_18px_rgba(249,115,22,0.45)]" />
      <svg viewBox="0 0 100 80" className="absolute" style={{ width: size * 0.62, left: size * 0.22, top: -size * 0.12, transform: "rotate(-10deg)" }}>
        <ellipse cx="50" cy="66" rx="46" ry="9" fill="#1c1029" />
        <path d="M30 64 L52 6 Q56 0 60 8 L72 64 Z" fill="#241436" />
        <path d="M31 58 L71 58 L72 64 L30 64 Z" fill="#f97316" />
        <rect x="46" y="56" width="10" height="9" rx="1.5" fill="#fbbf24" />
      </svg>
      <span className="absolute -bottom-1 -right-2 select-none" style={{ fontSize: size * 0.34 }}>🎃</span>
    </div>
  );
}

export function HalloweenBanner() {
  return (
    <div className="flex items-center gap-4 rounded-2xl border border-primary/40 bg-card/70 p-4 backdrop-blur"
      style={{ boxShadow: "0 0 40px -12px rgba(249,115,22,0.55)" }}>
      <HalloweenDrop size={64} />
      <div>
        <p className="font-display text-lg font-bold tracking-tight">Notte di Halloween 🦇</p>
        <p className="text-xs text-muted-foreground">Solo per oggi SBO si è travestito. A mezzanotte torna tutto normale.</p>
      </div>
    </div>
  );
}
