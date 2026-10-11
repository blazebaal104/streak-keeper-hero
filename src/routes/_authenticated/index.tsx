import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useMemo, useRef, useState } from "react";
import { motion, AnimatePresence } from "motion/react";
import { toast } from "sonner";
import { Toaster } from "@/components/ui/sonner";
import { Calendar } from "@/components/ui/calendar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Slider } from "@/components/ui/slider";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog";
import {
  Plus, Droplets, Flame, Dumbbell, CalendarDays, Users, Undo2, Sparkles, TrendingUp, Settings, Copy, UserPlus, Shield, X, LogOut,
  MessageCircle, Send, Trash2, Camera, Lock, Crown, Star, UserMinus, Check, CheckCheck, Bell, Snowflake, LineChart as LineChartIcon,
} from "lucide-react";
import { LineChart, Line, XAxis, YAxis, Tooltip as RTooltip, ResponsiveContainer, CartesianGrid, Area, AreaChart, ComposedChart, ReferenceLine } from "recharts";
import { useServerFn } from "@tanstack/react-start";
import { Target } from "lucide-react";
import { agentTick } from "@/lib/zero-agent.functions";
import { planForDay, weeklyLimitFor, weekStart as zeroWeekStart, isTrainingDay, LEAGUE_LABEL } from "@/lib/sbo-zero";
import {
  useProjectZero, useSaveProjectZero, useWorkoutDays, useSaveWorkoutDay, useAgentMessages, useMarkAgentRead, type CustomWorkout,
} from "@/lib/sbo-zero-cloud";
import {
  useZeroTheme, ZeroQuestionnaire, ThemePicker, ZeroBanner, ZeroButtons, TodayPlanCard, PlanOverlay, CustomWorkoutDialog, AgentRow, AgentChatDialog,
} from "@/components/sbo/project-zero";
import logo from "@/assets/sbo-logo.png";
import { supabase } from "@/integrations/supabase/client";
import {
  useAuthUser, useMyProfile, useProfileById, useUpdateProfile, useEvents, useAddEvent, useUndoLastEvent, useRemoveEventOnDay,
  useBadges, checkStreakBadges, useFriends, useAddFriendByCode, useRemoveFriend,
  useClan, useClanMembers, useCreateClan, useJoinClanByTag, useLeaveClan,
  useDirectMessages, useSendDirectMessage, useClanMessages, useSendClanMessage,
  useIncomingInvites, useSendClanInvite, useAcceptInvite, useDeclineInvite,
  useSetClanRole, useKickClanMember, useEventsOf, useBadgesOf, useMarkDmsRead, useUnreadDmCounts,
  countToday, countThisMonth, computeFireStreak, computeIceStreak, maxFireStreak, maxIceStreak,
  eventsByDay, adaptiveLimit, dailyCountsSeries, localDayKey,
  type Profile, type Badge, type FriendRow,
} from "@/lib/sbo-cloud";
import { FIRE_BADGES, ICE_BADGES, WORKOUT_BADGES, ALL_BADGES, badgeDef, badgeByThreshold, tierLabel, type BadgeDef, type BadgeCategory } from "@/lib/sbo-badges";
import { FIRE_TITLES, ICE_TITLES, currentTitle, nextTitle, titleTierLabel, titleId, titleById, type TitleDef } from "@/lib/sbo-titles";
import { TitleBadge } from "@/components/sbo/title-badge";
import { encourage, slowdown, restart, overLimitShame, streakAtRisk, fireEncourage, iceEncourage } from "@/lib/sbo-phrases";
import { ensureNotificationPermission, notify, scheduleStreakReminder, CLAN_ROLE_LABEL, compressImageToDataUrl } from "@/lib/sbo-notifications";
import { ensureServiceWorker, subscribeToPush, schedulePush, isPushSupported } from "@/lib/sbo-push";
import { BadgeArt } from "@/components/sbo/badge-art";
import { StreakButton } from "@/components/sbo/streak-button";
import { AmbientMood } from "@/components/sbo/ambient-mood";


const AVATAR_CHOICES = ["💧","🌊","⚡","🌟","🔥","🌙","⚔️","👑","🐉","🦁","🐺","🦅","🌸","🍀","🎯","🧊","☄️","🌈"];

export const Route = createFileRoute("/_authenticated/")({
  component: SboApp,
  head: () => ({
    links: [
      // Preload the fire loop so the button is already ablaze on first paint.
      { rel: "preload", as: "video", href: "/fire-loop.mp4", type: "video/mp4" },
    ],
  }),
});

