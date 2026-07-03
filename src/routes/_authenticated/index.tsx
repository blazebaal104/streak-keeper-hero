import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
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
import { Plus, Droplets, Flame, CalendarDays, Users, Undo2, Sparkles, TrendingUp, Settings, Copy, UserPlus, Shield, X, LogOut, MessageCircle, Send, User as UserIcon, Trash2 } from "lucide-react";
import logo from "@/assets/sbo-logo.png";
import { supabase } from "@/integrations/supabase/client";
import {
  useAuthUser, useMyProfile, useUpdateProfile, useEvents, useAddEvent, useUndoLastEvent,
  useBadges, checkStreakBadges, useFriends, useAddFriendByCode, useRemoveFriend,
  useClan, useClanMembers, useCreateClan, useJoinClanByTag, useLeaveClan,
  useDirectMessages, useSendDirectMessage, useClanMessages, useSendClanMessage,
  countToday, countThisMonth, computeStreak, eventsByDay, adaptiveLimit,
  type Profile, type Badge,
} from "@/lib/sbo-cloud";
import { STREAK_BADGES, badgeByThreshold, tierLabel, type BadgeDef } from "@/lib/sbo-badges";
import { encourage, slowdown, restart } from "@/lib/sbo-phrases";

const AVATAR_CHOICES = ["💧","🌊","⚡","🌟","🔥","🌙","⚔️","👑","🐉","🦁","🐺","🦅","🌸","🍀","🎯","🧊","☄️","🌈"];

