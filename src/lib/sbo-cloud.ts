import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { supabase } from "@/integrations/supabase/client";

export type Profile = {
  id: string;
  username: string;
  avatar: string;
  friend_code: string;
  daily_limit: number;
  use_adaptive_limit: boolean;
  notifications_enabled: boolean;
  clan_id: string | null;
  slot_1: string | null;
  slot_2: string | null;
  slot_3: string | null;
};

export type SboEvent = { id: string; user_id: string; ts: string };
export type Badge = { id: string; user_id: string; category: string; tier: string; threshold: number; awarded_at: string };
export type Clan = { id: string; name: string; tag: string; owner_id: string };
export type DirectMessage = { id: string; sender_id: string; recipient_id: string; content: string; created_at: string };
export type ClanMessage = { id: string; clan_id: string; sender_id: string | null; content: string; is_system: boolean; created_at: string };

// ---------- Auth ----------
export function useAuthUser() {
  const [userId, setUserId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    supabase.auth.getUser().then(({ data }) => {
      setUserId(data.user?.id ?? null);
      setLoading(false);
    });
    const { data: sub } = supabase.auth.onAuthStateChange((_e, session) => {
      setUserId(session?.user?.id ?? null);
    });
    return () => sub.subscription.unsubscribe();
  }, []);
  return { userId, loading };
}

// ---------- Profile ----------
export function useMyProfile(userId: string | null) {
  return useQuery({
    queryKey: ["profile", userId],
    enabled: !!userId,
    queryFn: async (): Promise<Profile> => {
      const { data, error } = await supabase.from("profiles").select("*").eq("id", userId!).single();
      if (error) throw error;
      return data as Profile;
    },
  });
}

export function useUpdateProfile(userId: string | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (patch: Partial<Profile>) => {
      const { error } = await supabase.from("profiles").update(patch).eq("id", userId!);
      if (error) throw error;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["profile", userId] }),
  });
}

export function useProfileById(id: string | null | undefined) {
  return useQuery({
    queryKey: ["profile-by-id", id],
    enabled: !!id,
    queryFn: async (): Promise<Profile | null> => {
      if (!id) return null;
      const { data, error } = await supabase.from("profiles").select("*").eq("id", id).maybeSingle();
      if (error) throw error;
      return data as Profile | null;
    },
  });
}

// ---------- Events ----------
export function useEvents(userId: string | null) {
  return useQuery({
    queryKey: ["events", userId],
    enabled: !!userId,
    queryFn: async (): Promise<SboEvent[]> => {
      const { data, error } = await supabase.from("events").select("*").eq("user_id", userId!).order("ts", { ascending: false }).limit(1000);
      if (error) throw error;
      return (data ?? []) as SboEvent[];
    },
  });
}

export function useAddEvent(userId: string | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const { error } = await supabase.from("events").insert({ user_id: userId! });
      if (error) throw error;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["events", userId] }),
  });
}

export function useUndoLastEvent(userId: string | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const { data } = await supabase.from("events").select("id").eq("user_id", userId!).order("ts", { ascending: false }).limit(1);
      const id = data?.[0]?.id;
      if (!id) return;
      await supabase.from("events").delete().eq("id", id);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["events", userId] }),
  });
}

// ---------- Badges ----------
export function useBadges(userId: string | null) {
  const qc = useQueryClient();
  useEffect(() => {
    if (!userId) return;
    const ch = supabase
      .channel(`badges:${userId}`)
      .on("postgres_changes", { event: "INSERT", schema: "public", table: "badges", filter: `user_id=eq.${userId}` }, () => {
        qc.invalidateQueries({ queryKey: ["badges", userId] });
      })
      .subscribe();
    return () => { supabase.removeChannel(ch); };
  }, [userId, qc]);
  return useQuery({
    queryKey: ["badges", userId],
    enabled: !!userId,
    queryFn: async (): Promise<Badge[]> => {
      const { data, error } = await supabase.from("badges").select("*").eq("user_id", userId!).order("threshold", { ascending: true });
      if (error) throw error;
      return (data ?? []) as Badge[];
    },
  });
}

