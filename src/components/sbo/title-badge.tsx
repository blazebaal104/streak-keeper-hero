import type { TitleDef, TitleTier } from "@/lib/sbo-titles";

const FLOW: TitleTier[] = ["mythic", "legendary"];
const SHEEN: TitleTier[] = ["super_rare", "epic", "mythic", "legendary"];
const GLOW: TitleTier[] = ["rare", "epic", "mythic", "legendary"];
const SPARKS: TitleTier[] = ["mythic", "legendary"];

/**
 * Equipped title shown under the nickname.
 * Rarity drives the colour ramp and the animation stack.
 */
export function TitleBadge({
  title,
  size = "sm",
  className = "",
}: {
  title: TitleDef;
  size?: "xs" | "sm" | "md";
  className?: string;
}) {
  const flow = FLOW.includes(title.tier);
  const sheen = SHEEN.includes(title.tier);
  const glow = GLOW.includes(title.tier);
  const sparks = SPARKS.includes(title.tier);

  const text = size === "md" ? "text-sm" : size === "sm" ? "text-xs" : "text-[10px]";

  return (
    <span className={`relative inline-flex items-center gap-1 overflow-hidden ${className}`}>
      <span className={text} aria-hidden>
        {title.emoji}
      </span>
      <span
        className={`${text} bg-clip-text font-semibold tracking-wide text-transparent`}
        style={{
          backgroundImage: title.gradient,
          backgroundSize: flow ? "200% 100%" : undefined,
          animation: [
            flow ? "sbo-title-flow 4s linear infinite" : "",
            glow ? "sbo-title-glow 2.8s ease-in-out infinite" : "",
          ]
            .filter(Boolean)
            .join(", "),
        }}
      >
        {title.name}
      </span>

      {sheen && (
        <span
          aria-hidden
          className="pointer-events-none absolute inset-y-0 left-0 w-8"
          style={{
            background: "linear-gradient(90deg, transparent, rgba(255,255,255,0.55), transparent)",
            animation: "sbo-title-sheen 3.6s ease-in-out infinite",
          }}
        />
      )}

      {sparks &&
        [12, 42, 72].map((left, i) => (
          <span
            key={left}
            aria-hidden
            className="pointer-events-none absolute top-0 h-1 w-1 rounded-full bg-white"
            style={{
              left: `${left}%`,
              animation: `sbo-title-spark ${2 + i * 0.4}s ease-in-out ${i * 0.5}s infinite`,
            }}
          />
        ))}
    </span>
  );
}