function SboApp() {
  const nav = useNavigate();
  const { userId, loading: authLoading } = useAuthUser();
  const profileQ = useMyProfile(userId);
  const eventsQ = useEvents(userId);
  const badgesQ = useBadges(userId);
  const friendsQ = useFriends(userId);
  const clanQ = useClan(profileQ.data?.clan_id);
  const clanMembersQ = useClanMembers(profileQ.data?.clan_id);
  const invitesQ = useIncomingInvites(userId);

  const addEvent = useAddEvent(userId);
  const undoLast = useUndoLastEvent(userId);
  const removeOnDay = useRemoveEventOnDay(userId);
  const updateProfile = useUpdateProfile(userId);
  const addFriend = useAddFriendByCode(userId);
  const removeFriend = useRemoveFriend(userId);
  const createClan = useCreateClan(userId);
  const joinClan = useJoinClanByTag(userId);
  const leaveClan = useLeaveClan(userId);

  const [showProfile, setShowProfile] = useState(false);
  const [showInventory, setShowInventory] = useState<{ slot: 0 | 1 | 2 } | false>(false);
  const [showSettings, setShowSettings] = useState(false);
  const [showAddFriend, setShowAddFriend] = useState(false);
  const [showClanModal, setShowClanModal] = useState(false);
  const [showClanInviteFriend, setShowClanInviteFriend] = useState(false);
  const [chatWith, setChatWith] = useState<Profile | null>(null);
  const [showClanChat, setShowClanChat] = useState(false);
  const [viewFriend, setViewFriend] = useState<Profile | null>(null);
  const [showInvites, setShowInvites] = useState(false);
  const [bursts, setBursts] = useState<number[]>([]);
  const [melting, setMelting] = useState(false);
  const [dayEdit, setDayEdit] = useState<Date | null>(null);
  const unreadQ = useUnreadDmCounts(userId);
  const unread = unreadQ.data ?? {};


  const profile = profileQ.data;
  const events = eventsQ.data ?? [];
  const badges = badgesQ.data ?? [];
  const friends = friendsQ.data ?? [];
  const invites = invitesQ.data ?? [];

  // Deep links from push notifications: /?open=dm:<userId> | clan | invites | friend:<userId>
  const [tab, setTab] = useState("home");
  const [pendingOpen, setPendingOpen] = useState<string | null>(() =>
    typeof window === "undefined" ? null : new URLSearchParams(window.location.search).get("open"),
  );
  useEffect(() => {
    if (!pendingOpen) return;
    const [kind, id] = pendingOpen.split(":");
    if (kind === "dm" || kind === "friend") {
      if (!friendsQ.isSuccess) return; // wait for the friend list
      const p = friends.find((f) => f.profile.id === id)?.profile;
      setTab("friends");
      if (p) (kind === "dm" ? setChatWith : setViewFriend)(p);
    } else if (kind === "clan") {
      setTab("friends");
      setShowClanChat(true);
    } else if (kind === "invites") {
      setTab("friends");
      setShowInvites(true);
    }
    setPendingOpen(null);
    window.history.replaceState(null, "", window.location.pathname);
  }, [pendingOpen, friendsQ.isSuccess, friends]);

  const today = countToday(events);
  const month = countThisMonth(events);
  const fireStreak = computeFireStreak(events);
  const iceStreak = computeIceStreak(events);
  const limit = profile ? (profile.use_adaptive_limit ? adaptiveLimit(events, profile.daily_limit) : profile.daily_limit) : 3;
  const byDay = useMemo(() => eventsByDay(events), [events]);
  const series30 = useMemo(() => dailyCountsSeries(events, 30), [events]);
  const series90 = useMemo(() => dailyCountsSeries(events, 90), [events]);
  // Giorni "puliti" totali: giorni senza eventi dal primo evento a oggi.
  const totalCleanDays = useMemo(() => {
    if (events.length === 0) return 0;
    const first = new Date(Math.min(...events.map((e) => new Date(e.ts).getTime())));
    first.setHours(0, 0, 0, 0);
    const today = new Date(); today.setHours(0, 0, 0, 0);
    const days = Math.floor((today.getTime() - first.getTime()) / 86400000) + 1;
    let clean = 0;
    for (let i = 0; i < days; i++) {
      const d = new Date(first.getTime() + i * 86400000);
      if (!(byDay.get(localDayKey(d)) ?? 0)) clean++;
    }
    return clean;
  }, [events, byDay]);
  const prevOverLimit = useRef(false);

  // ================= PROGETTO ZERO =================
  const pzQ = useProjectZero(userId);
  const workoutsQ = useWorkoutDays(userId);
  const agentQ = useAgentMessages(userId);
  const savePz = useSaveProjectZero(userId);
  const saveWD = useSaveWorkoutDay(userId);
  const markAgentRead = useMarkAgentRead(userId);
  const tickFn = useServerFn(agentTick);
  const [showQuiz, setShowQuiz] = useState<false | "start" | "reassess" | "redo">(false);
  const [showPlanOverlay, setShowPlanOverlay] = useState(false);
  const [showCustom, setShowCustom] = useState(false);
  const [showAgent, setShowAgent] = useState(false);
  const pz = pzQ.data ?? null;
  const pzActive = !!pz?.active && !!pz.started_at;
  const agentMsgs = agentQ.data ?? [];
  const workouts = workoutsQ.data ?? [];
  const wdByDay = useMemo(() => new Map(workouts.map((w) => [w.day, w])), [workouts]);
  const pzStart = pz?.started_at ? new Date(pz.started_at) : null;
  const pzDay = pzStart ? Math.floor((new Date().setHours(0, 0, 0, 0) - new Date(pzStart).setHours(0, 0, 0, 0)) / 86400000) + 1 : 0;
  const weeklyLimit = pz ? weeklyLimitFor(pz.start_limit, pzStart) : 3;
  const weekStartKey = localDayKey(zeroWeekStart(new Date()));
  const weekCount = useMemo(() => events.filter((e) => localDayKey(e.ts) >= weekStartKey).length, [events, weekStartKey]);
  const workoutsWeek = useMemo(() => workouts.filter((w) => w.day >= weekStartKey && (w.completed || w.custom.length > 0)).length, [workouts, weekStartKey]);
  const doneKeysBefore = useMemo(() => { const t = localDayKey(new Date()); return new Set(workouts.filter((w) => w.day < t && (w.completed || w.custom.length > 0)).map((w) => w.day)); }, [workouts]);
  const todayPlan = useMemo(() => (pz && userId ? planForDay(new Date(), pz.league, pz.answers, pz.last_assessment_at ? new Date(pz.last_assessment_at) : pzStart, userId, doneKeysBefore) : null),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [pz, userId, doneKeysBefore]);
  useEffect(() => { if (pz && !pz.active && (pz.answers as { _redo?: boolean })?._redo) setShowQuiz("redo"); }, [pz]);
  const todayWD = todayPlan ? wdByDay.get(todayPlan.key) : undefined;
  const reassessDue = !!pz?.last_assessment_at && Date.now() - new Date(pz.last_assessment_at).getTime() > 30 * 86400000;
  useZeroTheme(pzActive ? pz!.theme : null);

  const runTick = async (reason: "open" | "event" | "quit" | "workout") => {
    try { await tickFn({ data: { tzOffset: new Date().getTimezoneOffset(), reason } }); }
    catch (e) { console.warn("coach tick failed", e); }
  };
  // Coach checks in when the app opens
  useEffect(() => {
    if (!pzActive || !userId) return;
    const k = `sbo:pzTick:${localDayKey(new Date())}`;
    if (localStorage.getItem(k)) return;
    localStorage.setItem(k, "1");
    runTick("open");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pzActive, userId]);
  // Big daily plan on first open of the day
  useEffect(() => {
    if (!pzActive || !todayPlan) return;
    const k = `sbo:pzPlanSeen:${todayPlan.key}`;
    if (localStorage.getItem(k)) return;
    localStorage.setItem(k, "1");
    setShowPlanOverlay(true);
  }, [pzActive, todayPlan]);
  // Toast when the coach writes
  const seenAgent = useRef<number | null>(null);
  useEffect(() => {
    if (!agentQ.data) return;
    const n = agentQ.data.length;
    if (seenAgent.current !== null && n > seenAgent.current && !showAgent) {
      const last = agentQ.data[n - 1];
      toast(`🏋️ Coach Zero`, { description: last.content.slice(0, 140), action: { label: "Apri", onClick: () => { setShowAgent(true); markAgentRead.mutate(); } } });
      notify("🏋️ Coach Zero", last.content.slice(0, 140), "coach-zero");
    }
    seenAgent.current = n;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [agentQ.data]);

  const saveToday = (patch: Partial<{ done_ids: string[]; custom: CustomWorkout[] }>) => {
    if (!todayPlan) return;
    const done_ids = patch.done_ids ?? todayWD?.done_ids ?? [];
    const custom = patch.custom ?? todayWD?.custom ?? [];
    const planDone = !todayPlan.rest && todayPlan.exercises.every((e) => done_ids.includes(e.id));
    const completed = planDone || custom.length > 0;
    const was = !!todayWD?.completed;
    saveWD.mutate({ day: todayPlan.key, done_ids, custom, plan_size: todayPlan.exercises.length, completed });
    if (completed && !was) {
      toast.success("💪 Allenamento completato!", { description: "Spunta verde sul calendario. Il coach approva." });
      runTick("workout");
      const id = Date.now();
      setBursts((b) => [...b, id]);
      setTimeout(() => setBursts((b) => b.filter((x) => x !== id)), 2400);
    }
  };
  const toggleExercise = (id: string) => {
    const cur = new Set(todayWD?.done_ids ?? []);
    if (cur.has(id)) cur.delete(id); else cur.add(id);
    saveToday({ done_ids: Array.from(cur) });
  };
  const skipExercise = (id: string) => {
    const cur = todayWD?.done_ids ?? [];
    if (cur.some((x) => x.startsWith("skip:"))) return;
    saveToday({ done_ids: [...cur, `skip:${id}`, id] });
  };
  const addCustomWorkout = (name: string, note: string) => {
    saveToday({ custom: [...(todayWD?.custom ?? []), { name, note, at: new Date().toISOString() }] });
    setShowCustom(false);
  };
  const workoutStatus = (d: Date): "done" | "missed" | null => {
    if (!pz || !pzStart) return null;
    const day = new Date(d); day.setHours(0, 0, 0, 0);
    const start = new Date(pzStart); start.setHours(0, 0, 0, 0);
    const t = new Date(); t.setHours(0, 0, 0, 0);
    if (day < start || day > t) return null;
    const w = wdByDay.get(localDayKey(day));
    if (w && (w.completed || w.custom.length > 0)) return "done";
    if (day.getTime() === t.getTime()) return null;
    return isTrainingDay(pz.league, day, pz.answers) ? "missed" : null;
  };
  const chart30 = useMemo(() => {
    let wk = "", cum = 0;
    return series30.map((d) => {
      const w = wdByDay.get(d.date);
      const ws = localDayKey(zeroWeekStart(new Date(d.date + "T12:00:00")));
      if (ws !== wk) { wk = ws; cum = 0; }
      cum += d.count;
      const lim = pzStart && new Date(d.date + "T12:00:00") >= new Date(new Date(pzStart).setHours(0, 0, 0, 0)) ? weeklyLimitFor(pz!.start_limit, pzStart, new Date(d.date + "T12:00:00")) : Infinity;
      const over = Math.min(d.count, Math.max(0, cum - lim));
      return { ...d, workouts: w ? (w.completed ? 1 : 0) + w.custom.length : 0, over: over || null };
    });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [series30, wdByDay, pz]);
  const chart90 = useMemo(() => {
    const raw = series90.map((d) => { const w = wdByDay.get(d.date); return w ? (w.completed ? 1 : 0) + w.custom.length : 0; });
    const avg = (arr: number[]) => Math.round((arr.reduce((a, b) => a + b, 0) / arr.length) * 100) / 100;
    return series90.map((d, i) => {
      const from = Math.max(0, i - 6);
      // Rolling 7-day average, recomputed every day: rises gradually in heavy weeks, falls gradually in light ones.
      const ma7 = avg(series90.slice(from, i + 1).map((x) => x.count));
      return { ...d, workouts: raw[i], ma7, wma7: avg(raw.slice(from, i + 1)) };
    });
  }, [series90, wdByDay]);

  // Streak-badge check (belt & suspenders: DB trigger also runs it)
  useEffect(() => {
    if (!userId) return;
    checkStreakBadges(userId).catch(() => {});
  }, [userId]);

  // Notifications: permission + streak reminder + PWA push subscribe
  useEffect(() => {
    if (!profile || !userId) return;
    // On native (Capacitor) subscribeToPush handles the OS permission prompt itself;
    // on the web it uses the browser's Notification permission + Service Worker.
    if (isPushSupported()) {
      subscribeToPush(userId).catch(() => {});
    } else {
      ensureNotificationPermission().catch(() => {});
    }
    ensureServiceWorker().catch(() => {});
    const cleanup = scheduleStreakReminder(() => ({ streak: fireStreak, countToday: today, username: profile.username }));
    return cleanup;
  }, [profile, userId, fireStreak, today]);

  // Realtime notifications: new DMs, invites, badges
  const chatWithRef = useRef<{ id: string } | null>(null);
  useEffect(() => { chatWithRef.current = chatWith ?? null; }, [chatWith]);
  useEffect(() => {
    if (!userId) return;
    const ch = supabase
      .channel(`notify:${userId}:${Math.random().toString(36).slice(2)}`)
      .on("postgres_changes", { event: "INSERT", schema: "public", table: "direct_messages", filter: `recipient_id=eq.${userId}` }, (p) => {
        const m = p.new as { sender_id: string; content: string };
        if (chatWithRef.current?.id === m.sender_id) return; // already looking at chat
        supabase.from("profiles").select("username").eq("id", m.sender_id).maybeSingle().then(({ data }) => {
          const from = data?.username ?? "Un amico";
          toast(`💬 ${from}`, { description: m.content });
          notify(`💬 ${from}`, m.content, `dm-${m.sender_id}`);
        });
      })
      .on("postgres_changes", { event: "INSERT", schema: "public", table: "clan_invites", filter: `to_user_id=eq.${userId}` }, () => {
        toast("🛡️ Nuovo invito al clan!", { description: "Apri gli inviti per rispondere." });
        notify("🛡️ Invito al clan", "Qualcuno ti ha invitato in un clan.", "clan-invite");
      })
      .on("postgres_changes", { event: "INSERT", schema: "public", table: "friendships" }, (p) => {
        const m = p.new as { user_a: string; user_b: string };
        if (m.user_a === userId || m.user_b === userId) {
          toast("👥 Nuovo amico aggiunto!");
        }
      })
      .subscribe();
    return () => { supabase.removeChannel(ch); };
  }, [userId]);

  const handleAdd = async () => {
    const wasOver = today >= limit;
    const brokeIce = iceStreak > 0;
    await addEvent.mutateAsync(undefined);
    if (pzActive) {
      if (weekCount + 1 >= weeklyLimit) runTick("event");
      const id = Date.now();
      setBursts((b) => [...b, id]);
      setTimeout(() => setBursts((b) => b.filter((x) => x !== id)), 2400);
      const n = weekCount + 1;
      if (n > weeklyLimit) toast.error(`🚨 ${n}/${weeklyLimit} — OLTRE IL LIMITE`, { description: "Il coach ti sta scrivendo. Non sarà gentile." });
      else if (n === weeklyLimit) toast.warning(`${n}/${weeklyLimit} — limite raggiunto`, { description: "Da qui a lunedì: zero." });
      else toast(`${n}/${weeklyLimit} questa settimana`, { description: "Più stai lontano dal limite, meglio è." });
      return;
    }
    const id = Date.now();
    setBursts((b) => [...b, id]);
    setTimeout(() => setBursts((b) => b.filter((x) => x !== id)), 2400);
    if (brokeIce) {
      setMelting(true);
      setTimeout(() => setMelting(false), 1400);
    }

    const newToday = today + 1;
    const name = profile?.username ?? "";
    if (!profile?.notifications_enabled) return;
    if (newToday >= limit) {
      toast("🌊 Fermati un attimo", { description: slowdown(name) });
      // Schedule delayed shame push (~10 min later) only when crossing for the first time today
      if (!wasOver && !prevOverLimit.current && userId) {
        prevOverLimit.current = true;
        const deliverAt = new Date(Date.now() + 10 * 60 * 1000);
        schedulePush(userId, {
          deliverAt,
          title: "💧 SBO",
          body: overLimitShame(name),
          tag: "over-limit-shame",
          url: "/",
        }).catch(() => {});
      }
    }
    else if (brokeIce) toast("❄️ Streak di ghiaccio rotta", { description: restart(name) });
    else if (fireStreak > 0) toast(fireEncourage(name, fireStreak + (today === 0 ? 1 : 0)));
    else toast(encourage(name));
  };


  const handleSignOut = async () => {
    await supabase.auth.signOut();
    nav({ to: "/auth" });
  };

  if (authLoading || profileQ.isLoading || !profile) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <img src={logo} alt="SBO" width={64} height={64} className="drop-shadow-glow animate-pulse" />
      </div>
    );
  }

  const equipped: (BadgeDef | null)[] = [profile.slot_1, profile.slot_2, profile.slot_3].map((s) => {
    if (!s) return null;
    const b = badges.find((x) => x.id === s);
    if (!b) return null;
    return badgeDef(b.category as BadgeCategory, b.threshold) ?? null;
  });

  return (
    <div className="min-h-screen text-foreground">
      <Toaster position="top-center" theme="dark" />

      {/* Page-wide mood: ignition flash on fire, permanent frost haze on ice */}
      <AmbientMood mode={pzActive ? "neutral" : fireStreak > 0 ? "fire" : iceStreak > 0 ? "ice" : "neutral"} />

      {/* Falling drops overlay */}
      <div className="pointer-events-none fixed inset-0 z-40 overflow-hidden">
        <AnimatePresence>
          {bursts.map((id) => <FallingDrops key={id} seed={id} />)}
        </AnimatePresence>
      </div>

      {/* Header */}
      <header className="mx-auto flex max-w-3xl items-center justify-between px-6 pt-8 pb-4">
        <button className="flex items-center gap-3 text-left" onClick={() => setShowProfile(true)} aria-label="Apri profilo">
          <AvatarView profile={profile} size={44} className="border-2 border-primary/40" />
          <div>
            <h1 className="text-lg font-bold tracking-tight leading-tight">{profile.username}</h1>
            {titleById(profile.equipped_title) ? (
              <TitleBadge title={titleById(profile.equipped_title)!} size="xs" />
            ) : (
              <p className="text-[11px] text-muted-foreground font-mono">{profile.friend_code}</p>
            )}
          </div>
        </button>
        <div className="flex items-center gap-2">
          {pzActive ? (<>
          <div className="flex items-center gap-1.5 rounded-full border border-border bg-card/60 px-2.5 py-2 backdrop-blur" title="Questa settimana / limite">
            <Droplets className="h-4 w-4" style={{ color: `var(--zero-${weekCount > weeklyLimit ? "over" : weekCount === weeklyLimit ? "limit" : "ok"})` }} />
            <span className="text-sm font-semibold">{weekCount}/{weeklyLimit}</span>
          </div>
          <div className="flex items-center gap-1.5 rounded-full border border-border bg-card/60 px-2.5 py-2 backdrop-blur" title="Allenamenti questa settimana">
            <Dumbbell className="h-4 w-4 text-primary" />
            <span className="text-sm font-semibold">{workoutsWeek}</span>
          </div>
          </>) : (<>
          <div className="flex items-center gap-1.5 rounded-full border border-border bg-card/60 px-2.5 py-2 backdrop-blur" title="Streak di fuoco (giorni consecutivi con almeno un evento)">
            <Flame className="h-4 w-4 text-orange-400" />
            <span className="text-sm font-semibold">{fireStreak}</span>
          </div>
          <div className="flex items-center gap-1.5 rounded-full border border-border bg-card/60 px-2.5 py-2 backdrop-blur" title="Streak di ghiaccio (giorni consecutivi senza eventi)">
            <Snowflake className="h-4 w-4 text-sky-300" />
            <span className="text-sm font-semibold">{iceStreak}</span>
          </div>
          </>)}
          <Button variant="ghost" size="icon" className="relative rounded-full border border-border bg-card/60 backdrop-blur" onClick={() => setShowInvites(true)} aria-label="Notifiche">
            <Bell className="h-4 w-4" />
            {invites.length > 0 && <span className="absolute -top-0.5 -right-0.5 h-4 min-w-4 rounded-full bg-destructive text-[10px] font-bold text-white grid place-items-center px-1">{invites.length}</span>}
          </Button>
          <Button variant="ghost" size="icon" className="rounded-full border border-border bg-card/60 backdrop-blur" onClick={() => setShowSettings(true)} aria-label="Impostazioni">
            <Settings className="h-4 w-4" />
          </Button>
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-6 pb-24">
        <Tabs value={tab} onValueChange={setTab} className="w-full">
          <TabsList className="mx-auto grid w-full max-w-md grid-cols-4 rounded-full bg-card/60 backdrop-blur">
            <TabsTrigger value="home" className="rounded-full text-xs"><Droplets className="mr-1 h-3.5 w-3.5" /> Home</TabsTrigger>
            <TabsTrigger value="calendar" className="rounded-full text-xs"><CalendarDays className="mr-1 h-3.5 w-3.5" /> Cal</TabsTrigger>
            <TabsTrigger value="chart" className="rounded-full text-xs"><LineChartIcon className="mr-1 h-3.5 w-3.5" /> Trend</TabsTrigger>
            <TabsTrigger value="friends" className="rounded-full text-xs"><Users className="mr-1 h-3.5 w-3.5" /> Social</TabsTrigger>
          </TabsList>

          {/* HOME */}
          <TabsContent value="home" className="mt-8 space-y-8">
            {pzActive && pz && todayPlan ? (
              <>
                <ZeroBanner day={pzDay} league={pz.league} weekCount={weekCount} limit={weeklyLimit} workoutsWeek={workoutsWeek} />
                {reassessDue && (
                  <button onClick={() => setShowQuiz("reassess")} className="w-full rounded-2xl border border-dashed border-primary/60 bg-primary/10 p-3 text-left text-sm">
                    <b className="text-primary">È passato un mese.</b> Rifai il questionario e scopri se sali di lega →
                  </button>
                )}
                <ZeroButtons weekCount={weekCount} limit={weeklyLimit} workoutDone={!!todayWD?.completed} rest={todayPlan.rest}
                  onSbo={handleAdd}
                  onWorkout={() => (todayPlan.rest || todayWD?.completed ? setShowCustom(true) : setShowPlanOverlay(true))} />
                {events.length > 0 && (
                  <div className="-mt-4 flex justify-center">
                    <Button variant="ghost" size="sm" className="text-muted-foreground" onClick={() => undoLast.mutate()}>
                      <Undo2 className="mr-1.5 h-3.5 w-3.5" /> Annulla ultimo evento
                    </Button>
                  </div>
                )}
                <TodayPlanCard plan={todayPlan} wd={todayWD} onToggle={toggleExercise} onSkip={skipExercise} onCustom={() => setShowCustom(true)} onExpand={() => setShowPlanOverlay(true)} />
                <div className="flex items-center justify-center gap-4">
                  {equipped.map((b, i) => (
                    <BadgeSlot key={i} def={b} onClick={() => setShowInventory({ slot: i as 0 | 1 | 2 })} />
                  ))}
                </div>
              </>
            ) : (
              <>
            <div className="grid grid-cols-3 gap-3">
              <Stat label="Oggi" value={today} accent={today >= limit} />
              <Stat label="Questo mese" value={month} />
              <Stat label={profile.use_adaptive_limit ? "Limite adattivo" : "Limite"} value={limit} icon={profile.use_adaptive_limit ? <TrendingUp className="h-3.5 w-3.5" /> : undefined} />
            </div>

            {/* Equipped badge strip: always 3 slot buttons that open inventory */}
            <div className="flex items-center justify-center gap-4">
              {equipped.map((b, i) => (
                <BadgeSlot key={i} def={b} onClick={() => setShowInventory({ slot: i as 0 | 1 | 2 })} />
              ))}
            </div>

            <div className="relative flex flex-col items-center justify-center py-6">
              <StreakButton fireStreak={fireStreak} iceStreak={iceStreak} onPress={handleAdd} melting={melting} />
              <p className="mt-4 text-center text-sm text-muted-foreground">
                {fireStreak > 0
                  ? `🔥 La tua fiamma cresce: ${fireStreak} giorni di fila.`
                  : iceStreak > 0
                    ? `❄️ Sei congelato da ${iceStreak} giorni. Premere rompe il ghiaccio.`
                    : "Premi per aggiungere. Tutto è salvato sul tuo account."}
              </p>
              {events.length > 0 && (
                <Button variant="ghost" size="sm" className="mt-2 text-muted-foreground" onClick={() => undoLast.mutate()}>
                  <Undo2 className="mr-1.5 h-3.5 w-3.5" /> Annulla ultimo
                </Button>
              )}
            </div>


            <Card className="border-border/50 bg-card/60 p-5 backdrop-blur">
              <div className="flex items-start gap-3">
                <Sparkles className="mt-1 h-5 w-5 shrink-0 text-accent" />
                <div>
                  <p className="text-sm font-semibold">Suggerimento del giorno</p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {today >= limit ? slowdown(profile.username) : fireStreak >= 3 ? fireEncourage(profile.username, fireStreak) : iceStreak >= 3 ? iceEncourage(profile.username, iceStreak) : encourage(profile.username)}
                  </p>

                </div>
              </div>
            </Card>
              </>
            )}
          </TabsContent>

          {/* CALENDAR */}
          <TabsContent value="calendar" className="mt-8">
            <Card className="border-border/50 bg-card/60 p-4 backdrop-blur">
              <p className="mb-3 px-2 text-sm text-muted-foreground">Registro personale — solo per uso medico e di monitoraggio.</p>
              <Calendar mode="single" className="pointer-events-auto mx-auto"
                selected={dayEdit ?? undefined}
                onSelect={(d) => { if (d) setDayEdit(d); }}
                disabled={(d) => {
                  const t = new Date(); t.setHours(0, 0, 0, 0);
                  const day = new Date(d); day.setHours(0, 0, 0, 0);
                  return day.getTime() > t.getTime() || day.getTime() < t.getTime() - 7 * 86400000;
                }}
                modifiers={{
                  logged: (d) => (byDay.get(localDayKey(d)) ?? 0) > 0,
                  heavy: (d) => (byDay.get(localDayKey(d)) ?? 0) >= limit,
                  wDone: (d) => pzActive && workoutStatus(d) === "done",
                  wMissed: (d) => pzActive && workoutStatus(d) === "missed",
                }}
                modifiersClassNames={{
                  logged: "relative after:absolute after:bottom-1 after:left-1/2 after:-translate-x-1/2 after:h-1 after:w-1 after:rounded-full after:bg-accent",
                  heavy: "relative after:absolute after:bottom-1 after:left-1/2 after:-translate-x-1/2 after:h-1.5 after:w-1.5 after:rounded-full after:bg-destructive",
                  wDone: "relative before:absolute before:top-0.5 before:right-0.5 before:content-['✓'] before:text-[9px] before:font-bold before:text-[var(--zero-ok)]",
                  wMissed: "relative before:absolute before:top-0.5 before:right-0.5 before:content-['✕'] before:text-[9px] before:font-bold before:text-[var(--zero-over)]",
                }} />
              <div className="mt-4 flex flex-wrap justify-center gap-x-4 gap-y-1 text-xs text-muted-foreground">
                <span className="flex items-center gap-1.5"><span className="h-1.5 w-1.5 rounded-full bg-accent" /> attività</span>
                <span className="flex items-center gap-1.5"><span className="h-2 w-2 rounded-full bg-destructive" /> sopra il limite</span>
                {pzActive && <span className="flex items-center gap-1.5"><span className="font-bold text-[var(--zero-ok)]">✓</span> allenamento fatto</span>}
                {pzActive && <span className="flex items-center gap-1.5"><span className="font-bold text-[var(--zero-over)]">✕</span> allenamento saltato</span>}
              </div>
              <p className="mt-3 text-center text-[11px] text-muted-foreground">
                Tocca un giorno degli ultimi 7 per aggiungere o togliere eventi dimenticati.
              </p>
            </Card>
          </TabsContent>

          {/* ANALYTICS */}
          <TabsContent value="chart" className="mt-8 space-y-4">
            <Card className="border-border/50 bg-card/60 p-4 backdrop-blur">
              <div className="mb-3 flex items-center justify-between px-1">
                <p className="text-sm font-semibold">Andamento — ultimi 30 giorni</p>
                
              </div>
              {chart30.every((d) => d.count === 0 && d.workouts === 0) ? (
                <p className="py-10 text-center text-xs text-muted-foreground">Nessun dato ancora. Il grafico si popolerà con l'uso.</p>
              ) : (
                <div className="h-56 w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <ComposedChart data={chart30} margin={{ top: 6, right: 8, left: -22, bottom: 0 }}>
                      <defs>
                        <linearGradient id="gCount" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="0%" stopColor="#f97316" stopOpacity={0.55} />
                          <stop offset="100%" stopColor="#f97316" stopOpacity={0} />
                        </linearGradient>
                        <linearGradient id="gOver" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="0%" stopColor="#ffd000" stopOpacity={0.95} />
                          <stop offset="45%" stopColor="#ff3d00" stopOpacity={0.8} />
                          <stop offset="100%" stopColor="#7f0000" stopOpacity={0.2} />
                        </linearGradient>
                        <linearGradient id="gWork" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="0%" stopColor="#ef4444" stopOpacity={0.45} />
                          <stop offset="100%" stopColor="#ef4444" stopOpacity={0} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" stroke="#ffffff" opacity={0.15} />
                      <XAxis dataKey="date" tickFormatter={(s: string) => s.slice(5)} fontSize={10} stroke="#ffffff" tick={{ fill: "#ffffff" }} />
                      <YAxis allowDecimals={false} fontSize={10} stroke="#ffffff" tick={{ fill: "#ffffff" }} width={28} />
                      <RTooltip contentStyle={{ background: "#0f172a", border: "1px solid #334155", borderRadius: 8, fontSize: 12, color: "#fff" }} labelStyle={{ color: "#fff" }} />
                      {pzActive && <Area type="monotone" dataKey="workouts" stroke="#ef4444" fill="url(#gWork)" strokeWidth={2} name="Allenamenti" />}
                      <Area type="monotone" dataKey="count" stroke="#fb923c" fill="url(#gCount)" strokeWidth={2} name="Al giorno" />
                      {pzActive && <Area type="monotone" dataKey="over" stroke="#ff2a00" fill="url(#gOver)" strokeWidth={2.5} name="Oltre il limite" />}
                    </ComposedChart>
                  </ResponsiveContainer>
                </div>
              )}
              {pzActive && (
                <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 px-1 text-[10px] text-muted-foreground">
                  <span className="flex items-center gap-1"><span className="h-2 w-2 rounded-full bg-[#fb923c]" /> eventi</span>
                  <span className="flex items-center gap-1"><span className="h-2 w-2 rounded-full bg-[#ef4444]" /> allenamenti</span>
                  <span className="flex items-center gap-1"><span className="h-2 w-2 rounded-full bg-gradient-to-b from-[#ffd000] to-[#ff2a00]" /> oltre il limite</span>
                </div>
              )}
            </Card>
            <Card className="border-border/50 bg-card/60 p-4 backdrop-blur">
              <p className="mb-3 text-sm font-semibold px-1">Trend a 90 giorni</p>
              {chart90.every((d) => d.count === 0 && d.workouts === 0) ? (
                <p className="py-8 text-center text-xs text-muted-foreground">Nessun dato ancora.</p>
              ) : (
                <div className="h-48 w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={chart90} margin={{ top: 6, right: 8, left: -22, bottom: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#ffffff" opacity={0.15} />
                      <XAxis dataKey="date" tickFormatter={(s: string) => s.slice(5)} fontSize={10} stroke="#ffffff" tick={{ fill: "#ffffff" }} />
                      <YAxis fontSize={10} stroke="#ffffff" tick={{ fill: "#ffffff" }} width={28} />
                      <RTooltip contentStyle={{ background: "#0f172a", border: "1px solid #334155", borderRadius: 8, fontSize: 12, color: "#fff" }} labelStyle={{ color: "#fff" }} />
                      <Line type="monotone" dataKey="ma7" stroke="#38bdf8" strokeWidth={2.5} dot={false} name="Media 7gg" />
                      {pzActive && <Line type="monotone" dataKey="workouts" stroke="#ef4444" strokeWidth={2} dot={false} name="Allenamenti" />}
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              )}
              <p className="mt-2 px-1 text-[11px] text-muted-foreground">{pzActive ? "Obiettivo: la linea rossa (allenamenti) sempre sopra quella blu." : "La linea blu è la media degli ultimi 7 giorni: mostra se stai salendo o scendendo. Obiettivo: portarla giù nel tempo."}</p>
            </Card>
          </TabsContent>

          {/* SOCIAL */}
          <TabsContent value="friends" className="mt-8 space-y-6">
            {(pzActive || agentMsgs.length > 0) && (
              <AgentRow messages={agentMsgs} onOpen={() => { setShowAgent(true); markAgentRead.mutate(); }} />
            )}
            <Card className="border-border/50 bg-card/60 p-5 backdrop-blur">
              <p className="text-xs text-muted-foreground uppercase tracking-wider">Il tuo codice amico</p>
              <div className="mt-2 flex items-center gap-2">
                <p className="flex-1 font-mono text-2xl font-bold tracking-widest">{profile.friend_code}</p>
                <Button size="sm" variant="secondary" onClick={async () => {
                  await navigator.clipboard.writeText(profile.friend_code);
                  toast("📋 Codice copiato!");
                }}><Copy className="mr-1.5 h-3.5 w-3.5" /> Copia</Button>
              </div>
            </Card>

            {/* Clan */}
            <div>
              <div className="mb-2 flex items-center justify-between px-2">
                <p className="text-xs uppercase tracking-wider text-muted-foreground">Clan</p>
                {profile.clan_id && clanQ.data && (
                  <button className="text-xs text-muted-foreground hover:text-destructive" onClick={async () => { if (confirm("Uscire dal clan?")) { await leaveClan.mutateAsync(); toast("Hai lasciato il clan"); } }}>
                    <X className="mr-1 inline h-3 w-3" /> esci
                  </button>
                )}
              </div>
              {profile.clan_id && clanQ.data ? (
                <ClanCard
                  clan={clanQ.data}
                  members={clanMembersQ.data ?? []}
                  meId={userId!}
                  myRole={profile.clan_role}
                  onOpenChat={() => setShowClanChat(true)}
                  onInvite={() => setShowClanInviteFriend(true)}
                />
              ) : (
                <Card className="border-dashed border-border bg-transparent p-5 text-center">
                  <Shield className="mx-auto mb-2 h-8 w-8 text-muted-foreground" />
                  <p className="text-sm text-muted-foreground">Crea o unisciti a un clan per chattare e competere.</p>
                  <Button className="mt-3" size="sm" onClick={() => setShowClanModal(true)}>Gestisci clan</Button>
                </Card>
              )}
            </div>

            {/* Friends list */}
            <div>
              <div className="mb-2 flex items-center justify-between px-2">
                <p className="text-xs uppercase tracking-wider text-muted-foreground">Amici</p>
                <Button size="sm" variant="secondary" onClick={() => setShowAddFriend(true)}>
                  <UserPlus className="mr-1.5 h-3.5 w-3.5" /> Aggiungi
                </Button>
              </div>
              {friends.length === 0 ? (
                <Card className="border-dashed border-border bg-transparent p-5 text-center">
                  <Users className="mx-auto mb-2 h-8 w-8 text-muted-foreground" />
                  <p className="text-sm text-muted-foreground">Non hai ancora amici. Aggiungine uno con il suo codice.</p>
                </Card>
              ) : (
                <div className="space-y-2">
                  {friends.map((f) => (
                    <FriendRowCard key={f.friend_id} row={f} unread={unread[f.friend_id] ?? 0}
                      onOpenProfile={() => setViewFriend(f.profile)}
                      onChat={() => setChatWith(f.profile)}
                      onRemove={() => { if (confirm(`Rimuovere ${f.profile.username}?`)) removeFriend.mutate(f.friend_id); }} />
                  ))}

                </div>
              )}
            </div>
          </TabsContent>
        </Tabs>
      </main>

      {/* PROFILE DIALOG */}
      <ProfileDialog
        open={showProfile}
        onClose={() => setShowProfile(false)}
        profile={profile}
        badges={badges}
        onUpdate={(patch) => updateProfile.mutate(patch)}
        onSignOut={handleSignOut}
        onOpenInventory={(slot) => { setShowProfile(false); setShowInventory({ slot }); }}
      />

      {/* INVENTORY */}
      {showInventory !== false && (
        <InventoryDialog
          onEquipTitle={(id) => updateProfile.mutate({ equipped_title: id })}
          slot={showInventory.slot}
          profile={profile}
          badges={badges}
          totalEvents={events.length}
          totalCleanDays={totalCleanDays}
          onClose={() => setShowInventory(false)}
          onEquip={(badgeId) => {
            const patch: Partial<Profile> = {};
            if (showInventory.slot === 0) patch.slot_1 = badgeId;
            if (showInventory.slot === 1) patch.slot_2 = badgeId;
            if (showInventory.slot === 2) patch.slot_3 = badgeId;
            updateProfile.mutate(patch);
            setShowInventory(false);
          }}
        />
      )}

      {/* SETTINGS */}
      <Dialog open={showSettings} onOpenChange={setShowSettings}>
        <DialogContent className="border-border bg-card">
          <DialogHeader>
            <DialogTitle>Impostazioni</DialogTitle>
            <DialogDescription>Personalizza SBO come vuoi tu.</DialogDescription>
          </DialogHeader>
          <div className="max-h-[65vh] space-y-5 overflow-y-auto py-2 pr-1">
            {/* PROGETTO ZERO */}
            <div className="relative overflow-hidden rounded-2xl border border-primary/40 bg-primary/10 p-4">
              <div className="pointer-events-none absolute -right-8 -top-8 h-24 w-24 rounded-full bg-primary/30 blur-2xl" />
              <div className="flex items-center justify-between gap-3">
                <div>
                  <Label className="flex items-center gap-1.5 text-base"><Target className="h-4 w-4 text-primary" /> Progetto Zero</Label>
                  <p className="text-xs text-muted-foreground">{pzActive ? `Attivo da ${pzDay} giorni · ${LEAGUE_LABEL[pz!.league]}` : "Trasforma SBO nel tuo personal trainer. Minimo 7 giorni."}</p>
                </div>
                <Switch checked={pzActive} onCheckedChange={async (v) => {
                  if (v) { setShowQuiz("start"); return; }
                  if (pzDay <= 7) { toast.error(`Il Progetto Zero dura almeno 7 giorni. Potrai disattivarlo tra ${8 - pzDay} giorni.`); return; }
                  if (!confirm("Vuoi davvero mollare il Progetto Zero? Il coach non la prenderà bene.")) return;
                  try { await runTick("quit"); await savePz.mutateAsync({ active: false }); toast("Progetto Zero disattivato"); }
                  catch (e) { toast.error((e as Error).message); }
                }} />
              </div>
              {pzActive && (
                <div className="mt-4 space-y-3">
                  <p className="text-xs uppercase tracking-wider text-muted-foreground">Tema</p>
                  <ThemePicker value={pz!.theme} onChange={(t) => savePz.mutate({ theme: t })} />
                  <Button variant="secondary" size="sm" className="w-full" onClick={() => setShowQuiz("reassess")}>Rifai il questionario</Button>
                </div>
              )}
            </div>
            <div className="flex items-center justify-between">
              <div>
                <Label>Limite adattivo</Label>
                <p className="text-xs text-muted-foreground">Basato sulle tue ultime 2 settimane</p>
              </div>
              <Switch checked={profile.use_adaptive_limit} onCheckedChange={(v) => updateProfile.mutate({ use_adaptive_limit: v })} />
            </div>
            {!profile.use_adaptive_limit && (
              <div>
                <div className="mb-2 flex items-center justify-between">
                  <Label>Limite giornaliero</Label>
                  <span className="font-mono text-sm font-semibold">{profile.daily_limit}</span>
                </div>
                <Slider min={1} max={10} step={1} value={[profile.daily_limit]} onValueChange={([v]) => updateProfile.mutate({ daily_limit: v })} />
              </div>
            )}
            <div className="flex items-center justify-between rounded-lg border border-border p-3">
              <div>
                <Label>Notifiche di incoraggiamento</Label>
                <p className="text-xs text-muted-foreground">Toast in-app e promemoria streak</p>
              </div>
              <Switch checked={profile.notifications_enabled} onCheckedChange={(v) => updateProfile.mutate({ notifications_enabled: v })} />
            </div>
            <div className="space-y-3 rounded-lg border border-border p-3">
              <p className="text-xs uppercase tracking-wider text-muted-foreground">Cosa vuoi ricevere</p>
              <div className="flex items-center justify-between">
                <Label className="font-normal">💬 Messaggi</Label>
                <Switch checked={profile.notif_messages} onCheckedChange={(v) => updateProfile.mutate({ notif_messages: v })} />
              </div>
              <div className="flex items-center justify-between">
                <Label className="font-normal">🔥 Streak a rischio</Label>
                <Switch checked={profile.notif_streak} onCheckedChange={(v) => updateProfile.mutate({ notif_streak: v })} />
              </div>
              <div className="flex items-center justify-between">
                <Label className="font-normal">🛡️ Amici e clan</Label>
                <Switch checked={profile.notif_social} onCheckedChange={(v) => updateProfile.mutate({ notif_social: v })} />
              </div>
            </div>

            <Button variant="secondary" className="w-full" onClick={async () => {
              // Must run from a tap: browsers only show the permission prompt on a user gesture.
              const ok = isPushSupported() ? await subscribeToPush(profile.id) : (await ensureNotificationPermission()) === "granted";
              if (ok) toast.success("Notifiche attivate 🔔 Arriveranno anche ad app chiusa.");
              else toast.error("Notifiche bloccate: abilitale nelle impostazioni del telefono per SBO.");
            }}>
              <Bell className="mr-2 h-4 w-4" /> Attiva notifiche del sistema
            </Button>
            <Button variant="ghost" className="w-full text-xs" onClick={async () => {
              await schedulePush(profile.id, { deliverAt: new Date(), title: "SBO 🔔", body: "Notifica di prova: se la vedi, funziona tutto!", tag: "test", url: "/" });
              toast.success("Notifica di prova inviata: arriva entro un minuto. Puoi anche chiudere l'app.");
            }}>
              Invia notifica di prova
            </Button>
          </div>
          <DialogFooter><Button className="w-full" onClick={() => setShowSettings(false)}>Fatto</Button></DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ADD FRIEND */}
      <AddFriendDialog open={showAddFriend} onClose={() => setShowAddFriend(false)} onAdd={async (code) => {
        try {
          const name = await addFriend.mutateAsync(code);
          toast.success(`✅ ${name} aggiunto!`);
          setShowAddFriend(false);
        } catch (e: unknown) { toast.error((e as Error).message ?? "Errore"); }
      }} />

      {/* CLAN MODAL */}
      <ClanDialog open={showClanModal} onClose={() => setShowClanModal(false)}
        onCreate={async (name, tag) => {
          try { await createClan.mutateAsync({ name, tag }); toast.success(`🛡️ Clan [${tag}] ${name} creato!`); setShowClanModal(false); }
          catch (e: unknown) { const m = (e as Error).message; toast.error(m?.includes("duplicate") ? "Tag già usato" : m); }
        }}
        onJoin={async (tag) => {
          try { const c = await joinClan.mutateAsync(tag); toast.success(`Ti sei unito a ${c.name}!`); setShowClanModal(false); }
          catch (e: unknown) { toast.error((e as Error).message); }
        }} />

      {/* CLAN INVITE FRIEND */}
      {showClanInviteFriend && profile.clan_id && clanQ.data && (
        <InviteFriendToClanDialog
          userId={userId!}
          clanId={profile.clan_id}
          clanName={clanQ.data.name}
          myName={profile.username}
          friends={friends}
          existingMemberIds={new Set((clanMembersQ.data ?? []).map((m) => m.id))}
          onClose={() => setShowClanInviteFriend(false)}
        />
      )}

      {/* INCOMING INVITES */}
      <InvitesDialog open={showInvites} onClose={() => setShowInvites(false)} />

      {/* DIRECT CHAT */}
      {chatWith && userId && (
        <DirectChatDialog userId={userId} other={chatWith} myName={profile.username} onClose={() => setChatWith(null)} />
      )}

      {/* PROGETTO ZERO */}
      <ZeroQuestionnaire
        open={showQuiz !== false}
        reassess={showQuiz === "reassess"}
        redo={showQuiz === "redo"}
        initial={pz?.answers}
        initialTheme={pz?.theme}
        onClose={() => setShowQuiz(false)}
        onSubmit={async (answers, theme, league, startLimit) => {
          const nowIso = new Date().toISOString();
          answers = { ...answers }; delete (answers as { _redo?: boolean })._redo;
          try {
            if (showQuiz === "reassess" && pz) {
              await savePz.mutateAsync({ answers, league, theme, last_assessment_at: nowIso });
              toast.success(`Piano aggiornato: ${LEAGUE_LABEL[league]}`);
            } else {
              await savePz.mutateAsync({ active: true, started_at: nowIso, answers, league, theme, start_limit: startLimit, last_assessment_at: nowIso });
              toast.success("🎯 Progetto Zero attivato. Il coach ti ha scritto.");
              setTimeout(() => runTick("open"), 400);
            }
            setShowQuiz(false);
            setShowSettings(false);
          } catch (e) { toast.error((e as Error).message); }
        }}
      />
      {pzActive && todayPlan && (
        <PlanOverlay open={showPlanOverlay} plan={todayPlan} wd={todayWD} name={pz?.answers?.realName || profile.username} day={pzDay}
          onToggle={toggleExercise} onSkip={skipExercise} onCustom={() => setShowCustom(true)}
          onClose={() => setShowPlanOverlay(false)} />
      )}
      <CustomWorkoutDialog open={showCustom} onClose={() => setShowCustom(false)} onSave={addCustomWorkout} />
      {showAgent && <AgentChatDialog messages={agentMsgs} onClose={() => setShowAgent(false)} />}

      {/* CLAN CHAT */}
      {showClanChat && profile.clan_id && userId && (
        <ClanChatDialog clanId={profile.clan_id} userId={userId} clanName={clanQ.data?.name ?? "Clan"} members={clanMembersQ.data ?? []} onClose={() => setShowClanChat(false)} />
      )}

      {/* FRIEND PROFILE */}
      {viewFriend && <FriendProfileDialog friend={viewFriend} onClose={() => setViewFriend(null)} onChat={() => { setChatWith(viewFriend); setViewFriend(null); }} />}

      {/* RETRO-LOG: add/remove events on a past day */}
      <Dialog open={!!dayEdit} onOpenChange={(v) => !v && setDayEdit(null)}>
        <DialogContent className="max-w-xs">
          <DialogHeader>
            <DialogTitle>
              {dayEdit?.toLocaleDateString("it-IT", { weekday: "long", day: "numeric", month: "long" })}
            </DialogTitle>
            <DialogDescription>Correggi il registro se ti sei dimenticato (o hai premuto per sbaglio).</DialogDescription>
          </DialogHeader>
          <div className="flex items-center justify-center gap-5 py-2">
            <Button
              size="icon"
              variant="secondary"
              className="h-11 w-11 rounded-full"
              disabled={!dayEdit || (byDay.get(localDayKey(dayEdit)) ?? 0) === 0 || removeOnDay.isPending}
              onClick={() => dayEdit && removeOnDay.mutate(dayEdit)}
              aria-label="Togli un evento"
            >
              <X className="h-5 w-5" />
            </Button>
            <div className="text-center">
              <p className="font-display text-4xl font-bold">{dayEdit ? (byDay.get(localDayKey(dayEdit)) ?? 0) : 0}</p>
              <p className="text-[11px] uppercase tracking-wider text-muted-foreground">eventi</p>
            </div>
            <Button
              size="icon"
              className="h-11 w-11 rounded-full"
              disabled={!dayEdit || addEvent.isPending}
              onClick={() => {
                if (!dayEdit) return;
                const at = new Date(dayEdit);
                const now = new Date();
                const isToday = localDayKey(at) === localDayKey(now);
                if (isToday) at.setTime(now.getTime());
                else at.setHours(21, 0, 0, 0);
                addEvent.mutate(at);
              }}
              aria-label="Aggiungi un evento"
            >
              <Plus className="h-5 w-5" />
            </Button>
          </div>
          <DialogFooter>
            <Button variant="ghost" className="w-full" onClick={() => setDayEdit(null)}>Chiudi</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

// ============ Sub-components ============

function AvatarView({ profile, size = 44, className = "" }: { profile: { avatar: string; avatar_url: string | null }; size?: number; className?: string }) {
  return (
    <div className={`relative flex items-center justify-center overflow-hidden rounded-full bg-card/60 backdrop-blur ${className}`} style={{ width: size, height: size, fontSize: size * 0.55 }}>
      {profile.avatar_url ? (
        <img src={profile.avatar_url} alt="" className="h-full w-full object-cover" />
      ) : (
        <span>{profile.avatar}</span>
      )}
    </div>
  );
}

function Stat({ label, value, accent, icon }: { label: string; value: number; accent?: boolean; icon?: React.ReactNode }) {
  return (
    <Card className={`border-border/50 bg-card/60 p-4 backdrop-blur ${accent ? "ring-2 ring-destructive/50" : ""}`}>
      <p className="flex items-center gap-1 text-xs text-muted-foreground">{icon} {label}</p>
      <p className={`mt-1 text-3xl font-bold ${accent ? "text-destructive" : ""}`}>{value}</p>
    </Card>
  );
}

function BadgeSlot({ def, onClick }: { def: BadgeDef | null; onClick?: () => void }) {
  if (!def) {
    return (
      <button onClick={onClick}
        className="flex h-16 w-16 flex-col items-center justify-center rounded-2xl border-2 border-dashed border-primary/50 text-primary/70 bg-card/40 backdrop-blur transition hover:bg-card/80 hover:scale-105">
        <Plus className="h-5 w-5" />
        <span className="text-[9px] mt-0.5 font-semibold">slot</span>
      </button>
    );
  }
  return (
    <button onClick={onClick} title={`${def.name} — ${tierLabel(def.tier)} (${def.threshold}gg)`} className="transition hover:scale-105">
      <BadgeArt def={def} size={64} />
    </button>
  );
}


function FriendRowCard({ row, unread = 0, onOpenProfile, onChat, onRemove }: { row: FriendRow; unread?: number; onOpenProfile: () => void; onChat: () => void; onRemove: () => void }) {
  const eventsQ = useEventsOf(row.friend_id);
  const evs = eventsQ.data ?? [];
  const fire = computeFireStreak(evs);
  const ice = computeIceStreak(evs);
  const todayCount = countToday(evs);
  return (
    <Card className="border-border/50 bg-card/60 p-4 backdrop-blur">
      <div className="flex items-center gap-3">
        <button onClick={onOpenProfile} className="flex items-center gap-3 flex-1 min-w-0 text-left">
          <AvatarView profile={row.profile} size={40} />
          <div className="flex-1 min-w-0">
            <p className="font-medium truncate">{row.profile.username}</p>
            <p className="flex items-center gap-2 text-[11px] text-muted-foreground">
              <span className="flex items-center gap-0.5"><Flame className="h-3 w-3 text-orange-400" /> {fire}</span>
              <span className="flex items-center gap-0.5"><Snowflake className="h-3 w-3 text-sky-300" /> {ice}</span>
              <span className="flex items-center gap-0.5"><Droplets className="h-3 w-3 text-primary" /> {todayCount} oggi</span>
            </p>
          </div>
        </button>
        <Button size="sm" variant="secondary" className="relative" onClick={onChat}>
          <MessageCircle className="h-3.5 w-3.5" />
          {unread > 0 && (
            <span className="absolute -top-1.5 -right-1.5 grid h-4 min-w-4 place-items-center rounded-full bg-destructive px-1 text-[10px] font-bold text-white">
              {unread > 9 ? "9+" : unread}
            </span>
          )}
        </Button>
        <Button size="sm" variant="ghost" onClick={onRemove}><Trash2 className="h-3.5 w-3.5 text-muted-foreground" /></Button>
      </div>
    </Card>
  );
}


function ClanCard({ clan, members, meId, myRole, onOpenChat, onInvite }: {
  clan: { id: string; name: string; tag: string; owner_id: string };
  members: Profile[]; meId: string; myRole: Profile["clan_role"];
  onOpenChat: () => void; onInvite: () => void;
}) {
  const setRole = useSetClanRole();
  const kick = useKickClanMember();
  const canInvite = myRole === "leader" || myRole === "co_leader" || myRole === "recruit";
  const canKick = myRole === "leader";
  const canPromote = myRole === "leader";

  const sorted = [...members].sort((a, b) => {
    const order = { leader: 0, co_leader: 1, recruit: 2 } as const;
    return order[a.clan_role] - order[b.clan_role];
  });

  return (
    <Card className="border-border/50 bg-card/60 p-5 backdrop-blur">
      <div className="flex items-center gap-3">
        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-accent to-primary">
          <Shield className="h-5 w-5 text-primary-foreground" />
        </div>
        <div className="flex-1">
          <p className="text-xs text-muted-foreground">[{clan.tag}]</p>
          <p className="text-lg font-bold">{clan.name}</p>
        </div>
        <Button size="sm" variant="secondary" onClick={onOpenChat}>
          <MessageCircle className="mr-1.5 h-3.5 w-3.5" /> Chat
        </Button>
      </div>

      {canInvite && (
        <Button size="sm" className="w-full mt-3" variant="secondary" onClick={onInvite}>
          <UserPlus className="mr-1.5 h-3.5 w-3.5" /> Invita un amico
        </Button>
      )}

      <div className="mt-4 space-y-1.5">
        {sorted.map((m) => (
          <div key={m.id} className="flex items-center justify-between rounded-lg bg-background/30 px-3 py-2 text-sm">
            <span className="flex items-center gap-2 min-w-0">
              <AvatarView profile={m} size={28} />
              <span className={`truncate ${m.id === meId ? "font-semibold" : ""}`}>{m.username}{m.id === meId ? " (tu)" : ""}</span>
              <RoleBadge role={m.clan_role} />
            </span>
            {m.id !== meId && (canPromote || canKick) && (
              <div className="flex gap-1">
                {canPromote && m.clan_role === "recruit" && (
                  <button title="Promuovi a Vice" className="p-1 rounded hover:bg-card" onClick={() => setRole.mutate({ target: m.id, role: "co_leader" })}>
                    <Star className="h-3.5 w-3.5 text-amber-400" />
                  </button>
                )}
                {canPromote && m.clan_role === "co_leader" && (
                  <button title="Retrocedi a Recluta" className="p-1 rounded hover:bg-card" onClick={() => setRole.mutate({ target: m.id, role: "recruit" })}>
                    <UserMinus className="h-3.5 w-3.5 text-muted-foreground" />
                  </button>
                )}
                {canKick && (
                  <button title="Espelli" className="p-1 rounded hover:bg-destructive/20" onClick={() => { if (confirm(`Espellere ${m.username}?`)) kick.mutate(m.id); }}>
                    <X className="h-3.5 w-3.5 text-destructive" />
                  </button>
                )}
              </div>
            )}
          </div>
        ))}
      </div>
    </Card>
  );
}

function RoleBadge({ role }: { role: Profile["clan_role"] }) {
  const label = CLAN_ROLE_LABEL[role];
  const cls = role === "leader" ? "bg-amber-500/25 text-amber-300 border-amber-400/40"
    : role === "co_leader" ? "bg-primary/25 text-primary border-primary/40"
    : "bg-muted/30 text-muted-foreground border-border";
  const icon = role === "leader" ? <Crown className="h-3 w-3" /> : role === "co_leader" ? <Star className="h-3 w-3" /> : null;
  return <span className={`ml-1 inline-flex items-center gap-1 rounded-full border px-1.5 py-0.5 text-[9px] font-semibold ${cls}`}>{icon}{label}</span>;
}

function ProfileDialog({ open, onClose, profile, badges, onUpdate, onSignOut, onOpenInventory }: {
  open: boolean; onClose: () => void; profile: Profile; badges: Badge[];
  onUpdate: (p: Partial<Profile>) => void; onSignOut: () => void;
  onOpenInventory: (slot: 0 | 1 | 2) => void;
}) {
  const [uploading, setUploading] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);
  const equipped = [profile.slot_1, profile.slot_2, profile.slot_3];

  const handleFile = async (f: File) => {
    setUploading(true);
    try {
      const url = await compressImageToDataUrl(f);
      onUpdate({ avatar_url: url });
      toast.success("Foto profilo aggiornata!");
    } catch { toast.error("Impossibile caricare l'immagine"); }
    finally { setUploading(false); }
  };

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="border-border bg-card max-w-md max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Il tuo profilo</DialogTitle>
        </DialogHeader>
        <div className="space-y-5">
          {/* Avatar */}
          <div className="flex items-center gap-4">
            <AvatarView profile={profile} size={72} className="border-2 border-primary/40" />
            <div className="flex-1 space-y-2">
              <input ref={fileRef} type="file" accept="image/*" className="hidden"
                onChange={(e) => { const f = e.target.files?.[0]; if (f) handleFile(f); e.target.value = ""; }} />
              <Button size="sm" variant="secondary" className="w-full" disabled={uploading} onClick={() => fileRef.current?.click()}>
                <Camera className="mr-1.5 h-3.5 w-3.5" /> {uploading ? "Carico…" : "Carica foto"}
              </Button>
              {profile.avatar_url && (
                <Button size="sm" variant="ghost" className="w-full text-xs" onClick={() => onUpdate({ avatar_url: null })}>
                  Rimuovi foto
                </Button>
              )}
            </div>
          </div>

          <div>
            <Label className="text-xs uppercase tracking-wider text-muted-foreground">Avatar emoji</Label>
            <div className="mt-2 grid grid-cols-9 gap-1">
              {AVATAR_CHOICES.map((a) => (
                <button key={a} onClick={() => onUpdate({ avatar: a })}
                  className={`h-9 w-9 rounded-lg text-xl flex items-center justify-center transition ${profile.avatar === a ? "bg-primary/30 ring-2 ring-primary" : "hover:bg-card/80"}`}>
                  {a}
                </button>
              ))}
            </div>
            <p className="mt-1 text-[10px] text-muted-foreground">Usato quando non hai una foto.</p>
          </div>

          {/* Username display */}
          <div>
            <Label className="text-xs uppercase tracking-wider text-muted-foreground">Username</Label>
            <p className="mt-1 text-lg font-semibold">{profile.username}</p>
            {titleById(profile.equipped_title) && <TitleBadge title={titleById(profile.equipped_title)!} size="sm" />}
            <p className="text-[11px] text-muted-foreground">L'username non è modificabile dopo la creazione.</p>
          </div>

          {/* Friend code */}
          <div>
            <Label className="text-xs uppercase tracking-wider text-muted-foreground">Codice amico</Label>
            <p className="mt-1 font-mono text-lg tracking-widest">{profile.friend_code}</p>
          </div>

          {/* Slots (tap → inventory) */}
          <div>
            <Label className="text-xs uppercase tracking-wider text-muted-foreground">Slot medaglie</Label>
            <p className="text-[11px] text-muted-foreground">Tocca uno slot per scegliere dalla tua collezione.</p>
            <div className="mt-2 flex items-center justify-center gap-3">
              {[0, 1, 2].map((i) => {
                const bid = equipped[i];
                const b = bid ? badges.find((x) => x.id === bid) : null;
                const def = b ? badgeDef(b.category as BadgeCategory, b.threshold) ?? null : null;
                return <BadgeSlot key={i} def={def} onClick={() => onOpenInventory(i as 0 | 1 | 2)} />;
              })}
            </div>
          </div>

          <StreakProgress badges={badges} />

          <Button variant="destructive" className="w-full" onClick={onSignOut}>
            <LogOut className="mr-2 h-4 w-4" /> Esci dall'account
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}

