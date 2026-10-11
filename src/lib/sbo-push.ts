// Client-side push subscription helpers.
// Works in two environments:
//  - Web (browser / PWA): uses the Service Worker at /sw.js + Web Push (VAPID).
//  - Native (Capacitor Android/iOS): uses @capacitor/push-notifications for
//    the native permission prompt and FCM/APNs token registration.
// The public API (isPushSupported / ensureServiceWorker / subscribeToPush /
// unsubscribeFromPush / schedulePush) stays the same so the rest of the app
// doesn't need to know which environment it's in.

import { supabase } from "@/integrations/supabase/client";
import { Capacitor } from "@capacitor/core";

export const VAPID_PUBLIC_KEY =
  "BNp_XOyAZS6Zpkjmd1CeWwvUa9le67SYB7fM33EF4cM0eNSH1_E5FGOX4pgMdVeIrupVD0UeryspidJlK3GNP68";

// --- platform helpers ---------------------------------------------------

function isNative(): boolean {
  try { return Capacitor.isNativePlatform(); } catch { return false; }
}

function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
  const b64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");
  const raw = atob(b64);
  const out = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; ++i) out[i] = raw.charCodeAt(i);
  return out;
}

function arrayBufferToBase64(buf: ArrayBuffer | null): string {
  if (!buf) return "";
  const bytes = new Uint8Array(buf);
  let bin = "";
  for (let i = 0; i < bytes.byteLength; i++) bin += String.fromCharCode(bytes[i]);
  return btoa(bin);
}

export function isPushSupported(): boolean {
  if (isNative()) return true; // native plugin handles everything
  return typeof window !== "undefined"
    && "serviceWorker" in navigator
    && "PushManager" in window
    && "Notification" in window;
}

// --- native (Capacitor) path -------------------------------------------

let nativeListenersAttached = false;

async function subscribeNative(userId: string): Promise<boolean> {
  try {
    const { PushNotifications } = await import("@capacitor/push-notifications");

    let perm = await PushNotifications.checkPermissions();
    if (perm.receive === "prompt" || perm.receive === "prompt-with-rationale") {
      perm = await PushNotifications.requestPermissions();
    }
    if (perm.receive !== "granted") return false;

    if (!nativeListenersAttached) {
      nativeListenersAttached = true;
      await PushNotifications.addListener("registration", async (token) => {
        try {
          await supabase
            .from("push_subscriptions")
            .upsert(
              {
                user_id: userId,
                endpoint: `capacitor://${Capacitor.getPlatform()}/${token.value}`,
                p256dh: "native",
                auth: "native",
                user_agent: `capacitor-${Capacitor.getPlatform()}`,
              },
              { onConflict: "endpoint" },
            );
        } catch (e) {
          console.warn("[push] native token save failed", e);
        }
      });
      await PushNotifications.addListener("registrationError", (err) => {
        console.warn("[push] native registration error", err);
      });
      // Foreground receive is handled by the OS; we can hook here if needed.
      await PushNotifications.addListener("pushNotificationReceived", () => {});
      await PushNotifications.addListener("pushNotificationActionPerformed", () => {});
    }

    await PushNotifications.register();
    return true;
  } catch (e) {
    console.warn("[push] native subscribe failed", e);
    return false;
  }
}

// --- web (Service Worker + Web Push) path ------------------------------

export async function ensureServiceWorker(): Promise<ServiceWorkerRegistration | null> {
  if (isNative()) return null;
  if (!isPushSupported()) return null;
  try {
    const reg = await navigator.serviceWorker.register("/sw.js", { scope: "/" });
    return reg;
  } catch (e) {
    console.warn("[push] SW register failed", e);
    return null;
  }
}

async function subscribeWeb(userId: string): Promise<boolean> {
  if (typeof window === "undefined" || !("Notification" in window)) return false;
  if (Notification.permission === "default") {
    const p = await Notification.requestPermission();
    if (p !== "granted") return false;
  }
  if (Notification.permission !== "granted") return false;

  const reg = await ensureServiceWorker();
  if (!reg) return false;

  let sub = await reg.pushManager.getSubscription();
  if (!sub) {
    try {
      const key = urlBase64ToUint8Array(VAPID_PUBLIC_KEY);
      sub = await reg.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: key.buffer.slice(key.byteOffset, key.byteOffset + key.byteLength) as ArrayBuffer,
      });
    } catch (e) {
      console.warn("[push] subscribe failed", e);
      return false;
    }
  }

  const json = sub.toJSON() as { endpoint?: string; keys?: { p256dh?: string; auth?: string } };
  const endpoint = json.endpoint || sub.endpoint;
  const p256dh = json.keys?.p256dh || arrayBufferToBase64(sub.getKey("p256dh"));
  const authKey = json.keys?.auth || arrayBufferToBase64(sub.getKey("auth"));
  if (!endpoint || !p256dh || !authKey) return false;

  const { error } = await supabase
    .from("push_subscriptions")
    .upsert({ user_id: userId, endpoint, p256dh, auth: authKey, user_agent: navigator.userAgent }, { onConflict: "endpoint" });
  if (error) { console.warn("[push] save sub failed", error); return false; }
  return true;
}

// --- public API --------------------------------------------------------

export async function subscribeToPush(userId: string): Promise<boolean> {
  if (isNative()) return subscribeNative(userId);
  return subscribeWeb(userId);
}

export async function unsubscribeFromPush(): Promise<void> {
  if (isNative()) {
    try {
      const { PushNotifications } = await import("@capacitor/push-notifications");
      await PushNotifications.removeAllListeners();
      nativeListenersAttached = false;
    } catch {}
    return;
  }
  if (!isPushSupported()) return;
  const reg = await navigator.serviceWorker.getRegistration("/");
  const sub = await reg?.pushManager.getSubscription();
  if (sub) {
    await supabase.from("push_subscriptions").delete().eq("endpoint", sub.endpoint);
    await sub.unsubscribe();
  }
}

// Schedule a server-delivered push at a future time. The server cron picks it up
// and sends it via whichever transport is registered for this user (web push
// and/or native FCM/APNs).
export async function schedulePush(userId: string, args: { deliverAt: Date; title: string; body: string; tag?: string; url?: string }) {
  const { error } = await supabase.from("scheduled_notifications").insert({
    user_id: userId,
    deliver_at: args.deliverAt.toISOString(),
    title: args.title,
    body: args.body,
    tag: args.tag ?? null,
    url: args.url ?? null,
  });
  if (error) console.warn("[push] schedule failed", error);
}
