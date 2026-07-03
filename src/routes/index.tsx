import { createFileRoute } from "@tanstack/react-router";
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  Plus,
  Droplets,
  Flame,
  CalendarDays,
  Users,
  Undo2,
  Sparkles,
  TrendingUp,
  Settings,
  Copy,
  UserPlus,
  Shield,
  X,
} from "lucide-react";
import logo from "@/assets/sbo-logo.png";
import {
  useSbo,
  countToday,
  countThisMonth,
  computeStreak,
  eventsByDay,
  adaptiveLimit,
  encourage,
  slowdown,
  restart,
  milestone,
  generateFriendCode,
  type Friend,
} from "@/lib/sbo-store";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "SBO — Il tuo contatore personale" },
      {
        name: "description",
        content:
          "SBO ti aiuta a monitorare, capire e migliorare le tue abitudini con calendario, streak, clan e incoraggiamenti personalizzati.",
      },
      { property: "og:title", content: "SBO — Il tuo contatore personale" },
      { property: "og:description", content: "Monitora, capisci, migliora. Con SBO ogni goccia conta." },
    ],
  }),
  component: SboApp,
});

function SboApp() {
  const { state, update, addEvent, undoLast, ready } = useSbo();
  const [showOnboard, setShowOnboard] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [showAddFriend, setShowAddFriend] = useState(false);
  const [showCreateClan, setShowCreateClan] = useState(false);
  const [nickInput, setNickInput] = useState("");
  const [friendCodeInput, setFriendCodeInput] = useState("");
  const [clanNameInput, setClanNameInput] = useState("");
  const [bursts, setBursts] = useState<number[]>([]);

  useEffect(() => {
    if (!ready) return;
    if (!state.onboarded) setShowOnboard(true);
    if (state.onboarded && !state.friendCode) {
      update({ friendCode: generateFriendCode() });
    }
  }, [ready, state.onboarded, state.friendCode, update]);

  const today = countToday(state.events);
  const month = countThisMonth(state.events);
  const streak = computeStreak(state.events);
  const limit = state.useAdaptiveLimit
    ? adaptiveLimit(state.events, state.dailyLimit)
    : state.dailyLimit;
  const byDay = useMemo(() => eventsByDay(state.events), [state.events]);

  // notifications on mount
  useEffect(() => {
    if (!ready || !state.onboarded || !state.notificationsEnabled) return;
    const name = state.nickname;
    const h = new Date().getHours();
    if (state.streakReminders && streak >= 2 && today === 0 && h >= 18) {
      setTimeout(() => toast(`🔥 Streak di ${streak} giorni!`, { description: encourage(name) }), 800);
    }
    if (state.encouragementReminders && streak === 0 && state.events.length > 0) {
      const last = state.events[state.events.length - 1].ts;
      if (Date.now() - last > 36 * 3600 * 1000) {
        setTimeout(() => toast("💧 Nuovo inizio", { description: restart(name) }), 1200);
      }
    }
    if (state.streakReminders && streak > 0 && [3, 7, 14, 30, 60, 100].includes(streak) && today === 0) {
      setTimeout(() => toast(milestone(name, streak)), 400);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ready]);

  const handleAdd = () => {
    addEvent();
    const id = Date.now();
    setBursts((b) => [...b, id]);
    setTimeout(() => setBursts((b) => b.filter((x) => x !== id)), 2200);

    const newToday = today + 1;
    const name = state.nickname;
    if (!state.notificationsEnabled) return;
    if (newToday >= limit) {
      toast("🌊 Fermati un attimo", { description: slowdown(name) });
    } else if (streak > 0) {
      toast("💧 Streak resettata", { description: restart(name) });
    } else {
      toast(encourage(name));
    }
  };

  const saveOnboard = () => {
    const n = nickInput.trim();
    if (!n) return;
    update({ nickname: n, onboarded: true, friendCode: generateFriendCode() });
    setShowOnboard(false);
    toast(`Benvenuto ${n} 💧`, { description: "La tua avventura SBO inizia adesso." });
  };

  const copyFriendCode = async () => {
    try {
      await navigator.clipboard.writeText(state.friendCode);
      toast("📋 Codice copiato!", { description: "Condividilo con chi vuoi aggiungere." });
    } catch {
      toast("Impossibile copiare");
    }
  };

  const addFriendByCode = () => {
    const code = friendCodeInput.trim().toUpperCase();
    if (code.length < 4) return toast("Codice non valido");
    if (code === state.friendCode) return toast("Non puoi aggiungere te stesso 😅");
    if (state.friends.some((f) => f.code === code)) return toast("Amico già aggiunto");
    const newFriend: Friend = {
      code,
      nickname: `amico_${code.slice(0, 3).toLowerCase()}`,
      streak: Math.floor(Math.random() * 20),
      avatar: ["🌊", "⚡", "🌟", "🔥", "🌙", "⚔️"][Math.floor(Math.random() * 6)],
    };
    update((s) => ({ ...s, friends: [...s.friends, newFriend] }));
    setFriendCodeInput("");
    setShowAddFriend(false);
    toast(`✅ ${newFriend.nickname} aggiunto!`);
  };

  const createClan = () => {
    const n = clanNameInput.trim();
    if (n.length < 3) return toast("Nome clan troppo corto");
    const tag = n.slice(0, 4).toUpperCase();
    update({
      clan: {
        name: n,
        tag,
        members: [
          { nickname: state.nickname, streak },
          { nickname: "lupo_solitario", streak: 12 },
          { nickname: "silent_flame", streak: 8 },
        ],
      },
    });
    setClanNameInput("");
    setShowCreateClan(false);
    toast(`🛡️ Clan [${tag}] ${n} creato!`);
  };

  const leaveClan = () => {
    update({ clan: null });
    toast("Hai lasciato il clan");
  };

  const totalClanStreak = state.clan
    ? state.clan.members.reduce((s, m) => s + (m.nickname === state.nickname ? streak : m.streak), 0)
    : 0;

  if (!ready) return null;

  return (
    <div className="min-h-screen text-foreground">
      <Toaster position="top-center" theme="dark" />

      {/* Falling white drops overlay */}
      <div className="pointer-events-none fixed inset-0 z-40 overflow-hidden">
        <AnimatePresence>
          {bursts.map((id) => (
            <FallingDrops key={id} seed={id} />
          ))}
        </AnimatePresence>
      </div>

      {/* Header */}
      <header className="mx-auto flex max-w-3xl items-center justify-between px-6 pt-8 pb-4">
        <div className="flex items-center gap-3">
          <img src={logo} alt="SBO logo" width={40} height={40} className="drop-shadow-glow" />
          <div>
            <h1 className="text-2xl font-bold tracking-tight">SBO</h1>
            <p className="text-xs text-muted-foreground">Ciao {state.nickname || "amico"} 👋</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <div className="flex items-center gap-2 rounded-full border border-border bg-card/60 px-4 py-2 backdrop-blur">
            <Flame className="h-4 w-4 text-accent" />
            <span className="text-sm font-semibold">{streak}</span>
            <span className="text-xs text-muted-foreground">giorni</span>
          </div>
          <Button
            variant="ghost"
            size="icon"
            className="rounded-full border border-border bg-card/60 backdrop-blur"
            onClick={() => setShowSettings(true)}
            aria-label="Impostazioni"
          >
            <Settings className="h-4 w-4" />
          </Button>
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-6 pb-24">
        <Tabs defaultValue="home" className="w-full">
          <TabsList className="mx-auto grid w-full max-w-sm grid-cols-3 rounded-full bg-card/60 backdrop-blur">
            <TabsTrigger value="home" className="rounded-full">
              <Droplets className="mr-1.5 h-4 w-4" /> Home
            </TabsTrigger>
            <TabsTrigger value="calendar" className="rounded-full">
              <CalendarDays className="mr-1.5 h-4 w-4" /> Calendario
            </TabsTrigger>
            <TabsTrigger value="friends" className="rounded-full">
              <Users className="mr-1.5 h-4 w-4" /> Social
            </TabsTrigger>
          </TabsList>

          {/* HOME */}
          <TabsContent value="home" className="mt-8 space-y-8">
            <div className="grid grid-cols-3 gap-3">
              <Stat label="Oggi" value={today} accent={today >= limit} />
              <Stat label="Questo mese" value={month} />
              <Stat
                label={state.useAdaptiveLimit ? "Limite adattivo" : "Limite"}
                value={limit}
                icon={state.useAdaptiveLimit ? <TrendingUp className="h-3.5 w-3.5" /> : undefined}
              />
            </div>

            <div className="relative flex flex-col items-center justify-center py-10">
              <motion.button
                onClick={handleAdd}
                whileTap={{ scale: 0.9 }}
                whileHover={{ scale: 1.04 }}
                transition={{ type: "spring", stiffness: 400, damping: 15 }}
                className="group relative flex h-48 w-48 items-center justify-center rounded-full"
                style={{ background: "var(--gradient-drop)", boxShadow: "var(--shadow-drop)" }}
                aria-label="Aggiungi 1 al contatore"
              >
                <div className="absolute inset-2 rounded-full bg-gradient-to-br from-white/40 to-transparent" />
                <Plus className="relative h-16 w-16 text-primary-foreground" strokeWidth={2.5} />
              </motion.button>

              <p className="mt-8 text-center text-sm text-muted-foreground">
                Premi per aggiungere. Tutto rimane privato sul tuo dispositivo.
              </p>

              {state.events.length > 0 && (
                <Button variant="ghost" size="sm" className="mt-2 text-muted-foreground" onClick={undoLast}>
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
                    {today >= limit
                      ? slowdown(state.nickname)
                      : streak >= 3
                        ? `Sei a ${streak} giorni. ${encourage(state.nickname)}`
                        : encourage(state.nickname)}
                  </p>
                </div>
              </div>
            </Card>
          </TabsContent>

          {/* CALENDAR */}
          <TabsContent value="calendar" className="mt-8">
            <Card className="border-border/50 bg-card/60 p-4 backdrop-blur">
              <p className="mb-3 px-2 text-sm text-muted-foreground">
                Registro personale — solo per uso medico e di monitoraggio.
              </p>
              <Calendar
                mode="single"
                className="pointer-events-auto mx-auto"
                modifiers={{
                  logged: (d) => (byDay.get(d.toISOString().slice(0, 10)) ?? 0) > 0,
                  heavy: (d) => (byDay.get(d.toISOString().slice(0, 10)) ?? 0) >= limit,
                }}
                modifiersClassNames={{
                  logged:
                    "relative after:absolute after:bottom-1 after:left-1/2 after:-translate-x-1/2 after:h-1 after:w-1 after:rounded-full after:bg-accent",
                  heavy:
                    "relative after:absolute after:bottom-1 after:left-1/2 after:-translate-x-1/2 after:h-1.5 after:w-1.5 after:rounded-full after:bg-destructive",
                }}
              />
              <div className="mt-4 flex justify-center gap-4 text-xs text-muted-foreground">
                <span className="flex items-center gap-1.5">
                  <span className="h-1.5 w-1.5 rounded-full bg-accent" /> attività
                </span>
                <span className="flex items-center gap-1.5">
                  <span className="h-2 w-2 rounded-full bg-destructive" /> sopra il limite
                </span>
              </div>
            </Card>
          </TabsContent>

          {/* SOCIAL: friends + clan */}
          <TabsContent value="friends" className="mt-8 space-y-6">
            {/* Your card + friend code */}
            <Card className="border-border/50 bg-card/60 p-5 backdrop-blur">
              <div className="flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-full bg-gradient-to-br from-primary to-accent text-2xl">
                  💧
                </div>
                <div className="flex-1">
                  <p className="text-sm text-muted-foreground">Tu, {state.nickname}</p>
                  <p className="text-lg font-bold">{streak} giorni di streak</p>
                </div>
                <Flame className="h-6 w-6 text-accent" />
              </div>
              <div className="mt-4 flex items-center gap-2 rounded-xl border border-dashed border-border bg-background/30 p-3">
                <div className="flex-1">
                  <p className="text-xs text-muted-foreground">Il tuo codice amico</p>
                  <p className="font-mono text-lg font-bold tracking-widest">{state.friendCode || "……"}</p>
                </div>
                <Button size="sm" variant="secondary" onClick={copyFriendCode}>
                  <Copy className="mr-1.5 h-3.5 w-3.5" /> Copia
                </Button>
              </div>
            </Card>

            {/* Clan */}
            <div>
              <div className="mb-2 flex items-center justify-between px-2">
                <p className="text-xs uppercase tracking-wider text-muted-foreground">Clan</p>
                {state.clan && (
                  <button
                    className="text-xs text-muted-foreground hover:text-destructive"
                    onClick={leaveClan}
                  >
                    <X className="mr-1 inline h-3 w-3" />
                    esci
                  </button>
                )}
              </div>
              {state.clan ? (
                <Card className="border-border/50 bg-card/60 p-5 backdrop-blur">
                  <div className="flex items-center gap-3">
                    <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-accent to-primary">
                      <Shield className="h-5 w-5 text-primary-foreground" />
                    </div>
                    <div className="flex-1">
                      <p className="text-xs text-muted-foreground">[{state.clan.tag}]</p>
                      <p className="text-lg font-bold">{state.clan.name}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-xs text-muted-foreground">Streak totale</p>
                      <p className="text-xl font-bold text-accent">{totalClanStreak}</p>
                    </div>
                  </div>
                  <div className="mt-4 space-y-1.5">
                    {state.clan.members.map((m) => (
                      <div
                        key={m.nickname}
                        className="flex items-center justify-between rounded-lg bg-background/30 px-3 py-2 text-sm"
                      >
                        <span className={m.nickname === state.nickname ? "font-semibold" : ""}>
                          {m.nickname === state.nickname ? `${m.nickname} (tu)` : m.nickname}
                        </span>
                        <span className="flex items-center gap-1 text-muted-foreground">
                          <Flame className="h-3 w-3 text-accent" />
                          {m.nickname === state.nickname ? streak : m.streak}
                        </span>
                      </div>
                    ))}
                  </div>
                </Card>
              ) : (
                <Card className="border-dashed border-border bg-transparent p-5 text-center">
                  <Shield className="mx-auto mb-2 h-8 w-8 text-muted-foreground" />
                  <p className="text-sm text-muted-foreground">
                    Crea un clan per gareggiare a streak totale con altri.
                  </p>
                  <Button
                    className="mt-3"
                    size="sm"
                    onClick={() => setShowCreateClan(true)}
                  >
                    Crea clan
                  </Button>
                </Card>
              )}
            </div>

            {/* Friends leaderboard */}
            <div>
              <div className="mb-2 flex items-center justify-between px-2">
                <p className="text-xs uppercase tracking-wider text-muted-foreground">Classifica amici</p>
                <Button size="sm" variant="secondary" onClick={() => setShowAddFriend(true)}>
                  <UserPlus className="mr-1.5 h-3.5 w-3.5" /> Aggiungi
                </Button>
              </div>
              <div className="space-y-2">
                {[
                  ...state.friends,
                  { code: state.friendCode, nickname: state.nickname || "tu", streak, avatar: "💧" },
                ]
                  .sort((a, b) => b.streak - a.streak)
                  .map((f, i) => (
                    <Card
                      key={f.code + i}
                      className="border-border/50 bg-card/60 p-4 backdrop-blur"
                    >
                      <div className="flex items-center gap-3">
                        <span className="w-6 text-center text-sm font-bold text-muted-foreground">
                          {i + 1}
                        </span>
                        <span className="text-2xl">{f.avatar}</span>
                        <div className="flex-1">
                          <p className="font-medium">
                            {f.nickname}
                            {f.code === state.friendCode ? " (tu)" : ""}
                          </p>
                          <p className="font-mono text-[10px] text-muted-foreground">{f.code}</p>
                        </div>
                        <span className="flex items-center gap-1 text-sm">
                          <Flame className="h-3.5 w-3.5 text-accent" /> {f.streak}
                        </span>
                      </div>
                    </Card>
                  ))}
              </div>
              <p className="mt-3 px-2 text-[11px] text-muted-foreground">
                Gli amici sono salvati localmente in questa demo. Attiva la sync online per amici reali cross-dispositivo.
              </p>
            </div>
          </TabsContent>
        </Tabs>
      </main>

      {/* ONBOARDING */}
      <Dialog open={showOnboard} onOpenChange={setShowOnboard}>
        <DialogContent className="border-border bg-card">
          <DialogHeader>
            <div className="mx-auto mb-2">
              <img src={logo} alt="SBO" width={64} height={64} className="drop-shadow-glow" />
            </div>
            <DialogTitle className="text-center text-2xl">Benvenuto in SBO</DialogTitle>
            <DialogDescription className="text-center">
              Scegli un nickname. Lo useremo per le tue notifiche personalizzate.
            </DialogDescription>
          </DialogHeader>
          <Input
            placeholder="il tuo nickname"
            value={nickInput}
            onChange={(e) => setNickInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && saveOnboard()}
            className="text-center"
            maxLength={20}
            autoFocus
          />
          <DialogFooter>
            <Button onClick={saveOnboard} disabled={!nickInput.trim()} className="w-full">
              Inizia 💧
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* SETTINGS */}
      <Dialog open={showSettings} onOpenChange={setShowSettings}>
        <DialogContent className="border-border bg-card">
          <DialogHeader>
            <DialogTitle>Impostazioni</DialogTitle>
            <DialogDescription>Personalizza SBO come vuoi tu.</DialogDescription>
          </DialogHeader>

          <div className="space-y-5 py-2">
            <div>
              <Label className="text-xs uppercase tracking-wider text-muted-foreground">Nickname</Label>
              <Input
                className="mt-1.5"
                value={state.nickname}
                onChange={(e) => update({ nickname: e.target.value })}
                maxLength={20}
              />
            </div>

            <div className="flex items-center justify-between">
              <div>
                <Label>Limite adattivo</Label>
                <p className="text-xs text-muted-foreground">Basato sulle tue ultime 2 settimane</p>
              </div>
              <Switch
                checked={state.useAdaptiveLimit}
                onCheckedChange={(v) => update({ useAdaptiveLimit: v })}
              />
            </div>

            {!state.useAdaptiveLimit && (
              <div>
                <div className="mb-2 flex items-center justify-between">
                  <Label>Limite giornaliero</Label>
                  <span className="font-mono text-sm font-semibold">{state.dailyLimit}</span>
                </div>
                <Slider
                  min={1}
                  max={10}
                  step={1}
                  value={[state.dailyLimit]}
                  onValueChange={([v]) => update({ dailyLimit: v })}
                />
              </div>
            )}

            <div className="space-y-3 rounded-lg border border-border p-3">
              <div className="flex items-center justify-between">
                <Label>Notifiche</Label>
                <Switch
                  checked={state.notificationsEnabled}
                  onCheckedChange={(v) => update({ notificationsEnabled: v })}
                />
              </div>
              <div className="flex items-center justify-between pl-3">
                <Label className="text-sm text-muted-foreground">Promemoria streak</Label>
                <Switch
                  disabled={!state.notificationsEnabled}
                  checked={state.streakReminders}
                  onCheckedChange={(v) => update({ streakReminders: v })}
                />
              </div>
              <div className="flex items-center justify-between pl-3">
                <Label className="text-sm text-muted-foreground">Frasi di incoraggiamento</Label>
                <Switch
                  disabled={!state.notificationsEnabled}
                  checked={state.encouragementReminders}
                  onCheckedChange={(v) => update({ encouragementReminders: v })}
                />
              </div>
            </div>
          </div>

          <DialogFooter>
            <Button className="w-full" onClick={() => setShowSettings(false)}>
              Fatto
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ADD FRIEND */}
      <Dialog open={showAddFriend} onOpenChange={setShowAddFriend}>
        <DialogContent className="border-border bg-card">
          <DialogHeader>
            <DialogTitle>Aggiungi un amico</DialogTitle>
            <DialogDescription>Inserisci il codice a 6 caratteri del tuo amico.</DialogDescription>
          </DialogHeader>
          <Input
            className="text-center font-mono text-xl tracking-widest uppercase"
            placeholder="XXXXXX"
            maxLength={6}
            value={friendCodeInput}
            onChange={(e) => setFriendCodeInput(e.target.value.toUpperCase())}
            onKeyDown={(e) => e.key === "Enter" && addFriendByCode()}
          />
          <DialogFooter>
            <Button className="w-full" onClick={addFriendByCode}>
              Aggiungi
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* CREATE CLAN */}
      <Dialog open={showCreateClan} onOpenChange={setShowCreateClan}>
        <DialogContent className="border-border bg-card">
          <DialogHeader>
            <DialogTitle>Crea il tuo clan</DialogTitle>
            <DialogDescription>
              I clan competono a streak totale: chi somma più giorni vince.
            </DialogDescription>
          </DialogHeader>
          <Input
            placeholder="Nome del clan"
            maxLength={24}
            value={clanNameInput}
            onChange={(e) => setClanNameInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && createClan()}
          />
          <DialogFooter>
            <Button className="w-full" onClick={createClan}>
              Crea clan
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

function FallingDrops({ seed }: { seed: number }) {
  // 18 random drops falling from the top
  const drops = useMemo(() => {
    const rnd = (i: number) => {
      const x = Math.sin(seed + i * 999) * 10000;
      return x - Math.floor(x);
    };
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
        <motion.span
          key={i}
          initial={{ y: -60, x: 0, opacity: 0, scale: 0.6 }}
          animate={{ y: "110vh", x: d.drift, opacity: [0, 1, 1, 0], scale: 1 }}
          transition={{ duration: d.duration, delay: d.delay, ease: "easeIn" }}
          className="absolute top-0 block"
          style={{ left: `${d.left}%`, width: d.size, height: d.size * 1.4 }}
        >
          <svg viewBox="0 0 20 28" className="h-full w-full drop-shadow-glow">
            <path
              d="M10 1 C 10 8, 18 14, 18 20 A 8 8 0 0 1 2 20 C 2 14, 10 8, 10 1 Z"
              fill="white"
              opacity="0.95"
            />
          </svg>
        </motion.span>
      ))}
    </>
  );
}

function Stat({
  label,
  value,
  accent,
  icon,
}: {
  label: string;
  value: number;
  accent?: boolean;
  icon?: React.ReactNode;
}) {
  return (
    <Card
      className={`border-border/50 bg-card/60 p-4 backdrop-blur ${accent ? "ring-2 ring-destructive/50" : ""}`}
    >
      <p className="flex items-center gap-1 text-xs text-muted-foreground">
        {icon} {label}
      </p>
      <p className={`mt-1 text-3xl font-bold ${accent ? "text-destructive" : ""}`}>{value}</p>
    </Card>
  );
}