function InventoryDialog({ slot, profile, badges, totalEvents, totalCleanDays, onClose, onEquip, onEquipTitle }: {
  slot: 0 | 1 | 2; profile: Profile; badges: Badge[]; totalEvents: number; totalCleanDays: number;
  onClose: () => void; onEquip: (id: string | null) => void; onEquipTitle: (id: string | null) => void;
}) {
  const equippedIds = new Set([profile.slot_1, profile.slot_2, profile.slot_3].filter(Boolean) as string[]);
  const [preview, setPreview] = useState<BadgeDef | null>(null);

  const renderGroup = (title: string, defs: BadgeDef[], category: BadgeCategory) => {
    const ownedThresholds = new Set(
      badges.filter((b) => b.category === category || (category === "fire" && b.category === "streak")).map((b) => b.threshold),
    );
    return (
      <div className="mt-4">
        <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">{title}</p>
        <div className="grid grid-cols-3 gap-3">
          {defs.map((def) => {
            const ownedBadge = badges.find(
              (b) => b.threshold === def.threshold && (b.category === category || (category === "fire" && b.category === "streak")),
            );
            const isOwned = !!ownedBadge && ownedThresholds.has(def.threshold);
            const isEquipped = ownedBadge && equippedIds.has(ownedBadge.id);
            return (
              <button
                key={`${category}-${def.threshold}`}
                onClick={() => (isOwned && ownedBadge ? onEquip(ownedBadge.id) : setPreview(def))}
                className={`relative flex flex-col items-center gap-1 rounded-xl transition hover:scale-[1.03] ${isEquipped ? "outline outline-2 outline-primary rounded-xl" : ""}`}
              >
                <BadgeArt def={def} size={84} locked={!isOwned} className="w-full" />
                <span className={`text-[10px] font-semibold text-center leading-tight ${isOwned ? "" : "text-muted-foreground"}`}>{def.name}</span>
                <span className="text-[9px] text-muted-foreground">{def.threshold}gg</span>
                {!isOwned && <Lock className="absolute top-1.5 right-1.5 h-3.5 w-3.5 text-white/90" />}
                {isEquipped && <span className="absolute top-1 left-1 text-[9px] bg-primary text-primary-foreground px-1 py-0.5 rounded">Equipaggiata</span>}
              </button>
            );

          })}
        </div>
      </div>
    );
  };

  return (
    <Dialog open onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="border-border bg-card max-w-md max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Inventario medaglie — Slot {slot + 1}</DialogTitle>
          <DialogDescription>Le medaglie che non hai sono in grigio. Toccale per vedere come sbloccarle.</DialogDescription>
        </DialogHeader>

        <button onClick={() => onEquip(null)} className="flex w-full items-center justify-center gap-2 rounded-xl border-2 border-dashed border-border p-3 text-muted-foreground text-xs hover:bg-card/50">
          <X className="h-4 w-4" /> Rimuovi da questo slot
        </button>

        {renderGroup("🔥 Streak di fuoco (fai ogni giorno)", FIRE_BADGES, "fire")}
        {renderGroup("❄️ Streak di ghiaccio (giorni puliti)", ICE_BADGES, "ice")}
        {renderGroup("💪 Allenamenti totali", WORKOUT_BADGES, "workout")}

        <TitlesSection totalEvents={totalEvents} totalCleanDays={totalCleanDays} equippedTitle={profile.equipped_title} onEquipTitle={onEquipTitle} />

        {preview && (
          <Dialog open onOpenChange={(v) => !v && setPreview(null)}>
            <DialogContent className="border-border bg-card overflow-hidden">
              <DialogHeader>
                <DialogTitle className="flex items-center gap-2"><Lock className="h-4 w-4" /> {preview.name} — {tierLabel(preview.tier)}</DialogTitle>
              </DialogHeader>
              <div className="relative flex flex-col items-center justify-center py-8 overflow-hidden rounded-xl" style={{ background: preview.gradient }}>
                <div className="absolute inset-0 pointer-events-none">
                  <FloatingSparkles seed={preview.threshold} count={14} />
                </div>
                <motion.div animate={{ y: [0, -8, 0], rotate: [0, 6, -6, 0] }} transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
                  className="relative text-7xl grayscale opacity-70">{preview.emoji}</motion.div>
                <p className="relative mt-4 text-white font-bold text-lg drop-shadow">{preview.name}</p>
                <p className="relative text-white/80 text-sm">Non ancora sbloccata</p>
              </div>
              <div className="rounded-lg border border-border p-4 text-sm">
                <p className="font-semibold mb-1">Come ottenerla</p>
                <p className="text-muted-foreground">
                  {preview.category === "workout" ? <>Completa <b>{preview.threshold} allenamenti</b> in totale. 💪</> : preview.category === "ice"
                    ? <>Non registrare alcun evento per <b>{preview.threshold} giorni di fila</b>. Le medaglie di ghiaccio sono le più preziose. ❄️</>
                    : <>Registra almeno un evento ogni giorno per <b>{preview.threshold} giorni di fila</b>. 🔥</>}
                </p>
              </div>
            </DialogContent>
          </Dialog>
        )}
      </DialogContent>
    </Dialog>
  );
}

