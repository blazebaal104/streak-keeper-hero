import { useEffect, useState } from "react";
import { IS_DEV_SITE } from "./sbo-env";

export type Holiday = "halloween" | null;

/** Which holiday is it at this local time? Halloween = 31 October, 00:00 → 1 November 00:00. */
export function holidayAt(d: Date): Holiday {
  if (d.getMonth() === 9 && d.getDate() === 31) return "halloween";
  return null;
}

const DEV_KEY = "sbo:devHoliday"; // dev site only: "auto" | "halloween" | "none"

export function getDevHolidayOverride(): "auto" | "halloween" | "none" {
  if (!IS_DEV_SITE) return "auto";
  try { return (localStorage.getItem(DEV_KEY) as "auto" | "halloween" | "none") || "halloween"; } catch { return "halloween"; }
}
export function setDevHolidayOverride(v: "auto" | "halloween" | "none") {
  try { localStorage.setItem(DEV_KEY, v); } catch { /* ignore */ }
  window.dispatchEvent(new Event("sbo:holiday"));
}

function current(): Holiday {
  const o = getDevHolidayOverride();
  if (o === "halloween") return "halloween";
  if (o === "none") return null;
  return holidayAt(new Date());
}

/** Current holiday, switching on/off by itself at midnight even if the app stays open. */
export function useHoliday(): Holiday {
  const [h, setH] = useState<Holiday>(null);
  useEffect(() => {
    let timer: ReturnType<typeof setTimeout>;
    const tick = () => {
      setH(current());
      const now = new Date();
      const next = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 0, 0, 1);
      timer = setTimeout(tick, Math.min(next.getTime() - now.getTime(), 60 * 60 * 1000));
    };
    tick();
    const onChange = () => setH(current());
    window.addEventListener("sbo:holiday", onChange);
    document.addEventListener("visibilitychange", onChange);
    return () => { clearTimeout(timer); window.removeEventListener("sbo:holiday", onChange); document.removeEventListener("visibilitychange", onChange); };
  }, []);
  return h;
}
