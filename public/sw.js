/* SBO push service worker.
   Non è un app-shell cache — solo push + click-through. */

self.addEventListener("install", (e) => { self.skipWaiting(); });
self.addEventListener("activate", (e) => { e.waitUntil(self.clients.claim()); });

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
