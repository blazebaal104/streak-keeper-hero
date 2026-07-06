import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { supabase } from "@/integrations/supabase/client";

export type Profile = {
  id: string;
  username: string;
  avatar: string;
  avatar_url: string | null;
  friend_code: string;
  daily_limit: number;
  use_adaptive_limit: boolean;
  notifications_enabled: boolean;
  clan_id: string | null;
  clan_role: "leader" | "co_leader" | "recruit";
  slot_1: string | null;
  slot_2: string | null;
  slot_3: string | null;
};

export type SboEvent = { id: string; user_id: string; ts: string };
export type Badge = { id: string; user_id: string; category: string; tier: string; threshold: number; awarded_at: string };
export type Clan = { id: string; name: string; tag: string; owner_id: string };
export type DirectMessage = { id: string; sender_id: string; recipient_id: string; content: string; created_at: string; read_at: string | null };
export type ClanMessage = { id: string; clan_id: string; sender_id: string | null; content: string; is_system: boolean; created_at: string };
export type ClanInvite = { id: string; clan_id: string; from_user_id: string; to_user_id: string; status: "pending" | "accepted" | "declined"; created_at: string };

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
      return data as unknown as Profile;
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
      return data as unknown as Profile | null;
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

// Public API to read another user's events (RLS restricts to friends)
export function useEventsOf(otherId: string | null) {
  return useQuery({
    queryKey: ["events-of", otherId],
    enabled: !!otherId,
    queryFn: async (): Promise<SboEvent[]> => {
      const { data, error } = await supabase.from("events").select("*").eq("user_id", otherId!).order("ts", { ascending: false }).limit(1000);
      if (error) throw error;
      return (data ?? []) as SboEvent[];
    },
  });
}

export function useBadgesOf(otherId: string | null) {
  return useQuery({
    queryKey: ["badges-of", otherId],
    enabled: !!otherId,
    queryFn: async (): Promise<Badge[]> => {
      const { data, error } = await supabase.from("badges").select("*").eq("user_id", otherId!).order("threshold", { ascending: true });
      if (error) throw error;
      return (data ?? []) as Badge[];
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
      .channel(`badges:${userId}:${Math.random().toString(36).slice(2)}`)
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
      return (profs ?? []).map((p) => ({ friend_id: p.id, profile: p as unknown as Profile }));
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
  const qc = useQueryClient();
  useEffect(() => {
    if (!clanId) return;
    const ch = supabase
      .channel(`clan-members:${clanId}:${Math.random().toString(36).slice(2)}`)
      .on("postgres_changes", { event: "UPDATE", schema: "public", table: "profiles" }, () => {
        qc.invalidateQueries({ queryKey: ["clan-members", clanId] });
      })
      .subscribe();
    return () => { supabase.removeChannel(ch); };
  }, [clanId, qc]);
  return useQuery({
    queryKey: ["clan-members", clanId],
    enabled: !!clanId,
    queryFn: async (): Promise<Profile[]> => {
      const { data, error } = await supabase.from("profiles").select("*").eq("clan_id", clanId!);
      if (error) throw error;
      return (data ?? []) as unknown as Profile[];
    },
  });
}

export function useCreateClan(userId: string | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ name, tag }: { name: string; tag: string }) => {
      const { data, error } = await supabase.from("clans").insert({ name, tag: tag.toUpperCase(), owner_id: userId! }).select().single();
      if (error) throw error;
      // trigger on_clan_created will assign leader role
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
      await supabase.from("profiles").update({ clan_id: data.id, clan_role: "recruit" }).eq("id", userId!);
      return data;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["profile", userId] }),
  });
}

export function useLeaveClan(userId: string | null) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      await supabase.from("profiles").update({ clan_id: null, clan_role: "recruit" }).eq("id", userId!);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["profile", userId] }),
  });
}

export function useSetClanRole() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ target, role }: { target: string; role: "leader" | "co_leader" | "recruit" }) => {
      const { error } = await supabase.rpc("set_clan_role", { _target: target, _role: role });
      if (error) throw error;
    },
    onSuccess: (_d, v) => { qc.invalidateQueries({ queryKey: ["clan-members"] }); qc.invalidateQueries({ queryKey: ["profile", v.target] }); },
  });
}

export function useKickClanMember() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (target: string) => {
      const { error } = await supabase.rpc("kick_clan_member", { _target: target });
      if (error) throw error;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["clan-members"] }),
  });
}

