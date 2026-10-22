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
      {/* low graveyard fog */}
      <motion.div className="absolute inset-x-[-20%] bottom-0 h-56"
        style={{ background: "radial-gradient(ellipse 60% 100% at 30% 100%, rgba(190,170,230,0.20), transparent 70%), radial-gradient(ellipse 60% 100% at 75% 100%, rgba(160,200,180,0.14), transparent 70%)" }}
        animate={{ x: ["-4%", "4%", "-4%"] }} transition={{ duration: 18, repeat: Infinity, ease: "easeInOut" }} />
    </div>
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