/** Titoli onorifici basati sul totale complessivo di eventi / giorni puliti. */
function TitlesSection({ totalEvents, totalCleanDays, equippedTitle, onEquipTitle }: {
  totalEvents: number; totalCleanDays: number;
  equippedTitle: string | null; onEquipTitle: (id: string | null) => void;
}) {
  const equippedDef = titleById(equippedTitle);

  const Group = ({ title, list, total, unit }: { title: string; list: TitleDef[]; total: number; unit: string }) => {
    const cur = currentTitle(list[0].category, total);
    const next = nextTitle(list[0].category, total);
    return (
      <div className="mt-4">
        <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">{title}</p>
        <div className="rounded-xl border border-border p-3">
          <p className="text-xs text-muted-foreground">Totale: <b className="text-foreground">{total}</b> {unit}</p>
          <p className="mt-1 text-sm font-bold">{cur ? `${cur.emoji} ${cur.name}` : "Nessun titolo — ancora"}</p>
          {cur && <p className="text-[11px] text-muted-foreground">{titleTierLabel(cur.tier)}</p>}
          {next && (
            <div className="mt-2">
              <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
                <div className="h-full rounded-full bg-primary" style={{ width: `${Math.min(100, (total / next.threshold) * 100)}%` }} />
              </div>
              <p className="mt-1 text-[11px] text-muted-foreground">
                Prossimo: <b>{next.emoji} {next.name}</b> a {next.threshold} {unit}
              </p>
            </div>
          )}
        </div>
        <div className="mt-2 space-y-1">
          {list.map((t) => {
            const owned = total >= t.threshold;
            const isOn = equippedTitle === titleId(t);
            return (
              <button
                key={t.name}
                type="button"
                disabled={!owned}
                onClick={() => onEquipTitle(isOn ? null : titleId(t))}
                className={`flex w-full items-center gap-2 rounded-lg border px-2.5 py-1.5 text-left transition ${
                  isOn ? "border-primary bg-primary/10" : "border-border"
                } ${owned ? "hover:bg-card/70" : "opacity-50"}`}
              >
                <span className="text-base" style={owned ? undefined : { filter: "grayscale(1)" }}>{t.emoji}</span>
                <div className="min-w-0 flex-1">
                  {owned ? <TitleBadge title={t} size="sm" /> : <p className="truncate text-[12px] font-semibold">{t.name}</p>}
                  <p className="text-[10px] text-muted-foreground">{titleTierLabel(t.tier)} · {t.threshold} {unit}</p>
                </div>
                {!owned && <Lock className="h-3 w-3 text-muted-foreground" />}
                {isOn && <span className="rounded bg-primary px-1 py-0.5 text-[9px] font-bold text-primary-foreground">Equipaggiato</span>}
              </button>
            );
          })}
        </div>
      </div>
    );
  };

  return (
    <div className="mt-6 border-t border-border pt-2">
      <p className="text-sm font-bold">🏅 Titoli</p>
      <p className="text-[11px] text-muted-foreground">Si sbloccano con il totale complessivo, non con le streak.</p>

      {/* Slot titolo */}
      <div className="mt-3 rounded-xl border-2 border-dashed border-border p-3">
        <p className="text-[10px] uppercase tracking-wider text-muted-foreground">Slot titolo</p>
        <div className="mt-1 flex items-center justify-between gap-2">
          {equippedDef ? <TitleBadge title={equippedDef} size="md" /> : <span className="text-xs text-muted-foreground">Nessun titolo equipaggiato</span>}
          {equippedDef && (
            <Button size="sm" variant="ghost" className="h-7 text-[11px]" onClick={() => onEquipTitle(null)}>
              <X className="mr-1 h-3 w-3" /> Rimuovi
            </Button>
          )}
        </div>
        <p className="mt-1 text-[10px] text-muted-foreground">Appare sotto il tuo nome. Tocca un titolo sbloccato qui sotto per equipaggiarlo.</p>
      </div>

      <Group title="🔥 Titoli di fuoco" list={FIRE_TITLES} total={totalEvents} unit="eventi" />
      <Group title="❄️ Titoli di ghiaccio" list={ICE_TITLES} total={totalCleanDays} unit="giorni puliti" />
    </div>
  );
}