export async function checkStreakBadges(userId: string) {
  await supabase.rpc("check_streak_badges", { _user_id: userId });
}

// ---------- Friends ----------
export type FriendRow = { friend_id: string; profile: Profile };

export function useFriends(userId: string | null) {
  return useQuery({
    queryKey: ["friends", userId],
    enabled: !!userId,
    queryFn: async (): Promise<FriendRow[]> => {
      const { data, error } = await supabase.from("friendships").select("user_a,user_b").or(`user_a.eq.${userId},user_b.eq.${userId}`);
      if (error) throw error;
      const otherIds = (data ?? []).map((r) => (r.user_a === userId ? r.user_b : r.user_a));
      if (otherIds.length === 0) return [];
      const { data: profs, error: e2 } = await supabase.from("profiles").select("*").in("id", otherIds);
      if (e2) throw e2;
      return (profs ?? []).map((p) => ({ friend_id: p.id, profile: p as Profile }));
    },
  });
}

export function useAddFriendByCode(userId: string | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (code: string) => {
      const cleaned = code.trim().toUpperCase();
      const { data: target, error } = await supabase.from("profiles").select("id,username,friend_code").eq("friend_code", cleaned).maybeSingle();
      if (error) throw error;
      if (!target) throw new Error("Codice amico non trovato");
      if (target.id === userId) throw new Error("Non puoi aggiungere te stesso");
      const [a, b] = [userId!, target.id].sort();
      const { error: e2 } = await supabase.from("friendships").insert({ user_a: a, user_b: b });
      if (e2 && !e2.message.includes("duplicate")) throw e2;
      return target.username;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["friends", userId] }),
  });
}

export function useRemoveFriend(userId: string | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (otherId: string) => {
      const [a, b] = [userId!, otherId].sort();
      await supabase.from("friendships").delete().eq("user_a", a).eq("user_b", b);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["friends", userId] }),
  });
}

// ---------- Clans ----------
export function useClan(clanId: string | null | undefined) {
  return useQuery({
    queryKey: ["clan", clanId],
    enabled: !!clanId,
    queryFn: async () => {
      const { data, error } = await supabase.from("clans").select("*").eq("id", clanId!).single();
      if (error) throw error;
      return data as Clan;
    },
  });
}

export function useClanMembers(clanId: string | null | undefined) {
  return useQuery({
    queryKey: ["clan-members", clanId],
    enabled: !!clanId,
    queryFn: async (): Promise<Profile[]> => {
      const { data, error } = await supabase.from("profiles").select("*").eq("clan_id", clanId!);
      if (error) throw error;
      return (data ?? []) as Profile[];
    },
  });
}

export function useCreateClan(userId: string | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ name, tag }: { name: string; tag: string }) => {
      const { data, error } = await supabase.from("clans").insert({ name, tag, owner_id: userId! }).select().single();
      if (error) throw error;
      await supabase.from("profiles").update({ clan_id: data.id }).eq("id", userId!);
      return data as Clan;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["profile", userId] });
    },
  });
}

export function useJoinClanByTag(userId: string | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (tag: string) => {
      const { data, error } = await supabase.from("clans").select("id,name").eq("tag", tag.toUpperCase()).maybeSingle();
      if (error) throw error;
      if (!data) throw new Error("Clan non trovato");
      await supabase.from("profiles").update({ clan_id: data.id }).eq("id", userId!);
      return data;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["profile", userId] }),
  });
}

export function useLeaveClan(userId: string | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      await supabase.from("profiles").update({ clan_id: null }).eq("id", userId!);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["profile", userId] }),
  });
}

