// Instant native push: sends every due scheduled_notifications row to the user's phones through
// Firebase Cloud Messaging (HTTP v1). Called by a trigger right after a row is inserted (so a message
// arrives in ~1-2 s, like WhatsApp) and every minute by pg_cron for rows scheduled later.
// Idempotent: rows are claimed atomically (claim_fcm_batch), so concurrent calls never double-send.
import { createClient } from "jsr:@supabase/supabase-js@2";

const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
  auth: { persistSession: false, autoRefreshToken: false },
});

type SA = { project_id: string; client_email: string; private_key: string };
let cached: { token: string; exp: number } | null = null;

function b64url(data: ArrayBuffer | string) {
  const bytes = typeof data === "string" ? new TextEncoder().encode(data) : new Uint8Array(data);
  let s = "";
  bytes.forEach((b) => (s += String.fromCharCode(b)));
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

async function accessToken(sa: SA): Promise<string> {
  if (cached && cached.exp > Date.now() + 60_000) return cached.token;
  const now = Math.floor(Date.now() / 1000);
  const head = b64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claim = b64url(JSON.stringify({
    iss: sa.client_email, scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token", iat: now, exp: now + 3600,
  }));
  const pem = sa.private_key.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "");
  const der = Uint8Array.from(atob(pem), (c) => c.charCodeAt(0));
  const key = await crypto.subtle.importKey("pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(`${head}.${claim}`));
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${head}.${claim}.${b64url(sig)}`,
  });
  const j = await res.json();
  if (!j.access_token) throw new Error("oauth: " + JSON.stringify(j).slice(0, 200));
  cached = { token: j.access_token, exp: Date.now() + (j.expires_in ?? 3600) * 1000 };
  return cached.token;
}

Deno.serve(async () => {
  // the Firebase key lives in the database vault (or an env secret), readable only with the service role
  let raw = Deno.env.get("FCM_SERVICE_ACCOUNT");
  if (!raw) raw = (await admin.rpc("fcm_service_account")).data ?? undefined;
  if (!raw) return Response.json({ ok: false, error: "FCM_SERVICE_ACCOUNT not set" }, { status: 200 });
  const sa = JSON.parse(raw) as SA;
  const { data: rows, error } = await admin.rpc("claim_fcm_batch", { _limit: 100 });
  if (error) return Response.json({ ok: false, error: error.message }, { status: 500 });
  if (!rows?.length) return Response.json({ ok: true, sent: 0 });
  const token = await accessToken(sa);
  const users = [...new Set(rows.map((r: { user_id: string }) => r.user_id))];
  const { data: toks } = await admin.from("fcm_tokens").select("token,user_id").in("user_id", users);
  let sent = 0;
  for (const r of rows as { id: string; user_id: string; title: string; body: string; tag: string | null; url: string | null }[]) {
    for (const t of (toks ?? []).filter((x: { user_id: string }) => x.user_id === r.user_id)) {
      // data-only + high priority: the app's own service shows it, even when the app is closed
      const res = await fetch(`https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        body: JSON.stringify({
          message: {
            token: t.token,
            data: { id: r.id, title: r.title, body: r.body, tag: r.tag ?? "", url: r.url ?? "/" },
            android: { priority: "high", ttl: "86400s" },
          },
        }),
      });
      if (res.ok) sent++;
      else {
        // a phone that uninstalled the app or reset its token: forget it
        const txt = await res.text();
        if (res.status === 404 || /UNREGISTERED/.test(txt)) await admin.from("fcm_tokens").delete().eq("token", t.token);
        else console.warn("fcm", res.status, txt.slice(0, 300));
      }
    }
  }
  return Response.json({ ok: true, rows: rows.length, sent });
});
