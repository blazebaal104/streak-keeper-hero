import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { requireSupabaseAuth } from "@/integrations/supabase/auth-middleware";

export const Input = z.object({ tzOffset: z.number().min(-900).max(900), reason: z.enum(["open", "event", "quit", "workout"]) });


export const agentTick = createServerFn({ method: "POST" })
  .middleware([requireSupabaseAuth])
  .inputValidator((d) => Input.parse(d))
  .handler(async ({ data, context }) => (await import("./zero-agent.core.server")).agentTickCore(context as { supabase: unknown; userId: string }, data));
