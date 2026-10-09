import { useEffect } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { supabase } from "@/integrations/supabase/client";
import type { ZeroAnswers, League } from "./sbo-zero";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const db = supabase as any;

export type ProjectZero = {
  user_id: string; active: boolean; started_at: string | null; league: League; start_limit: number;
  theme: string; answers: ZeroAnswers; last_assessment_at: string | null;
};
export type CustomWorkout = { name: string; note?: string; at: string };
export type WorkoutDay = { user_id: string; day: string; done_ids: string[]; plan_size: number; custom: CustomWorkout[]; completed: boolean };
export type AgentMessage = { id: string; user_id: string; content: string; kind: string; created_at: string; read_at: string | null };

export function useProjectZero(userId: string | null) {
  return useQuery({
    queryKey: ["pz", userId],
    enabled: !!userId,
    queryFn: async (): Promise<ProjectZero | null> => {
      const { data, error } = await db.from("project_zero").select("*").eq("user_id", userId).maybeSingle();
      if (error) throw error;
      return data;
    },
  });
}

export function useSaveProjectZero(userId: string | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (patch: Partial<ProjectZero>) => {
      const { error } = await db.from("project_zero").upsert({ user_id: userId, ...patch });
      if (error) throw error;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pz", userId] }),
  });
}

export function useWorkoutDays(userId: string | null) {
  return useQuery({
    queryKey: ["workouts", userId],
    enabled: !!userId,
    queryFn: async (): Promise<WorkoutDay[]> => {
      const since = new Date(Date.now() - 120 * 86400000);
      const k = `${since.getFullYear()}-${String(since.getMonth() + 1).padStart(2, "0")}-${String(since.getDate()).padStart(2, "0")}`;
      const { data, error } = await db.from("workout_days").select("*").eq("user_id", userId).gte("day", k);
      if (error) throw error;
      return data ?? [];
    },
  });
}

export function useSaveWorkoutDay(userId: string | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (row: Omit<WorkoutDay, "user_id">) => {
      const { error } = await db.from("workout_days").upsert({ user_id: userId, ...row, updated_at: new Date().toISOString() });
      if (error) throw error;
    },
    onMutate: async (row) => {
      await qc.cancelQueries({ queryKey: ["workouts", userId] });
      qc.setQueryData<WorkoutDay[]>(["workouts", userId], (old = []) => [
        ...old.filter((w) => w.day !== row.day),
        { user_id: userId!, ...row },
      ]);
    },
  });
}

export function useAgentMessages(userId: string | null) {
  const qc = useQueryClient();
  useEffect(() => {
    if (!userId) return;
    const ch = supabase
      .channel(`agent:${userId}:${Math.random().toString(36).slice(2)}`)
      .on("postgres_changes", { event: "INSERT", schema: "public", table: "agent_messages", filter: `user_id=eq.${userId}` }, (p) => {
        qc.setQueryData<AgentMessage[]>(["agent", userId], (old = []) => old.some((m) => m.id === (p.new as AgentMessage).id) ? old : [...old, p.new as AgentMessage]);
      })
      .subscribe();
    return () => { supabase.removeChannel(ch); };
  }, [userId, qc]);
  return useQuery({
    queryKey: ["agent", userId],
    enabled: !!userId,
    queryFn: async (): Promise<AgentMessage[]> => {
      const { data, error } = await db.from("agent_messages").select("*").eq("user_id", userId).order("created_at", { ascending: true }).limit(300);
      if (error) throw error;
      return data ?? [];
    },
  });
}

export function useMarkAgentRead(userId: string | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      await db.from("agent_messages").update({ read_at: new Date().toISOString() }).eq("user_id", userId).is("read_at", null);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["agent", userId] }),
  });
}
