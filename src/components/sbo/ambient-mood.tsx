import { motion, AnimatePresence } from "motion/react";
import { useEffect, useState } from "react";

/**
 * Full-page mood layer.
 * - fire: a strong orange ignition pulse when the fire starts, settling into a
 *   permanent warmer orange halo around the edges of the screen (something is
 *   burning).
 * - ice: a permanent, slowly breathing frost haze so the whole page reads frozen.
 */
export function AmbientMood({ mode }: { mode: "fire" | "ice" | "neutral" }) {
  const [ignite, setIgnite] = useState(false);

  useEffect(() => {
    if (mode !== "fire") return;
    setIgnite(true);
    const t = setTimeout(() => setIgnite(false), 2600);
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

      {/* Fire: permanent burning halo around the screen edges */}
      {mode === "fire" && (
        <>
          <motion.div
            className="absolute inset-0"
            style={{
              boxShadow: "inset 0 0 200px rgba(249,115,22,0.32), inset 0 0 60px rgba(255,170,60,0.16)",
            }}
            animate={{ opacity: [0.75, 1, 0.75] }}
            transition={{ duration: 4.2, repeat: Infinity, ease: "easeInOut" }}
          />
          <motion.div
            className="absolute inset-x-0 bottom-0 h-2/3"
            style={{
              background:
                "radial-gradient(ellipse 75% 100% at 50% 100%, rgba(255,120,20,0.18), transparent 72%)",
            }}
            animate={{ opacity: [0.6, 1, 0.6] }}
            transition={{ duration: 4.5, repeat: Infinity, ease: "easeInOut" }}
          />
        </>
      )}

      {/* Fire: ignition flare — bright orange, then settles as the flame stabilises */}
      <AnimatePresence>
        {ignite && (
          <motion.div
            key="ignite"
            className="absolute inset-0"
            style={{
              background:
                "radial-gradient(ellipse 85% 60% at 50% 58%, rgba(255,150,40,0.62), rgba(190,55,0,0.3) 55%, transparent 80%)",
            }}
            initial={{ opacity: 0 }}
            animate={{ opacity: [0, 0.35, 1, 0.7, 0.85, 0] }}
            exit={{ opacity: 0 }}
            transition={{ duration: 2.6, ease: "easeInOut", times: [0, 0.18, 0.42, 0.6, 0.75, 1] }}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