export const Route = createFileRoute("/_authenticated/")({
  component: SboApp,
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

  const addEvent = useAddEvent(userId);
  const undoLast = useUndoLastEvent(userId);
  const updateProfile = useUpdateProfile(userId);
  const addFriend = useAddFriendByCode(userId);
  const removeFriend = useRemoveFriend(userId);
  const createClan = useCreateClan(userId);
  const joinClan = useJoinClanByTag(userId);
  const leaveClan = useLeaveClan(userId);

  const [showProfile, setShowProfile] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [showAddFriend, setShowAddFriend] = useState(false);
  const [showClanModal, setShowClanModal] = useState(false);
  const [chatWith, setChatWith] = useState<Profile | null>(null);
  const [showClanChat, setShowClanChat] = useState(false);
  const [bursts, setBursts] = useState<number[]>([]);

  const profile = profileQ.data;
  const events = eventsQ.data ?? [];
  const badges = badgesQ.data ?? [];
  const friends = friendsQ.data ?? [];

  const today = countToday(events);
  const month = countThisMonth(events);
  const streak = computeStreak(events);
  const limit = profile ? (profile.use_adaptive_limit ? adaptiveLimit(events, profile.daily_limit) : profile.daily_limit) : 3;
  const byDay = useMemo(() => eventsByDay(events), [events]);

  // Trigger streak-badge check daily & on load
  useEffect(() => {
    if (!userId) return;
    checkStreakBadges(userId).catch(() => {});
  }, [userId, events.length]);

  const handleAdd = async () => {
    await addEvent.mutateAsync();
    const id = Date.now();
    setBursts((b) => [...b, id]);
    setTimeout(() => setBursts((b) => b.filter((x) => x !== id)), 2200);

    const newToday = today + 1;
    const name = profile?.username ?? "";
    if (!profile?.notifications_enabled) return;
    if (newToday >= limit) toast("🌊 Fermati un attimo", { description: slowdown(name) });
    else if (streak > 0) toast("💧 Streak resettata", { description: restart(name) });
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
    return badgeByThreshold(b.threshold) ?? null;
  });

  return (
    <div className="min-h-screen text-foreground">
      <Toaster position="top-center" theme="dark" />

      {/* Falling drops overlay */}
      <div className="pointer-events-none fixed inset-0 z-40 overflow-hidden">
        <AnimatePresence>
          {bursts.map((id) => <FallingDrops key={id} seed={id} />)}
        </AnimatePresence>
      </div>

      {/* Header */}
      <header className="mx-auto flex max-w-3xl items-center justify-between px-6 pt-8 pb-4">
        <button className="flex items-center gap-3 text-left" onClick={() => setShowProfile(true)} aria-label="Apri profilo">
          <div className="relative">
            <div className="flex h-11 w-11 items-center justify-center rounded-full text-2xl border-2 border-primary/40 bg-card/60 backdrop-blur">
              {profile.avatar}
            </div>
          </div>
          <div>
            <h1 className="text-lg font-bold tracking-tight leading-tight">{profile.username}</h1>
            <p className="text-[11px] text-muted-foreground font-mono">{profile.friend_code}</p>
          </div>
        </button>
        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1.5 rounded-full border border-border bg-card/60 px-3 py-2 backdrop-blur">
            <Flame className="h-4 w-4 text-accent" />
            <span className="text-sm font-semibold">{streak}</span>
          </div>
          <Button variant="ghost" size="icon" className="rounded-full border border-border bg-card/60 backdrop-blur" onClick={() => setShowSettings(true)} aria-label="Impostazioni">
            <Settings className="h-4 w-4" />
          </Button>
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-6 pb-24">
        <Tabs defaultValue="home" className="w-full">
          <TabsList className="mx-auto grid w-full max-w-sm grid-cols-3 rounded-full bg-card/60 backdrop-blur">
            <TabsTrigger value="home" className="rounded-full"><Droplets className="mr-1.5 h-4 w-4" /> Home</TabsTrigger>
            <TabsTrigger value="calendar" className="rounded-full"><CalendarDays className="mr-1.5 h-4 w-4" /> Calendario</TabsTrigger>
            <TabsTrigger value="friends" className="rounded-full"><Users className="mr-1.5 h-4 w-4" /> Social</TabsTrigger>
          </TabsList>

          {/* HOME */}
          <TabsContent value="home" className="mt-8 space-y-8">
            <div className="grid grid-cols-3 gap-3">
              <Stat label="Oggi" value={today} accent={today >= limit} />
              <Stat label="Questo mese" value={month} />
              <Stat label={profile.use_adaptive_limit ? "Limite adattivo" : "Limite"} value={limit} icon={profile.use_adaptive_limit ? <TrendingUp className="h-3.5 w-3.5" /> : undefined} />
            </div>

            {/* Equipped badges strip */}
            <div className="flex items-center justify-center gap-4">
              {equipped.map((b, i) => (
                <BadgeSlot key={i} def={b} onClick={() => setShowProfile(true)} />
              ))}
            </div>

            <div className="relative flex flex-col items-center justify-center py-6">
              <motion.button onClick={handleAdd} whileTap={{ scale: 0.9 }} whileHover={{ scale: 1.04 }} transition={{ type: "spring", stiffness: 400, damping: 15 }}
                className="group relative flex h-48 w-48 items-center justify-center rounded-full"
                style={{ background: "var(--gradient-drop)", boxShadow: "var(--shadow-drop)" }}
                aria-label="Aggiungi 1 al contatore">
                <div className="absolute inset-2 rounded-full bg-gradient-to-br from-white/40 to-transparent" />
                <Plus className="relative h-16 w-16 text-primary-foreground" strokeWidth={2.5} />
              </motion.button>
              <p className="mt-8 text-center text-sm text-muted-foreground">Premi per aggiungere. Tutto è salvato sul tuo account.</p>
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
                    {today >= limit ? slowdown(profile.username) : streak >= 3 ? `Sei a ${streak} giorni. ${encourage(profile.username)}` : encourage(profile.username)}
                  </p>
                </div>
              </div>
            </Card>
          </TabsContent>

          {/* CALENDAR */}
          <TabsContent value="calendar" className="mt-8">
            <Card className="border-border/50 bg-card/60 p-4 backdrop-blur">
              <p className="mb-3 px-2 text-sm text-muted-foreground">Registro personale — solo per uso medico e di monitoraggio.</p>
              <Calendar mode="single" className="pointer-events-auto mx-auto"
                modifiers={{
                  logged: (d) => (byDay.get(d.toISOString().slice(0, 10)) ?? 0) > 0,
                  heavy: (d) => (byDay.get(d.toISOString().slice(0, 10)) ?? 0) >= limit,
                }}
                modifiersClassNames={{
                  logged: "relative after:absolute after:bottom-1 after:left-1/2 after:-translate-x-1/2 after:h-1 after:w-1 after:rounded-full after:bg-accent",
                  heavy: "relative after:absolute after:bottom-1 after:left-1/2 after:-translate-x-1/2 after:h-1.5 after:w-1.5 after:rounded-full after:bg-destructive",
                }} />
              <div className="mt-4 flex justify-center gap-4 text-xs text-muted-foreground">
                <span className="flex items-center gap-1.5"><span className="h-1.5 w-1.5 rounded-full bg-accent" /> attività</span>
                <span className="flex items-center gap-1.5"><span className="h-2 w-2 rounded-full bg-destructive" /> sopra il limite</span>
              </div>
            </Card>
          </TabsContent>

          {/* SOCIAL */}
          <TabsContent value="friends" className="mt-8 space-y-6">
            {/* Friend code card */}
            <Card className="border-border/50 bg-card/60 p-5 backdrop-blur">
              <p className="text-xs text-muted-foreground uppercase tracking-wider">Il tuo codice amico</p>
              <div className="mt-2 flex items-center gap-2">
                <p className="flex-1 font-mono text-2xl font-bold tracking-widest">{profile.friend_code}</p>
                <Button size="sm" variant="secondary" onClick={async () => {
                  await navigator.clipboard.writeText(profile.friend_code);
                  toast("📋 Codice copiato!");
                }}><Copy className="mr-1.5 h-3.5 w-3.5" /> Copia</Button>
              </div>
              <p className="mt-2 text-[11px] text-muted-foreground">Condividilo con chi vuoi aggiungere. Ogni codice è unico.</p>
            </Card>

            {/* Clan */}
            <div>
              <div className="mb-2 flex items-center justify-between px-2">
                <p className="text-xs uppercase tracking-wider text-muted-foreground">Clan</p>
                {profile.clan_id && clanQ.data && (
                  <button className="text-xs text-muted-foreground hover:text-destructive" onClick={async () => { await leaveClan.mutateAsync(); toast("Hai lasciato il clan"); }}>
                    <X className="mr-1 inline h-3 w-3" /> esci
                  </button>
                )}
              </div>
              {profile.clan_id && clanQ.data ? (
                <Card className="border-border/50 bg-card/60 p-5 backdrop-blur">
                  <div className="flex items-center gap-3">
                    <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-accent to-primary">
                      <Shield className="h-5 w-5 text-primary-foreground" />
                    </div>
                    <div className="flex-1">
                      <p className="text-xs text-muted-foreground">[{clanQ.data.tag}]</p>
                      <p className="text-lg font-bold">{clanQ.data.name}</p>
                    </div>
                    <Button size="sm" variant="secondary" onClick={() => setShowClanChat(true)}>
                      <MessageCircle className="mr-1.5 h-3.5 w-3.5" /> Chat
                    </Button>
                  </div>
                  <div className="mt-4 space-y-1.5">
                    {(clanMembersQ.data ?? []).map((m) => (
                      <div key={m.id} className="flex items-center justify-between rounded-lg bg-background/30 px-3 py-2 text-sm">
                        <span className="flex items-center gap-2">
                          <span className="text-lg">{m.avatar}</span>
                          <span className={m.id === userId ? "font-semibold" : ""}>{m.username}{m.id === userId ? " (tu)" : ""}</span>
                        </span>
                      </div>
                    ))}
                  </div>
                </Card>
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
                    <Card key={f.friend_id} className="border-border/50 bg-card/60 p-4 backdrop-blur">
                      <div className="flex items-center gap-3">
                        <span className="text-2xl">{f.profile.avatar}</span>
                        <div className="flex-1 min-w-0">
                          <p className="font-medium truncate">{f.profile.username}</p>
                          <p className="font-mono text-[10px] text-muted-foreground">{f.profile.friend_code}</p>
                        </div>
                        <Button size="sm" variant="secondary" onClick={() => setChatWith(f.profile)}>
                          <MessageCircle className="h-3.5 w-3.5" />
                        </Button>
                        <Button size="sm" variant="ghost" onClick={() => { if (confirm(`Rimuovere ${f.profile.username}?`)) removeFriend.mutate(f.friend_id); }}>
                          <Trash2 className="h-3.5 w-3.5 text-muted-foreground" />
                        </Button>
                      </div>
                    </Card>
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
      />

      {/* SETTINGS */}
      <Dialog open={showSettings} onOpenChange={setShowSettings}>
        <DialogContent className="border-border bg-card">
          <DialogHeader>
            <DialogTitle>Impostazioni</DialogTitle>
            <DialogDescription>Personalizza SBO come vuoi tu.</DialogDescription>
          </DialogHeader>
          <div className="space-y-5 py-2">
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
                <p className="text-xs text-muted-foreground">Frasi motivazionali quando premi il pulsante</p>
              </div>
              <Switch checked={profile.notifications_enabled} onCheckedChange={(v) => updateProfile.mutate({ notifications_enabled: v })} />
            </div>
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
        } catch (e: any) { toast.error(e.message ?? "Errore"); }
      }} />

      {/* CLAN MODAL */}
      <ClanDialog open={showClanModal} onClose={() => setShowClanModal(false)}
        onCreate={async (name, tag) => {
          try { await createClan.mutateAsync({ name, tag }); toast.success(`🛡️ Clan [${tag}] ${name} creato!`); setShowClanModal(false); }
          catch (e: any) { toast.error(e.message?.includes("duplicate") ? "Tag già usato" : e.message); }
        }}
        onJoin={async (tag) => {
          try { const c = await joinClan.mutateAsync(tag); toast.success(`Ti sei unito a ${c.name}!`); setShowClanModal(false); }
          catch (e: any) { toast.error(e.message); }
        }} />

      {/* DIRECT CHAT */}
      {chatWith && userId && (
        <DirectChatDialog userId={userId} other={chatWith} myName={profile.username} onClose={() => setChatWith(null)} />
      )}

      {/* CLAN CHAT */}
      {showClanChat && profile.clan_id && userId && (
        <ClanChatDialog clanId={profile.clan_id} userId={userId} clanName={clanQ.data?.name ?? "Clan"} members={clanMembersQ.data ?? []} onClose={() => setShowClanChat(false)} />
      )}
    </div>
  );
}