function FloatingSparkles({ seed, count = 8 }: { seed: number; count?: number }) {
  const rnd = (i: number) => { const x = Math.sin(seed * 13 + i * 991) * 10000; return x - Math.floor(x); };
  return (
    <>
      {Array.from({ length: count }, (_, i) => (
        <motion.span key={i}
          initial={{ opacity: 0, y: 0 }}
          animate={{ opacity: [0, 1, 0], y: -30 }}
          transition={{ duration: 2 + rnd(i) * 2, delay: rnd(i + 50) * 2, repeat: Infinity, ease: "easeOut" }}
          className="absolute text-yellow-100"
          style={{ left: `${rnd(i + 100) * 100}%`, bottom: `${rnd(i + 200) * 100}%`, fontSize: 8 + rnd(i + 300) * 8 }}>
          ✦
        </motion.span>
      ))}
    </>
  );
}

function StreakProgress({ badges }: { badges: Badge[] }) {
  const ownedFire = new Set(badges.filter((b) => b.category === "fire" || b.category === "streak").map((b) => b.threshold));
  const ownedIce = new Set(badges.filter((b) => b.category === "ice").map((b) => b.threshold));
  const nextFire = FIRE_BADGES.find((b) => !ownedFire.has(b.threshold));
  const nextIce = ICE_BADGES.find((b) => !ownedIce.has(b.threshold));
  if (!nextFire && !nextIce) return <p className="text-xs text-center text-muted-foreground">🏆 Hai tutte le medaglie!</p>;
  const Row = ({ n, label }: { n: BadgeDef; label: string }) => (
    <div className="mt-1 flex items-center gap-2">
      <span className="text-xl">{n.emoji}</span>
      <div className="flex-1">
        <p className="text-sm font-semibold">{n.name} <span className="text-[10px] font-normal text-muted-foreground">({tierLabel(n.tier)})</span></p>
        <p className="text-[11px] text-muted-foreground">{label}: {n.threshold} giorni</p>
      </div>
    </div>
  );
  return (
    <div className="space-y-2">
      {nextFire && <div className="rounded-lg border border-border p-3"><p className="text-xs text-muted-foreground">🔥 Prossima medaglia di fuoco</p><Row n={nextFire} label="streak" /></div>}
      {nextIce && <div className="rounded-lg border border-border p-3"><p className="text-xs text-muted-foreground">❄️ Prossima medaglia di ghiaccio</p><Row n={nextIce} label="giorni puliti" /></div>}
    </div>
  );
}

