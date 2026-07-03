import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { motion, AnimatePresence } from "motion/react";
import { toast } from "sonner";
import { Toaster } from "@/components/ui/sonner";
import { Calendar } from "@/components/ui/calendar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog";
import { Plus, Droplets, Flame, CalendarDays, Users, Undo2, Sparkles, TrendingUp } from "lucide-react";
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
} from "@/lib/sbo-store";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "SBO — Il tuo contatore personale" },
      { name: "description", content: "SBO ti aiuta a monitorare, capire e migliorare le tue abitudini con calendario, streak e incoraggiamenti personalizzati." },
      { property: "og:title", content: "SBO — Il tuo contatore personale" },
      { property: "og:description", content: "Monitora, capisci, migliora. Con SBO ogni goccia conta." },
    ],
  }),
  component: SboApp,
});

function SboApp() {
  const { state, update, addEvent, undoLast, ready } = useSbo();
  const [showOnboard, setShowOnboard] = useState(false);
  const [nickInput, setNickInput] = useState("");
  const [ripples, setRipples] = useState<number[]>([]);
  const [drops, setDrops] = useState<number[]>([]);
  const [bigNumber, setBigNumber] = useState<{ id: number; value: number } | null>(null);

  useEffect(() => {
    if (ready && !state.onboarded) setShowOnboard(true);
  }, [ready, state.onboarded]);

  const today = countToday(state.events);
  const month = countThisMonth(state.events);
  const streak = computeStreak(state.events);
  const limit = adaptiveLimit(state.events, state.dailyLimit);
  const byDay = useMemo(() => eventsByDay(state.events), [state.events]);

  // Notifications: streak reminders — fire once per session at mount
  useEffect(() => {
    if (!ready || !state.onboarded) return;
    const name = state.nickname;
    // If it's past 20:00 and streak > 2 and no events today → encourage
    const h = new Date().getHours();
    if (streak >= 2 && today === 0 && h >= 18) {
      setTimeout(() => toast(`🔥 Streak di ${streak} giorni!`, { description: encourage(name) }), 800);
    }
    // If streak == 0 and last event > 24h ago → nudge to restart
    if (streak === 0 && state.events.length > 0) {
      const last = state.events[state.events.length - 1].ts;
      if (Date.now() - last > 36 * 3600 * 1000) {
        setTimeout(() => toast("💧 Nuovo inizio", { description: restart(name) }), 1200);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ready]);

  const handleAdd = () => {
    addEvent();
    const id = Date.now();
    setRipples((r) => [...r, id]);
    setDrops((d) => [...d, id, id + 1, id + 2]);
    setBigNumber({ id, value: today + 1 });
    setTimeout(() => setRipples((r) => r.filter((x) => x !== id)), 1200);
    setTimeout(() => setDrops((d) => d.filter((x) => x < id || x > id + 2)), 1500);
    setTimeout(() => setBigNumber(null), 900);

    const newToday = today + 1;
    const name = state.nickname;
    if (newToday >= limit) {
      toast("🌊 Fermati un attimo", { description: slowdown(name) });
    } else if (streak > 0) {
      // was on a streak, just broke it
      toast("💧 Streak resettata", { description: restart(name) });
    } else {
      toast(encourage(name));
    }
  };

  const saveOnboard = () => {
    const n = nickInput.trim();
    if (!n) return;
    update({ nickname: n, onboarded: true });
    setShowOnboard(false);
    toast(`Benvenuto ${n} 💧`, { description: "La tua avventura SBO inizia adesso." });
  };

  if (!ready) return null;

  return (
    <div className="min-h-screen text-foreground">
      <Toaster position="top-center" theme="dark" />

      {/* Header */}
      <header className="mx-auto flex max-w-3xl items-center justify-between px-6 pt-8 pb-4">
        <div className="flex items-center gap-3">
          <img src={logo} alt="SBO logo" width={40} height={40} className="drop-shadow-glow" />
          <div>
            <h1 className="text-2xl font-bold tracking-tight">SBO</h1>
            <p className="text-xs text-muted-foreground">Ciao {state.nickname || "amico"} 👋</p>
          </div>
        </div>
        <div className="flex items-center gap-2 rounded-full border border-border bg-card/60 px-4 py-2 backdrop-blur">
          <Flame className="h-4 w-4 text-accent" />
          <span className="text-sm font-semibold">{streak}</span>
          <span className="text-xs text-muted-foreground">giorni</span>
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
              <Users className="mr-1.5 h-4 w-4" /> Amici
            </TabsTrigger>
          </TabsList>

          {/* HOME */}
          <TabsContent value="home" className="mt-8 space-y-8">
            <div className="grid grid-cols-3 gap-3">
              <Stat label="Oggi" value={today} accent={today >= limit} />
              <Stat label="Questo mese" value={month} />
              <Stat label="Limite adattivo" value={limit} icon={<TrendingUp className="h-3.5 w-3.5" />} />
            </div>

            {/* Big button */}
            <div className="relative flex flex-col items-center justify-center py-10">
              {/* falling drops */}
              <AnimatePresence>
                {drops.map((id, i) => (
                  <motion.div
                    key={id}
                    initial={{ y: -60, x: (i % 3 - 1) * 30, opacity: 0, scale: 0.5 }}
                    animate={{ y: 200, opacity: [0, 1, 0], scale: 1 }}
                    exit={{ opacity: 0 }}
                    transition={{ duration: 1.2, ease: "easeIn", delay: i * 0.08 }}
                    className="pointer-events-none absolute top-0"
                  >
                    <div className="h-3 w-3 rounded-full bg-drop-glow blur-[1px]" />
                  </motion.div>
                ))}
              </AnimatePresence>

              {/* ripples */}
              {ripples.map((id) => (
                <span
                  key={id}
                  className="pointer-events-none absolute h-48 w-48 rounded-full border-2 border-accent/50"
                  style={{ animation: "ripple 1.2s ease-out forwards" }}
                />
              ))}

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
                <AnimatePresence>
                  {bigNumber && (
                    <motion.div
                      key={bigNumber.id}
                      initial={{ opacity: 0, y: 0, scale: 0.6 }}
                      animate={{ opacity: 1, y: -100, scale: 1.6 }}
                      exit={{ opacity: 0 }}
                      transition={{ duration: 0.9, ease: "easeOut" }}
                      className="pointer-events-none absolute text-5xl font-bold text-accent drop-shadow-glow"
                    >
                      +1
                    </motion.div>
                  )}
                </AnimatePresence>
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
                  logged: "relative after:absolute after:bottom-1 after:left-1/2 after:-translate-x-1/2 after:h-1 after:w-1 after:rounded-full after:bg-accent",
                  heavy: "relative after:absolute after:bottom-1 after:left-1/2 after:-translate-x-1/2 after:h-1.5 after:w-1.5 after:rounded-full after:bg-destructive",
                }}
              />
              <div className="mt-4 flex justify-center gap-4 text-xs text-muted-foreground">
                <span className="flex items-center gap-1.5"><span className="h-1.5 w-1.5 rounded-full bg-accent" /> attività</span>
                <span className="flex items-center gap-1.5"><span className="h-2 w-2 rounded-full bg-destructive" /> sopra il limite</span>
              </div>
            </Card>
          </TabsContent>

          {/* FRIENDS */}
          <TabsContent value="friends" className="mt-8 space-y-4">
            <Card className="border-border/50 bg-card/60 p-5 backdrop-blur">
              <div className="flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-full bg-gradient-to-br from-primary to-accent text-2xl">
                  💧
                </div>
                <div className="flex-1">
                  <p className="text-sm text-muted-foreground">Tu, {state.nickname || "amico"}</p>
                  <p className="text-lg font-bold">{streak} giorni di streak</p>
                </div>
                <Flame className="h-6 w-6 text-accent" />
              </div>
            </Card>

            <p className="px-2 text-xs uppercase tracking-wider text-muted-foreground">Classifica amici</p>
            <div className="space-y-2">
              {[...state.friends, { nickname: state.nickname || "tu", streak, avatar: "💧" }]
                .sort((a, b) => b.streak - a.streak)
                .map((f, i) => (
                  <Card key={i} className="border-border/50 bg-card/60 p-4 backdrop-blur">
                    <div className="flex items-center gap-3">
                      <span className="w-6 text-center text-sm font-bold text-muted-foreground">{i + 1}</span>
                      <span className="text-2xl">{f.avatar}</span>
                      <span className="flex-1 font-medium">{f.nickname}</span>
                      <span className="flex items-center gap-1 text-sm">
                        <Flame className="h-3.5 w-3.5 text-accent" /> {f.streak}
                      </span>
                    </div>
                  </Card>
                ))}
            </div>

            <Card className="border-dashed border-border bg-transparent p-4 text-center">
              <p className="text-sm text-muted-foreground">
                Amici e classifiche real-time arriveranno con la sync online.
              </p>
            </Card>
          </TabsContent>
        </Tabs>
      </main>

      {/* Onboarding */}
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
    </div>
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
    <Card className={`border-border/50 bg-card/60 p-4 backdrop-blur ${accent ? "ring-2 ring-destructive/50" : ""}`}>
      <p className="flex items-center gap-1 text-xs text-muted-foreground">
        {icon} {label}
      </p>
      <p className={`mt-1 text-3xl font-bold ${accent ? "text-destructive" : ""}`}>{value}</p>
    </Card>
  );
}