// ============ Sub-components ============

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
      <button onClick={onClick} className="flex h-16 w-16 items-center justify-center rounded-2xl border-2 border-dashed border-border/60 text-muted-foreground text-xs">
        +
      </button>
    );
  }
  return (
    <button onClick={onClick} className={`relative flex h-16 w-16 items-center justify-center rounded-2xl ring-2 ${def.ring}`} style={{ background: def.gradient }}>
      <span className="text-3xl drop-shadow">{def.emoji}</span>
      {def.shiny && (
        <>
          <span className="absolute -top-1 -right-1 text-yellow-200 animate-pulse text-sm">✨</span>
          <span className="absolute -bottom-1 -left-1 text-yellow-200 animate-pulse text-xs" style={{ animationDelay: "0.4s" }}>✨</span>
        </>
      )}
    </button>
  );
}

function ProfileDialog({ open, onClose, profile, badges, onUpdate, onSignOut }: {
  open: boolean; onClose: () => void; profile: Profile; badges: Badge[];
  onUpdate: (p: Partial<Profile>) => void; onSignOut: () => void;
}) {
  const [pickSlot, setPickSlot] = useState<0 | 1 | 2 | null>(null);

  const equipped = [profile.slot_1, profile.slot_2, profile.slot_3];

  const setSlot = (idx: 0 | 1 | 2, badgeId: string | null) => {
    const patch: Partial<Profile> = {};
    if (idx === 0) patch.slot_1 = badgeId;
    if (idx === 1) patch.slot_2 = badgeId;
    if (idx === 2) patch.slot_3 = badgeId;
    onUpdate(patch);
    setPickSlot(null);
  };

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="border-border bg-card max-w-md">
        <DialogHeader>
          <DialogTitle>Il tuo profilo</DialogTitle>
        </DialogHeader>
        <div className="space-y-5">
          {/* Avatar picker */}
          <div>
            <Label className="text-xs uppercase tracking-wider text-muted-foreground">Avatar</Label>
            <div className="mt-2 grid grid-cols-9 gap-1">
              {AVATAR_CHOICES.map((a) => (
                <button key={a} onClick={() => onUpdate({ avatar: a })}
                  className={`h-9 w-9 rounded-lg text-xl flex items-center justify-center transition ${profile.avatar === a ? "bg-primary/30 ring-2 ring-primary" : "hover:bg-card/80"}`}>
                  {a}
                </button>
              ))}
            </div>
          </div>

          {/* Username display */}
          <div>
            <Label className="text-xs uppercase tracking-wider text-muted-foreground">Username</Label>
            <p className="mt-1 text-lg font-semibold">{profile.username}</p>
            <p className="text-[11px] text-muted-foreground">L'username non è modificabile una volta creato.</p>
          </div>

          {/* Friend code */}
          <div>
            <Label className="text-xs uppercase tracking-wider text-muted-foreground">Codice amico</Label>
            <p className="mt-1 font-mono text-lg tracking-widest">{profile.friend_code}</p>
          </div>

          {/* Equipped badge slots */}
          <div>
            <Label className="text-xs uppercase tracking-wider text-muted-foreground">Slot medaglie</Label>
            <div className="mt-2 flex items-center justify-center gap-3">
              {[0, 1, 2].map((i) => {
                const bid = equipped[i];
                const b = bid ? badges.find((x) => x.id === bid) : null;
                const def = b ? badgeByThreshold(b.threshold) ?? null : null;
                return <BadgeSlot key={i} def={def} onClick={() => setPickSlot(i as 0 | 1 | 2)} />;
              })}
            </div>
          </div>

          {/* Badge gallery */}
          <div>
            <Label className="text-xs uppercase tracking-wider text-muted-foreground">Le tue medaglie ({badges.length})</Label>
            {badges.length === 0 ? (
              <p className="mt-2 text-sm text-muted-foreground text-center py-4">Nessuna medaglia ancora. Continua la tua streak per sbloccarne!</p>
            ) : (
              <div className="mt-2 grid grid-cols-4 gap-2">
                {badges.map((b) => {
                  const def = badgeByThreshold(b.threshold);
                  if (!def) return null;
                  return (
                    <div key={b.id} className={`flex flex-col items-center gap-1 rounded-xl p-2 ring-2 ${def.ring}`} style={{ background: def.gradient }}>
                      <span className="text-2xl">{def.emoji}</span>
                      <span className="text-[10px] font-semibold text-white text-center leading-tight">{def.name}</span>
                      <span className="text-[9px] text-white/80">{b.threshold}gg</span>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Streak progress towards next */}
          <StreakProgress badges={badges} />

          <Button variant="destructive" className="w-full" onClick={onSignOut}>
            <LogOut className="mr-2 h-4 w-4" /> Esci dall'account
          </Button>
        </div>

        {/* Slot picker sub-dialog */}
        <Dialog open={pickSlot !== null} onOpenChange={(v) => !v && setPickSlot(null)}>
          <DialogContent className="border-border bg-card">
            <DialogHeader>
              <DialogTitle>Scegli medaglia per slot {pickSlot !== null ? pickSlot + 1 : ""}</DialogTitle>
            </DialogHeader>
            <div className="grid grid-cols-3 gap-2 max-h-72 overflow-auto">
              <button onClick={() => pickSlot !== null && setSlot(pickSlot, null)} className="flex flex-col items-center gap-1 rounded-xl border-2 border-dashed border-border p-3 text-muted-foreground text-xs">
                <X className="h-6 w-6" /> Nessuna
              </button>
              {badges.map((b) => {
                const def = badgeByThreshold(b.threshold);
                if (!def) return null;
                return (
                  <button key={b.id} onClick={() => pickSlot !== null && setSlot(pickSlot, b.id)}
                    className={`flex flex-col items-center gap-1 rounded-xl p-3 ring-2 ${def.ring}`} style={{ background: def.gradient }}>
                    <span className="text-2xl">{def.emoji}</span>
                    <span className="text-[10px] font-semibold text-white">{def.name}</span>
                  </button>
                );
              })}
            </div>
          </DialogContent>
        </Dialog>
      </DialogContent>
    </Dialog>
  );
}

function StreakProgress({ badges }: { badges: Badge[] }) {
  const owned = new Set(badges.filter((b) => b.category === "streak").map((b) => b.threshold));
  const next = STREAK_BADGES.find((b) => !owned.has(b.threshold));
  if (!next) return <p className="text-xs text-center text-muted-foreground">🏆 Hai tutte le medaglie streak!</p>;
  return (
    <div className="rounded-lg border border-border p-3">
      <p className="text-xs text-muted-foreground">Prossima medaglia streak</p>
      <div className="mt-1 flex items-center gap-2">
        <span className="text-xl">{next.emoji}</span>
        <div className="flex-1">
          <p className="text-sm font-semibold">{next.name} <span className="text-[10px] font-normal text-muted-foreground">({tierLabel(next.tier)})</span></p>
          <p className="text-[11px] text-muted-foreground">{next.threshold} giorni di streak</p>
        </div>
      </div>
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
            <div><Label>Tag del clan da unirti</Label><Input value={joinTag} onChange={(e) => setJoinTag(e.target.value.toUpperCase().slice(0, 5))} className="font-mono uppercase" placeholder="ABC" /></div>
            <Button className="w-full" onClick={() => onJoin(joinTag.trim())} disabled={joinTag.trim().length < 2}>Unisciti</Button>
          </TabsContent>
        </Tabs>
      </DialogContent>
    </Dialog>
  );
}

function DirectChatDialog({ userId, other, myName, onClose }: { userId: string; other: Profile; myName: string; onClose: () => void }) {
  const msgsQ = useDirectMessages(userId, other.id);
  const send = useSendDirectMessage(userId);
  const [text, setText] = useState("");
  const submit = async () => {
    if (!text.trim()) return;
    await send.mutateAsync({ to: other.id, content: text.trim() });
    setText("");
  };
  return (
    <Dialog open={true} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="border-border bg-card max-w-md flex flex-col h-[80vh]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2"><span className="text-2xl">{other.avatar}</span>{other.username}</DialogTitle>
        </DialogHeader>
        <ScrollArea className="flex-1 pr-3">
          <div className="space-y-2 py-2">
            {(msgsQ.data ?? []).map((m) => {
              const mine = m.sender_id === userId;
              return (
                <div key={m.id} className={`flex ${mine ? "justify-end" : "justify-start"}`}>
                  <div className={`max-w-[75%] rounded-2xl px-3 py-2 text-sm ${mine ? "bg-primary text-primary-foreground" : "bg-card border border-border"}`}>
                    {m.content}
                  </div>
                </div>
              );
            })}
            {(msgsQ.data ?? []).length === 0 && (
              <p className="text-center text-xs text-muted-foreground py-8">Inizia la conversazione, {myName}!</p>
            )}
          </div>
        </ScrollArea>
        <div className="flex gap-2 pt-2 border-t border-border">
          <Input value={text} onChange={(e) => setText(e.target.value)} onKeyDown={(e) => e.key === "Enter" && submit()} placeholder="Scrivi un messaggio…" maxLength={500} />
          <Button onClick={submit} disabled={!text.trim()}><Send className="h-4 w-4" /></Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}

function ClanChatDialog({ clanId, userId, clanName, members, onClose }: { clanId: string; userId: string; clanName: string; members: Profile[]; onClose: () => void }) {
  const msgsQ = useClanMessages(clanId);
  const send = useSendClanMessage(userId, clanId);
  const [text, setText] = useState("");
  const memberMap = useMemo(() => new Map(members.map((m) => [m.id, m])), [members]);
  const submit = async () => {
    if (!text.trim()) return;
    await send.mutateAsync(text.trim());
    setText("");
  };
  return (
    <Dialog open={true} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="border-border bg-card max-w-md flex flex-col h-[80vh]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2"><Shield className="h-5 w-5" /> {clanName}</DialogTitle>
          <DialogDescription>Gli annunci automatici arrivano quando qualcuno vince una medaglia epica o leggendaria.</DialogDescription>
        </DialogHeader>
        <ScrollArea className="flex-1 pr-3">
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
                  <div className={`max-w-[75%] rounded-2xl px-3 py-2 text-sm ${mine ? "bg-primary text-primary-foreground" : "bg-card border border-border"}`}>
                    {!mine && sender && <p className="text-[10px] font-semibold opacity-70 mb-0.5">{sender.username}</p>}
                    {m.content}
                  </div>
                </div>
              );
            })}
            {(msgsQ.data ?? []).length === 0 && (
              <p className="text-center text-xs text-muted-foreground py-8">Nessun messaggio ancora. Rompi il ghiaccio!</p>
            )}
          </div>
        </ScrollArea>
        <div className="flex gap-2 pt-2 border-t border-border">
          <Input value={text} onChange={(e) => setText(e.target.value)} onKeyDown={(e) => e.key === "Enter" && submit()} placeholder="Messaggio al clan…" maxLength={500} />
          <Button onClick={submit} disabled={!text.trim()}><Send className="h-4 w-4" /></Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}

function FallingDrops({ seed }: { seed: number }) {
  const drops = useMemo(() => {
    const rnd = (i: number) => { const x = Math.sin(seed + i * 999) * 10000; return x - Math.floor(x); };
    return Array.from({ length: 18 }, (_, i) => ({
      left: rnd(i) * 100,
      delay: rnd(i + 100) * 0.4,
      duration: 1.1 + rnd(i + 200) * 0.9,
      size: 8 + rnd(i + 300) * 12,
      drift: (rnd(i + 400) - 0.5) * 40,
    }));
  }, [seed]);
  return (
    <>
      {drops.map((d, i) => (
        <motion.span key={i} initial={{ y: -60, x: 0, opacity: 0, scale: 0.6 }} animate={{ y: "110vh", x: d.drift, opacity: [0, 1, 1, 0], scale: 1 }}
          transition={{ duration: d.duration, delay: d.delay, ease: "easeIn" }}
          className="absolute top-0 block" style={{ left: `${d.left}%`, width: d.size, height: d.size * 1.4 }}>
          <svg viewBox="0 0 20 28" className="h-full w-full drop-shadow-glow">
            <path d="M10 1 C 10 8, 18 14, 18 20 A 8 8 0 0 1 2 20 C 2 14, 10 8, 10 1 Z" fill="white" opacity="0.95" />
          </svg>
        </motion.span>
      ))}
    </>
  );
}