function AddFriendDialog({ open, onClose, onAdd }: { open: boolean; onClose: () => void; onAdd: (code: string) => void }) {
  const [code, setCode] = useState("");
  useEffect(() => { if (!open) setCode(""); }, [open]);
  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="border-border bg-card">
        <DialogHeader>
          <DialogTitle>Aggiungi un amico</DialogTitle>
          <DialogDescription>Inserisci il codice a 6 caratteri del tuo amico.</DialogDescription>
        </DialogHeader>
        <Input className="text-center font-mono text-xl tracking-widest uppercase" placeholder="XXXXXX" maxLength={6} value={code}
          onChange={(e) => setCode(e.target.value.toUpperCase())} onKeyDown={(e) => e.key === "Enter" && onAdd(code)} autoFocus />
        <DialogFooter><Button className="w-full" onClick={() => onAdd(code)} disabled={code.length < 4}>Aggiungi</Button></DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function ClanDialog({ open, onClose, onCreate, onJoin }: { open: boolean; onClose: () => void; onCreate: (name: string, tag: string) => void; onJoin: (tag: string) => void }) {
  const [tab, setTab] = useState<"create" | "join">("create");
  const [name, setName] = useState("");
  const [tag, setTag] = useState("");
  const [joinTag, setJoinTag] = useState("");
  useEffect(() => { if (!open) { setName(""); setTag(""); setJoinTag(""); } }, [open]);
  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="border-border bg-card">
        <DialogHeader><DialogTitle>Gestisci clan</DialogTitle></DialogHeader>
        <Tabs value={tab} onValueChange={(v) => setTab(v as "create" | "join")}>
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="create">Crea</TabsTrigger>
            <TabsTrigger value="join">Unisciti</TabsTrigger>
          </TabsList>
          <TabsContent value="create" className="space-y-3 mt-4">
            <div><Label>Nome del clan</Label><Input value={name} onChange={(e) => setName(e.target.value)} maxLength={24} placeholder="I Guerrieri della Goccia" /></div>
            <div><Label>Tag (2-5 caratteri)</Label><Input value={tag} onChange={(e) => setTag(e.target.value.toUpperCase().slice(0, 5))} className="font-mono uppercase" placeholder="ABC" /></div>
            <Button className="w-full" onClick={() => onCreate(name.trim(), tag.trim())} disabled={name.trim().length < 3 || tag.trim().length < 2}>Crea clan</Button>
          </TabsContent>
          <TabsContent value="join" className="space-y-3 mt-4">
            <div><Label>Tag del clan a cui unirti</Label><Input value={joinTag} onChange={(e) => setJoinTag(e.target.value.toUpperCase().slice(0, 5))} className="font-mono uppercase" placeholder="ABC" /></div>
            <Button className="w-full" onClick={() => onJoin(joinTag.trim())} disabled={joinTag.trim().length < 2}>Unisciti</Button>
          </TabsContent>
        </Tabs>
      </DialogContent>
    </Dialog>
  );
}

