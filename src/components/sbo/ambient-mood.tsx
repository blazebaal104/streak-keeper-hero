import { motion, AnimatePresence } from "motion/react";
import { useEffect, useState } from "react";

/**
 * Full-page mood layer.
 * - fire: a one-shot "ignition" flash (the room catches fire for a moment) that
 *   fades away, leaving only the button burning.
 * - ice: a permanent, slowly breathing frost haze so the whole page reads frozen.
 */
export function AmbientMood({ mode }: { mode: "fire" | "ice" | "neutral" }) {
  const [ignite, setIgnite] = useState(false);

  useEffect(() => {
    if (mode !== "fire") return;
    setIgnite(true);
    const t = setTimeout(() => setIgnite(false), 1900);
    return () => clearTimeout(t);
  }, [mode]);

  return (
    <div className="pointer-events-none fixed inset-0 z-30 overflow-hidden">
      {/* Ice: constant frozen haze */}
      {mode === "ice" && (
        <>
          <motion.div
            className="absolute inset-0"
            style={{
              background:
                "radial-gradient(ellipse 90% 60% at 50% 40%, rgba(125,211,252,0.16), transparent 70%)",
            }}
            animate={{ opacity: [0.7, 1, 0.7] }}
            transition={{ duration: 6, repeat: Infinity, ease: "easeInOut" }}
          />
          <div
            className="absolute inset-0"
            style={{
              boxShadow: "inset 0 0 220px rgba(56,189,248,0.28), inset 0 0 60px rgba(224,247,255,0.14)",
            }}
          />
        </>
      )}

      {/* Fire: ignition flash */}
      <AnimatePresence>
        {ignite && (
          <motion.div
            key="ignite"
            className="absolute inset-0"
            style={{
              background:
                "radial-gradient(ellipse 80% 55% at 50% 62%, rgba(255,140,30,0.55), rgba(180,50,0,0.22) 55%, transparent 78%)",
            }}
            initial={{ opacity: 0 }}
            animate={{ opacity: [0, 1, 0.45, 0.8, 0] }}
            exit={{ opacity: 0 }}
            transition={{ duration: 1.9, ease: "easeOut", times: [0, 0.12, 0.4, 0.6, 1] }}
          />
        )}
      </AnimatePresence>

      {/* Fire: faint permanent ember warmth at the bottom */}
      {mode === "fire" && (
        <motion.div
          className="absolute inset-x-0 bottom-0 h-1/2"
          style={{
            background: "radial-gradient(ellipse 70% 100% at 50% 100%, rgba(255,120,20,0.12), transparent 70%)",
          }}
          animate={{ opacity: [0.6, 1, 0.6] }}
          transition={{ duration: 4.5, repeat: Infinity, ease: "easeInOut" }}
        />
      )}
    </div>
  );
}
