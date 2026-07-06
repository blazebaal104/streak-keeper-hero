// Cron hook: send all scheduled_notifications whose deliver_at has passed.
// Also used by DB triggers indirectly (they queue rows; we send).
import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/api/public/hooks/send-push")({
  server: {
    handlers: {
      POST: async () => {
        const url = process.env.SUPABASE_URL!;
        const serviceKey = process.env.SUPABASE_SERVICE_ROLE_KEY!;
        const vapidPub = process.env.VAPID_PUBLIC_KEY!;
        const vapidPriv = process.env.VAPID_PRIVATE_KEY!;
        const vapidSubject = process.env.VAPID_SUBJECT || "mailto:notify@sbo.app";

        const { createClient } = await import("@supabase/supabase-js");
        const webpush = (await import("web-push")).default;
        webpush.setVapidDetails(vapidSubject, vapidPub, vapidPriv);

        const admin = createClient(url, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } });

        const { data: due, error } = await admin
          .from("scheduled_notifications")
          .select("id,user_id,title,body,tag,url")
          .eq("sent", false)
          .lte("deliver_at", new Date().toISOString())
          .limit(200);
        if (error) return Response.json({ error: error.message }, { status: 500 });

        let sent = 0;
        for (const n of due ?? []) {
          const { data: subs } = await admin
            .from("push_subscriptions")
            .select("endpoint,p256dh,auth")
            .eq("user_id", n.user_id);
          for (const s of subs ?? []) {
            try {
              await webpush.sendNotification(
                { endpoint: s.endpoint, keys: { p256dh: s.p256dh, auth: s.auth } },
                JSON.stringify({ title: n.title, body: n.body, tag: n.tag ?? undefined, url: n.url ?? "/" }),
              );
            } catch (e: unknown) {
              const err = e as { statusCode?: number };
              if (err.statusCode === 410 || err.statusCode === 404) {
                await admin.from("push_subscriptions").delete().eq("endpoint", s.endpoint);
              }
            }
          }
          await admin.from("scheduled_notifications").update({ sent: true }).eq("id", n.id);
          sent += 1;
        }
        return Response.json({ ok: true, processed: sent });
      },
    },
  },
});