// ---------- Clan Invites ----------
export function useIncomingInvites(userId: string | null) {
  const qc = useQueryClient();
  useEffect(() => {
    if (!userId) return;
    const ch = supabase
      .channel(`invites:${userId}:${Math.random().toString(36).slice(2)}`)
      .on("postgres_changes", { event: "*", schema: "public", table: "clan_invites", filter: `to_user_id=eq.${userId}` }, () => {
        qc.invalidateQueries({ queryKey: ["invites-in", userId] });
      })
      .subscribe();
    return () => { supabase.removeChannel(ch); };
  }, [userId, qc]);
  return useQuery({
    queryKey: ["invites-in", userId],
    enabled: !!userId,
    queryFn: async () => {
      const { data, error } = await supabase.from("clan_invites").select("*").eq("to_user_id", userId!).eq("status", "pending");
      if (error) throw error;
      return (data ?? []) as ClanInvite[];
    },
  });
}

export function useSendClanInvite(userId: string | null) {
  return useMutation({
    mutationFn: async ({ clanId, toUserId, myName, clanName }: { clanId: string; toUserId: string; myName: string; clanName: string }) => {
      const { error } = await supabase.from("clan_invites").insert({ clan_id: clanId, from_user_id: userId!, to_user_id: toUserId });
      if (error && !error.message.includes("duplicate")) throw error;
      // Also send a DM
      await supabase.from("direct_messages").insert({
        sender_id: userId!,
        recipient_id: toUserId,
        content: `👋 ${myName} ti invita nel clan "${clanName}"! Controlla i tuoi inviti.`,
      });
    },
  });
}

export function useAcceptInvite() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (inviteId: string) => {
      const { error } = await supabase.rpc("accept_clan_invite", { _invite_id: inviteId });
      if (error) throw error;
    },
    onSuccess: () => {
      qc.invalidateQueries();
    },
  });
}

export function useDeclineInvite() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (inviteId: string) => {
      const { error } = await supabase.rpc("decline_clan_invite", { _invite_id: inviteId });
      if (error) throw error;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["invites-in"] }),
  });
}

// ---------- Direct Messages ----------
export function useDirectMessages(userId: string | null, otherId: string | null) {
  const qc = useQueryClient();
  const key = ["dm", userId, otherId];
  useEffect(() => {
    if (!userId || !otherId) return;
    const ch = supabase
      .channel(`dm:${userId}:${otherId}:${Math.random().toString(36).slice(2)}`)
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
      .channel(`clan:${clanId}:${Math.random().toString(36).slice(2)}`)
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
/** ICE streak: consecutive days with 0 events, walking back from today. */
export function computeIceStreak(events: SboEvent[]): number {
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

/** FIRE streak: consecutive days with >=1 event including today. */
export function computeFireStreak(events: SboEvent[]): number {
  if (events.length === 0) return 0;
  const byDay = eventsByDay(events);
  const today = startOfDayTs(new Date());
  const todayKey = new Date(today).toISOString().slice(0, 10);
  if (!byDay.get(todayKey)) return 0;
  let streak = 0;
  let cursor = today;
  while (streak < 3650) {
    const key = new Date(cursor).toISOString().slice(0, 10);
    if (!byDay.get(key)) break;
    streak += 1;
    cursor -= DAY_MS;
  }
  return streak;
}

/** Legacy alias — kept for old imports that treated "streak" as ice. */
export const computeStreak = computeIceStreak;

export function adaptiveLimit(events: SboEvent[], fallback = 3): number {
  const cutoff = Date.now() - 14 * DAY_MS;
  const recent = events.filter((e) => new Date(e.ts).getTime() >= cutoff);
  if (recent.length < 5) return fallback;
  return Math.max(1, Math.round(recent.length / 14));
}

/** Aggregated daily counts for the last N days, oldest first, filling gaps with 0. */
export function dailyCountsSeries(events: SboEvent[], days = 30): { date: string; count: number; ma7: number }[] {
  const byDay = eventsByDay(events);
  const today = startOfDayTs(new Date());
  const out: { date: string; count: number; ma7: number }[] = [];
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(today - i * DAY_MS);
    const key = d.toISOString().slice(0, 10);
    out.push({ date: key, count: byDay.get(key) ?? 0, ma7: 0 });
  }
  // 7-day trailing moving average
  for (let i = 0; i < out.length; i++) {
    const from = Math.max(0, i - 6);
    const slice = out.slice(from, i + 1);
    const avg = slice.reduce((a, x) => a + x.count, 0) / slice.length;
    out[i].ma7 = Math.round(avg * 100) / 100;
  }
  return out;
}

/** Mark all unread DMs from `otherId` to me as read. */
export function useMarkDmsRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (otherId: string) => {
      const { error } = await supabase.rpc("mark_dms_read", { _other: otherId });
      if (error) throw error;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ["dm"] }),
  });
}

