import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { requireSupabaseAuth } from "@/integrations/supabase/auth-middleware";

export const Input = z.object({ text: z.string().trim().min(1).max(1000), tzOffset: z.number().min(-900).max(900) });

export const CHAT_PER_DAY = 30;

export const coachChat = createServerFn({ method: "POST" })
  .middleware([requireSupabaseAuth])
  .inputValidator((d) => Input.parse(d))
  .handler(async ({ data, context }) => (await import("./coach-chat.core.server")).coachChatCore(context as { supabase: unknown; userId: string }, data));