// ---------- Direct Messages ----------
export function useDirectMessages(userId: string | null, otherId: string | null) {
  const qc = useQueryClient();
  const key = ["dm", userId, otherId];
  useEffect(() => {
    if (!userId || !otherId) return;
    const ch = supabase
      .channel(`dm:${userId}:${otherId}`)
      .on("postgres_changes", { event: "INSERT", schema: "public", table: "direct_messages" }, (p) => {
        const m = p.new as DirectMessage;
        const involvesUs = (m.sender_id === userId && m.recipient_id === otherId) || (m.sender_id === otherId && m.recipient_id === userId);
        if (involvesUs) qc.invalidateQueries({ queryKey: key });
      })
      .subscribe();
    return () => { supabase.removeChannel(ch); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId, otherId]);
  return useQuery({
    queryKey: key,
    enabled: !!userId && !!otherId,
    queryFn: async (): Promise<DirectMessage[]> => {
      const { data, error } = await supabase
        .from("direct_messages")
        .select("*")
        .or(`and(sender_id.eq.${userId},recipient_id.eq.${otherId}),and(sender_id.eq.${otherId},recipient_id.eq.${userId})`)
        .order("created_at", { ascending: true })
        .limit(200);
      if (error) throw error;
      return (data ?? []) as DirectMessage[];
    },
  });
}

export function useSendDirectMessage(userId: string | null) {
  return useMutation({
    mutationFn: async ({ to, content }: { to: string; content: string }) => {
      const { error } = await supabase.from("direct_messages").insert({ sender_id: userId!, recipient_id: to, content });
      if (error) throw error;
    },
  });
}

// ---------- Clan Messages ----------
export function useClanMessages(clanId: string | null | undefined) {
  const qc = useQueryClient();
  const key = ["clan-msgs", clanId];
  useEffect(() => {
    if (!clanId) return;
    const ch = supabase
      .channel(`clan:${clanId}`)
      .on("postgres_changes", { event: "INSERT", schema: "public", table: "clan_messages", filter: `clan_id=eq.${clanId}` }, () => {
        qc.invalidateQueries({ queryKey: key });
      })
      .subscribe();
    return () => { supabase.removeChannel(ch); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clanId]);
  return useQuery({
    queryKey: key,
    enabled: !!clanId,
    queryFn: async (): Promise<ClanMessage[]> => {
      const { data, error } = await supabase.from("clan_messages").select("*").eq("clan_id", clanId!).order("created_at", { ascending: true }).limit(200);
      if (error) throw error;
      return (data ?? []) as ClanMessage[];
    },
  });
}

export function useSendClanMessage(userId: string | null, clanId: string | null | undefined) {
  return useMutation({
    mutationFn: async (content: string) => {
      const { error } = await supabase.from("clan_messages").insert({ clan_id: clanId!, sender_id: userId!, content, is_system: false });
      if (error) throw error;
    },
  });
}

// ---------- Derivations ----------
const DAY_MS = 86_400_000;
export function startOfDayTs(d: Date | number | string) {
  const x = new Date(d);
  x.setHours(0, 0, 0, 0);
  return x.getTime();
}
export function countToday(events: SboEvent[]) {
  const start = startOfDayTs(new Date());
  return events.filter((e) => new Date(e.ts).getTime() >= start).length;
}
export function countThisMonth(events: SboEvent[]) {
  const now = new Date();
  return events.filter((e) => {
    const d = new Date(e.ts);
    return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
  }).length;
}
export function eventsByDay(events: SboEvent[]): Map<string, number> {
  const m = new Map<string, number>();
  for (const e of events) {
    const k = new Date(startOfDayTs(e.ts)).toISOString().slice(0, 10);
    m.set(k, (m.get(k) ?? 0) + 1);
  }
  return m;
}
export function computeStreak(events: SboEvent[]): number {
  if (events.length === 0) return 0;
  const byDay = eventsByDay(events);
  const today = startOfDayTs(new Date());
  const todayKey = new Date(today).toISOString().slice(0, 10);
  if (byDay.get(todayKey)) return 0;
  let streak = 0;
  let cursor = today;
  while (streak < 3650) {
    const key = new Date(cursor).toISOString().slice(0, 10);
    if (byDay.get(key)) break;
    streak += 1;
    cursor -= DAY_MS;
  }
  return streak;
}
export function adaptiveLimit(events: SboEvent[], fallback = 3): number {
  const cutoff = Date.now() - 14 * DAY_MS;
  const recent = events.filter((e) => new Date(e.ts).getTime() >= cutoff);
  if (recent.length < 5) return fallback;
  return Math.max(1, Math.round(recent.length / 14));
}
