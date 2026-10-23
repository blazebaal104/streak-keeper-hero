// HTTP endpoints for the native Android app: the same coach logic as the web's server functions
// (coachChat / agentTick), authenticated with the user's Supabase access token (Authorization: Bearer).
import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/api/public/native/$fn")({
  server: {
    handlers: {
      POST: async ({ request, params }) => {
        const token = (request.headers.get("authorization") ?? "").replace(/^Bearer\s+/i, "");
        if (!token) return Response.json({ error: "unauthorized" }, { status: 401 });
        const { createClient } = await import("@supabase/supabase-js");
        const url = process.env.SUPABASE_URL!;
        const key = process.env.SUPABASE_PUBLISHABLE_KEY!;
        const supabase = createClient(url, key, {
          global: { headers: { Authorization: `Bearer ${token}` } },
          auth: { persistSession: false, autoRefreshToken: false },
        });
        const { data: u, error } = await supabase.auth.getUser(token);
        if (error || !u?.user) return Response.json({ error: "unauthorized" }, { status: 401 });
        const body = await request.json().catch(() => ({}));
        try {
          if (params.fn === "coach-chat") {
            const { Input, coachChatCore } = await import("@/lib/coach-chat.core.server");
            return Response.json(await coachChatCore({ supabase, userId: u.user.id }, Input.parse(body)));
          }
          if (params.fn === "agent-tick") {
            const { Input, agentTickCore } = await import("@/lib/zero-agent.core.server");
            return Response.json(await agentTickCore({ supabase, userId: u.user.id }, Input.parse(body)));
          }
          return Response.json({ error: "not found" }, { status: 404 });
        } catch (e) {
          return Response.json({ error: (e as Error).message }, { status: 400 });
        }
      },
    },
  },
});
