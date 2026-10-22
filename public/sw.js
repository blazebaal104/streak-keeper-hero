/* SBO service worker: push notifications + keeps the app's interface on the phone,
   so it opens instantly and keeps working on a bad connection. */

const CACHE = "sbo-shell-v1"; // bump when files in /sbo/ or icons change

self.addEventListener("install", () => { self.skipWaiting(); });
self.addEventListener("activate", (e) => {
  e.waitUntil((async () => {
    for (const k of await caches.keys()) if (k.startsWith("sbo-") && k !== CACHE) await caches.delete(k);
    await self.clients.claim();
  })());
});

const NEVER = [/^\/_serverFn/, /^\/api\//, /^\/\.well-known\//, /\.(mp4|webm)$/]; // live data, server calls, videos (range requests)
const IMMUTABLE = [/^\/assets\//, /^\/sbo\//, /\.(woff2?|png|webp|jpg|svg|ico)$/];

function timeout(ms) { return new Promise((_, rej) => setTimeout(() => rej(new Error("timeout")), ms)); }

self.addEventListener("fetch", (event) => {
  const req = event.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return; // Supabase, Gemini etc. always go to the network
  if (NEVER.some((r) => r.test(url.pathname))) return;

  // Pages: fresh from the network if it answers quickly, otherwise the copy on the phone.
  if (req.mode === "navigate") {
    event.respondWith((async () => {
      const cache = await caches.open(CACHE);
      const key = url.pathname;
      const net = fetch(req).then((res) => { if (res.ok) cache.put(key, res.clone()); return res; });
      try { return await Promise.race([net, timeout(2500)]); }
      catch {
        const hit = (await cache.match(key)) || (await cache.match("/"));
        return hit || net;
      }
    })());
    return;
  }

  // Build files have a unique name per version: once on the phone, never downloaded again.
  if (IMMUTABLE.some((r) => r.test(url.pathname))) {
    event.respondWith((async () => {
      const cache = await caches.open(CACHE);
      const hit = await cache.match(req);
      if (hit) return hit;
      const res = await fetch(req);
      if (res.ok && res.type === "basic") cache.put(req, res.clone());
      return res;
    })());
  }
});

self.addEventListener("push", (event) => {
  let data = {};
  try { data = event.data ? event.data.json() : {}; } catch { data = { title: "SBO", body: event.data ? event.data.text() : "" }; }
  const title = data.title || "SBO";
  const options = {
    body: data.body || "",
    tag: data.tag || undefined,
    icon: "/favicon.png",
    badge: "/favicon.png",
    vibrate: data.vibrate || [120, 60, 120],
    data: { url: data.url || "/" },
    renotify: !!data.tag,
  };
  event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const target = (event.notification.data && event.notification.data.url) || "/";
  event.waitUntil((async () => {
    const all = await self.clients.matchAll({ type: "window", includeUncontrolled: true });
    for (const c of all) {
      if ("focus" in c) { try { await c.focus(); if (c.navigate) await c.navigate(target); return; } catch {} }
    }
    if (self.clients.openWindow) await self.clients.openWindow(target);
  })());
});
