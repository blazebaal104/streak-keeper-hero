// Browser notifications + tiny image compressor.
// Real cross-device push requires PWA + VAPID + a server sender; this covers
// foreground/in-app notifications reliably, and OS-level notifications when
// the tab is in the background but the browser is running.

export async function ensureNotificationPermission(): Promise<NotificationPermission> {
  if (typeof window === "undefined" || !("Notification" in window)) return "denied";
  if (Notification.permission === "default") {
    try { return await Notification.requestPermission(); } catch { return "denied"; }
  }
  return Notification.permission;
}

export function notify(title: string, body: string, tag?: string) {
  if (typeof window === "undefined" || !("Notification" in window)) return;
  if (Notification.permission !== "granted") return;
  try {
    new Notification(title, { body, tag, icon: "/favicon.png", badge: "/favicon.png" });
  } catch {}
}

const LS_LAST_STREAK_REMINDER = "sbo:lastStreakReminder";

// Schedule a periodic in-page check that fires once/day if user hasn't logged today
// and has a streak to protect.
export function scheduleStreakReminder(getState: () => { streak: number; countToday: number; username: string }) {
  const run = () => {
    const s = getState();
    if (s.countToday > 0) return;
    if (s.streak <= 0) return;
    const today = new Date().toISOString().slice(0, 10);
    const last = localStorage.getItem(LS_LAST_STREAK_REMINDER);
    if (last === today) return;
    // Only remind after 18:00 local
    if (new Date().getHours() < 18) return;
    localStorage.setItem(LS_LAST_STREAK_REMINDER, today);
    notify("💧 Non perdere la streak!", `${s.username}, sei a ${s.streak} giorni. Un tap oggi la mantiene viva.`, "streak-reminder");
  };
  run();
  const id = setInterval(run, 60 * 60 * 1000); // hourly check
  return () => clearInterval(id);
}

export const CLAN_ROLE_LABEL: Record<"leader" | "co_leader" | "recruit", string> = {
  leader: "Capo",
  co_leader: "Vice",
  recruit: "Recluta",
};

// Compress a user-selected image to a small square data URL for the avatar.
export async function compressImageToDataUrl(file: File, size = 640, quality = 0.82): Promise<string> {
  const img = await new Promise<HTMLImageElement>((res, rej) => {
    const i = new Image();
    i.onload = () => res(i);
    i.onerror = rej;
    i.src = URL.createObjectURL(file);
  });
  const canvas = document.createElement("canvas");
  canvas.width = size; canvas.height = size;
  const ctx = canvas.getContext("2d")!;
  // cover crop
  const s = Math.min(img.width, img.height);
  const sx = (img.width - s) / 2;
  const sy = (img.height - s) / 2;
  ctx.drawImage(img, sx, sy, s, s, 0, 0, size, size);
  URL.revokeObjectURL(img.src);
  return canvas.toDataURL("image/jpeg", quality);
}