function InviteFriendToClanDialog({ userId, clanId, clanName, myName, friends, existingMemberIds, onClose }: {
  userId: string; clanId: string; clanName: string; myName: string;
  friends: FriendRow[]; existingMemberIds: Set<string>; onClose: () => void;
}) {
  const send = useSendClanInvite(userId);
  const available = friends.filter((f) => !existingMemberIds.has(f.friend_id));
  return (
    <Dialog open onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="border-border bg-card">
        <DialogHeader>
          <DialogTitle>Invita un amico nel clan</DialogTitle>
          <DialogDescription>Riceverà un messaggio privato con il tuo invito.</DialogDescription>
        </DialogHeader>
        {available.length === 0 ? (
          <p className="text-sm text-muted-foreground text-center py-4">Tutti i tuoi amici sono già nel clan, o non ne hai ancora.</p>
        ) : (
          <div className="space-y-2 max-h-80 overflow-auto">
            {available.map((f) => (
              <div key={f.friend_id} className="flex items-center gap-3 rounded-lg border border-border p-3">
                <AvatarView profile={f.profile} size={36} />
                <p className="flex-1 font-medium truncate">{f.profile.username}</p>
                <Button size="sm" onClick={async () => {
                  try { await send.mutateAsync({ clanId, toUserId: f.friend_id, myName, clanName }); toast.success(`Invito inviato a ${f.profile.username}`); }
                  catch { toast.error("Invito già presente o errore"); }
                }}>Invita</Button>
              </div>
            ))}
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

function InvitesDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { userId } = useAuthUser();
  const invitesQ = useIncomingInvites(userId);
  const accept = useAcceptInvite();
  const decline = useDeclineInvite();
  const invites = invitesQ.data ?? [];
  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="border-border bg-card">
        <DialogHeader>
          <DialogTitle>Notifiche</DialogTitle>
          <DialogDescription>Inviti al clan in attesa.</DialogDescription>
        </DialogHeader>
        {invites.length === 0 ? (
          <p className="text-sm text-muted-foreground text-center py-6">Nessun invito in sospeso.</p>
        ) : (
          <div className="space-y-2">
            {invites.map((inv) => <InviteRow key={inv.id} inviteId={inv.id} clanId={inv.clan_id} fromId={inv.from_user_id}
              onAccept={async () => { try { await accept.mutateAsync(inv.id); toast.success("Ti sei unito al clan!"); } catch (e: unknown) { toast.error((e as Error).message); } }}
              onDecline={async () => { await decline.mutateAsync(inv.id); toast("Invito rifiutato"); }} />)}
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

function InviteRow({ inviteId: _inviteId, clanId, fromId, onAccept, onDecline }: { inviteId: string; clanId: string; fromId: string; onAccept: () => void; onDecline: () => void }) {
  const clanQ = useClan(clanId);
  const [fromName, setFromName] = useState<string>("");
  useEffect(() => { supabase.from("profiles").select("username").eq("id", fromId).maybeSingle().then(({ data }) => setFromName(data?.username ?? "")); }, [fromId]);
  return (
    <div className="rounded-lg border border-border p-3 flex items-center gap-3">
      <Shield className="h-5 w-5 text-primary" />
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold truncate">{clanQ.data?.name ?? "Clan"} <span className="font-mono text-[10px] text-muted-foreground">[{clanQ.data?.tag}]</span></p>
        <p className="text-[11px] text-muted-foreground">Invito da {fromName || "…"}</p>
      </div>
      <Button size="sm" onClick={onAccept}><Check className="h-3.5 w-3.5" /></Button>
      <Button size="sm" variant="ghost" onClick={onDecline}><X className="h-3.5 w-3.5" /></Button>
    </div>
  );
}

function FriendProfileDialog({ friend, onClose, onChat }: { friend: Profile; onClose: () => void; onChat: () => void }) {
  const profileQ = useProfileById(friend.id);
  const visibleProfile = profileQ.data ?? friend;
  const eventsQ = useEventsOf(friend.id);
  const badgesQ = useBadgesOf(friend.id);
  const events = eventsQ.data ?? [];
  const badges = badgesQ.data ?? [];
  const fire = computeFireStreak(events);
  const ice = computeIceStreak(events);
  const month = countThisMonth(events);
  const byDay = useMemo(() => eventsByDay(events), [events]);
  const [avatarOpen, setAvatarOpen] = useState(false);
  return (
    <Dialog open onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="border-border bg-card max-w-md max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center gap-3">
            <button type="button" onClick={() => setAvatarOpen(true)} className="shrink-0 rounded-full focus:outline-none focus-visible:ring-2 focus-visible:ring-primary" aria-label={`Apri la foto di ${visibleProfile.username}`}>
              <AvatarView profile={visibleProfile} size={56} className="border-2 border-primary/40" />
            </button>
            <Dialog open={avatarOpen} onOpenChange={setAvatarOpen}>
              <DialogContent className="flex h-[100dvh] w-screen max-w-none items-center justify-center border-0 bg-black/95 p-0 sm:rounded-none" onClick={() => setAvatarOpen(false)}>
                <DialogTitle className="sr-only">Foto di {visibleProfile.username}</DialogTitle>
                {visibleProfile.avatar_url ? (
                  <img src={visibleProfile.avatar_url} alt={`Foto di ${visibleProfile.username}`} className="max-h-[100dvh] max-w-full object-contain" />
                ) : (
                  <span style={{ fontSize: "min(60vw, 60vh)", lineHeight: 1 }}>{visibleProfile.avatar}</span>
                )}
              </DialogContent>
            </Dialog>
            <div>
              <DialogTitle>{visibleProfile.username}</DialogTitle>
              {titleById(visibleProfile.equipped_title) ? (
                <TitleBadge title={titleById(visibleProfile.equipped_title) as TitleDef} size="sm" />
              ) : (
                <p className="font-mono text-[11px] text-muted-foreground">{visibleProfile.friend_code}</p>
              )}
            </div>
          </div>
        </DialogHeader>
        <div className="space-y-4">
          <div className="grid grid-cols-3 gap-3">
            <Card className="border-border/50 bg-card/60 p-3 backdrop-blur">
              <p className="text-xs text-muted-foreground flex items-center gap-1"><Flame className="h-3 w-3 text-orange-400" /> Fuoco</p>
              <p className="mt-1 text-2xl font-bold">{fire}</p>
            </Card>
            <Card className="border-border/50 bg-card/60 p-3 backdrop-blur">
              <p className="text-xs text-muted-foreground flex items-center gap-1"><Snowflake className="h-3 w-3 text-sky-300" /> Ghiaccio</p>
              <p className="mt-1 text-2xl font-bold">{ice}</p>
            </Card>
            <Card className="border-border/50 bg-card/60 p-3 backdrop-blur">
              <p className="text-xs text-muted-foreground">Mese</p>
              <p className="mt-1 text-2xl font-bold">{month}</p>
            </Card>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Card className="border-border/50 bg-card/60 p-3 backdrop-blur">
              <p className="text-xs text-muted-foreground">🔥 Record fuoco</p>
              <p className="mt-1 text-xl font-bold">{maxFireStreak(events)} gg</p>
            </Card>
            <Card className="border-border/50 bg-card/60 p-3 backdrop-blur">
              <p className="text-xs text-muted-foreground">❄️ Record ghiaccio</p>
              <p className="mt-1 text-xl font-bold">{maxIceStreak(events)} gg</p>
            </Card>
          </div>

          <div>
            <Label className="text-xs uppercase tracking-wider text-muted-foreground">Medaglie ({badges.length})</Label>
            {badges.length === 0 ? (
              <p className="mt-2 text-xs text-muted-foreground">Nessuna ancora.</p>
            ) : (
              <div className="mt-2 flex flex-wrap gap-2">
                {badges.map((b) => {
                  const def = badgeDef(b.category as BadgeCategory, b.threshold);
                  if (!def) return null;
                  return <BadgeArt key={b.id} def={def} size={52} />;
                })}
              </div>
            )}
          </div>


          <div>
            <Label className="text-xs uppercase tracking-wider text-muted-foreground">Calendario</Label>
            <Calendar mode="single" className="pointer-events-auto mx-auto mt-2"
              modifiers={{ logged: (d) => (byDay.get(localDayKey(d)) ?? 0) > 0 }}
              modifiersClassNames={{ logged: "relative after:absolute after:bottom-1 after:left-1/2 after:-translate-x-1/2 after:h-1 after:w-1 after:rounded-full after:bg-accent" }} />
          </div>

          <div>
            <Label className="text-xs uppercase tracking-wider text-muted-foreground">Andamento (30gg)</Label>
            {(() => {
              const fSeries = dailyCountsSeries(events, 30);
              if (fSeries.every((d) => d.count === 0)) {
                return <p className="mt-2 py-6 text-center text-xs text-muted-foreground">Nessun dato ancora.</p>;
              }
              return (
                <div className="mt-2 h-40 w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={fSeries} margin={{ top: 6, right: 8, left: -22, bottom: 0 }}>
                      <defs>
                        <linearGradient id={`gFriend-${friend.id}`} x1="0" y1="0" x2="0" y2="1">
                          <stop offset="0%" stopColor="#fb923c" stopOpacity={0.55} />
                          <stop offset="100%" stopColor="#fb923c" stopOpacity={0} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" stroke="#ffffff" opacity={0.15} />
                      <XAxis dataKey="date" tickFormatter={(s: string) => s.slice(5)} fontSize={10} stroke="#ffffff" tick={{ fill: "#ffffff" }} />
                      <YAxis allowDecimals={false} fontSize={10} stroke="#ffffff" tick={{ fill: "#ffffff" }} width={28} />
                      <RTooltip contentStyle={{ background: "#0f172a", border: "1px solid #334155", borderRadius: 8, fontSize: 12, color: "#fff" }} labelStyle={{ color: "#fff" }} />
                      <Area type="monotone" dataKey="count" stroke="#fb923c" fill={`url(#gFriend-${friend.id})`} strokeWidth={2} name="Al giorno" />
                      <Line type="monotone" dataKey="ma7" stroke="#38bdf8" strokeWidth={2} dot={false} name="Media 7gg" />
                    </AreaChart>
                  </ResponsiveContainer>
                </div>
              );
            })()}
          </div>

          <Button className="w-full" onClick={onChat}><MessageCircle className="mr-2 h-4 w-4" /> Manda un messaggio</Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}

function DirectChatDialog({ userId, other, myName, onClose }: { userId: string; other: Profile; myName: string; onClose: () => void }) {
  const msgsQ = useDirectMessages(userId, other.id);
  const send = useSendDirectMessage(userId);
  const markRead = useMarkDmsRead();
  const [text, setText] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => { scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" }); }, [msgsQ.data?.length]);
  // Mark incoming DMs as read whenever the list updates
  useEffect(() => {
    if (!msgsQ.data) return;
    const anyUnread = msgsQ.data.some((m) => m.sender_id === other.id && !m.read_at);
    if (anyUnread) markRead.mutate(other.id);
  }, [msgsQ.data, other.id]);

  const submit = async () => {
    const t = text.trim();
    if (!t) return;
    setText("");
    await send.mutateAsync({ to: other.id, content: t });
    inputRef.current?.focus();
  };
  const fmtTime = (iso: string) => new Date(iso).toLocaleTimeString("it-IT", { hour: "2-digit", minute: "2-digit" });
  const fmtDay = (iso: string) => {
    const d = new Date(iso);
    const today = new Date(); today.setHours(0, 0, 0, 0);
    const dk = new Date(d); dk.setHours(0, 0, 0, 0);
    const diff = Math.round((today.getTime() - dk.getTime()) / 86400000);
    if (diff === 0) return "Oggi";
    if (diff === 1) return "Ieri";
    return d.toLocaleDateString("it-IT", { day: "numeric", month: "long", year: "numeric" });
  };
  const messages = msgsQ.data ?? [];
  return (
    <Dialog open={true} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="border-border bg-card max-w-md flex flex-col h-[80vh]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <AvatarView profile={other} size={32} />
            {other.username}
          </DialogTitle>
        </DialogHeader>
        <div ref={scrollRef} className="flex-1 overflow-y-auto pr-2">
          <div className="space-y-2 py-2">
            {messages.map((m, idx) => {
              const mine = m.sender_id === userId;
              const prev = idx > 0 ? messages[idx - 1] : null;
              const showDay = !prev || localDayKey(prev.created_at) !== localDayKey(m.created_at);
              return (
                <div key={m.id}>
                  {showDay && (
                    <div className="my-3 flex justify-center">
                      <span className="rounded-full bg-muted px-3 py-1 text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
                        {fmtDay(m.created_at)}
                      </span>
                    </div>
                  )}
                  <div className={`flex ${mine ? "justify-end" : "justify-start"}`}>
                    <div className={`max-w-[75%] rounded-2xl px-3 py-2 text-sm ${mine ? "bg-secondary text-secondary-foreground" : "bg-primary text-primary-foreground"}`}>
                      <p className="whitespace-pre-wrap break-words">{m.content}</p>
                      <div className={`mt-0.5 flex items-center justify-end gap-1 text-[10px] ${mine ? "text-secondary-foreground/70" : "text-primary-foreground/70"}`}>
                        <span>{fmtTime(m.created_at)}</span>
                        {mine && (m.read_at
                          ? <CheckCheck className="h-3 w-3 text-sky-300" />
                          : <Check className="h-3 w-3 opacity-70" />)}
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
            {(msgsQ.data ?? []).length === 0 && (
              <p className="text-center text-xs text-muted-foreground py-8">Inizia la conversazione, {myName}!</p>
            )}
          </div>
        </div>
        <form className="flex gap-2 pt-2 border-t border-border" onSubmit={(e) => { e.preventDefault(); submit(); }}>
          <Input ref={inputRef} value={text} onChange={(e) => setText(e.target.value)} placeholder="Scrivi un messaggio…" maxLength={500} autoFocus />
          <Button type="submit" disabled={!text.trim()}><Send className="h-4 w-4" /></Button>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function ClanChatDialog({ clanId, userId, clanName, members, onClose }: { clanId: string; userId: string; clanName: string; members: Profile[]; onClose: () => void }) {
  const msgsQ = useClanMessages(clanId);
  const send = useSendClanMessage(userId, clanId);
  const [text, setText] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);
  const scrollRef = useRef<HTMLDivElement>(null);
  const memberMap = useMemo(() => new Map(members.map((m) => [m.id, m])), [members]);

  useEffect(() => { scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" }); }, [msgsQ.data?.length]);

  const submit = async () => {
    const t = text.trim();
    if (!t) return;
    setText("");
    await send.mutateAsync(t);
    inputRef.current?.focus();
  };
  return (
    <Dialog open={true} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="border-border bg-card max-w-md flex flex-col h-[80vh]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2"><Shield className="h-5 w-5" /> {clanName}</DialogTitle>
          <DialogDescription>Gli annunci automatici arrivano quando qualcuno vince una medaglia epica o leggendaria.</DialogDescription>
        </DialogHeader>
        <div ref={scrollRef} className="flex-1 overflow-y-auto pr-2">
          <div className="space-y-2 py-2">
            {(msgsQ.data ?? []).map((m) => {
              if (m.is_system) {
                return (
                  <div key={m.id} className="flex justify-center">
                    <div className="max-w-[85%] rounded-xl bg-gradient-to-r from-amber-500/20 to-fuchsia-500/20 border border-amber-500/40 px-3 py-2 text-xs text-center">
                      {m.content}
                    </div>
                  </div>
                );
              }
              const mine = m.sender_id === userId;
              const sender = m.sender_id ? memberMap.get(m.sender_id) : null;
              return (
                <div key={m.id} className={`flex ${mine ? "justify-end" : "justify-start"}`}>
                  <div className={`max-w-[75%] rounded-2xl px-3 py-2 text-sm ${mine ? "bg-secondary text-secondary-foreground" : "bg-primary text-primary-foreground"}`}>
                    {!mine && sender && <p className="text-[10px] font-semibold opacity-80 mb-0.5">{sender.username}</p>}
                    {m.content}
                  </div>
                </div>
              );
            })}
            {(msgsQ.data ?? []).length === 0 && (
              <p className="text-center text-xs text-muted-foreground py-8">Nessun messaggio ancora. Rompi il ghiaccio!</p>
            )}
          </div>
        </div>
        <form className="flex gap-2 pt-2 border-t border-border" onSubmit={(e) => { e.preventDefault(); submit(); }}>
          <Input ref={inputRef} value={text} onChange={(e) => setText(e.target.value)} placeholder="Messaggio al clan…" maxLength={500} autoFocus />
          <Button type="submit" disabled={!text.trim()}><Send className="h-4 w-4" /></Button>
        </form>
      </DialogContent>
    </Dialog>
  );
}

// ---------- Falling drops (varied, hardware-accelerated) ----------

function FallingDrops({ seed }: { seed: number }) {
  const drops = useMemo(() => {
    const rnd = (i: number) => { const x = Math.sin(seed + i * 999) * 10000; return x - Math.floor(x); };
    // Random burst size: sometimes light, sometimes heavy
    const intensity = rnd(1);
    const count = Math.round(14 + intensity * 26); // 14–40
    return Array.from({ length: count }, (_, i) => {
      const r = rnd(i);
      const isBig = r > 0.85;
      const isSmall = r < 0.35;
      const baseSize = isBig ? 20 + rnd(i + 10) * 18 : isSmall ? 5 + rnd(i + 20) * 5 : 9 + rnd(i + 30) * 8;
      return {
        left: rnd(i + 100) * 100,
        delay: rnd(i + 200) * 0.7,
        duration: 0.9 + rnd(i + 300) * 1.1,
        size: baseSize,
        drift: (rnd(i + 400) - 0.5) * 60,
        opacity: 0.55 + rnd(i + 500) * 0.45,
        rotate: (rnd(i + 600) - 0.5) * 20,
      };
    });
  }, [seed]);
  return (
    <>
      {drops.map((d, i) => (
        <motion.span
          key={i}
          initial={{ y: -80, x: 0, opacity: 0, scale: 0.6, rotate: d.rotate }}
          animate={{ y: "110vh", x: d.drift, opacity: [0, d.opacity, d.opacity, 0], scale: 1, rotate: d.rotate }}
          transition={{ duration: d.duration, delay: d.delay, ease: [0.35, 0.05, 0.5, 0.9] }}
          className="absolute top-0 block will-change-transform"
          style={{ left: `${d.left}%`, width: d.size, height: d.size * 1.4 }}
        >
          <svg viewBox="0 0 20 28" className="h-full w-full drop-shadow-glow">
            <path d="M10 1 C 10 8, 18 14, 18 20 A 8 8 0 0 1 2 20 C 2 14, 10 8, 10 1 Z" fill="white" opacity="0.95" />
          </svg>
        </motion.span>
      ))}
    </>
  );
}
