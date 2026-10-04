import { useEffect, useMemo, useRef, useState } from "react";
import { motion, AnimatePresence } from "motion/react";
import { Check, Dumbbell, Droplets, X, Plus, Target, Moon, ChevronRight, ChevronLeft, Bot, Maximize2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Slider } from "@/components/ui/slider";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import {
  computeLeague, startLimitFrom, LEAGUE_LABEL, ZERO_THEMES, type DayPlan, type ZeroAnswers, type League,
} from "@/lib/sbo-zero";
import type { AgentMessage, WorkoutDay } from "@/lib/sbo-zero-cloud";

// ---------------- Theme applier ----------------
export function useZeroTheme(theme: string | null) {
  useEffect(() => {
    const el = document.documentElement;
    if (theme) el.dataset.pzTheme = theme;
    else delete el.dataset.pzTheme;
    return () => { delete el.dataset.pzTheme; };
  }, [theme]);
}

// ---------------- Questionnaire ----------------
type Q = { key: keyof ZeroAnswers; title: string; hint: string; min: number; max: number; step: number; unit: string; def: number };
const QUESTIONS: Q[] = [
  { key: "age", title: "Quanti anni hai?", hint: "Serve per calibrare i valori di riferimento.", min: 12, max: 60, step: 1, unit: "anni", def: 17 },
  { key: "pushups", title: "Quante flessioni fai di fila?", hint: "Fatte bene, petto quasi a terra. Media 15-20 anni: 15-35.", min: 0, max: 100, step: 1, unit: "flessioni", def: 15 },
  { key: "plank", title: "Quanti secondi tieni il plank?", hint: "Corpo dritto sugli avambracci. Media 15-20 anni: 45-120s.", min: 0, max: 300, step: 5, unit: "secondi", def: 45 },
  { key: "pullups", title: "Quante trazioni fai di fila?", hint: "Mento sopra la sbarra. Media 15-20 anni: 3-10. Zero va benissimo.", min: 0, max: 40, step: 1, unit: "trazioni", def: 2 },
  { key: "squats", title: "Quanti squat fai di fila?", hint: "Squat a corpo libero, cosce parallele. Media: 30-60.", min: 0, max: 150, step: 5, unit: "squat", def: 30 },
  { key: "weeklyNow", title: "Quante volte a settimana lo fai adesso?", hint: "Sii onesto: serve a darti un limite iniziale realistico (max 5, ideale 3 o meno).", min: 0, max: 40, step: 1, unit: "volte/settimana", def: 5 },
];

export function ZeroQuestionnaire({ open, reassess, initial, initialTheme, onClose, onSubmit }: {
  open: boolean; reassess?: boolean; initial?: Partial<ZeroAnswers>; initialTheme?: string;
  onClose: () => void; onSubmit: (a: ZeroAnswers, theme: string, league: League, startLimit: number) => void;
}) {
  const [step, setStep] = useState(0);
  const [ans, setAns] = useState<ZeroAnswers>(() => ({
    age: 17, pushups: 15, plank: 45, pullups: 2, squats: 30, weeklyNow: 5, ...initial,
  }));
  const [theme, setTheme] = useState(initialTheme ?? "obsidian");
  useEffect(() => { if (open) setStep(0); }, [open]);
  const total = QUESTIONS.length + 3; // intro + questions + theme + result
  const league = computeLeague(ans);
  const limit = startLimitFrom(ans.weeklyNow);

  const body = () => {
    if (step === 0) {
      return (
        <div className="space-y-4">
          <div className="grid h-16 w-16 place-items-center rounded-2xl bg-primary/15 text-primary"><Target className="h-8 w-8" /></div>
          <h2 className="text-2xl font-bold">{reassess ? "Nuova valutazione" : "Progetto Zero"}</h2>
          {reassess ? (
            <p className="text-sm text-muted-foreground">È passato un mese. Rispondi con i tuoi numeri di oggi: il coach ricalcola la tua lega e il tuo piano.</p>
          ) : (
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li>• SBO diventa il tuo personal trainer: allenamenti ogni giorno, giorni di riposo, un coach che ti scrive.</li>
              <li>• Non vince chi lo fa di più: vince chi lo fa <b className="text-foreground">di meno</b>. Limite settimanale da 5 a 3, ideale sotto.</li>
              <li>• Dura <b className="text-foreground">almeno 7 giorni</b>: prima non puoi disattivarlo.</li>
              <li>• Se sfori il limite, il coach si incazza. Davvero.</li>
            </ul>
          )}
        </div>
      );
    }
    const qi = step - 1;
    if (qi < QUESTIONS.length) {
      const q = QUESTIONS[qi];
      const v = ans[q.key] as number;
      return (
        <div className="space-y-5">
          <p className="text-xs uppercase tracking-wider text-muted-foreground">Domanda {qi + 1} di {QUESTIONS.length}</p>
          <h2 className="text-xl font-bold">{q.title}</h2>
          <p className="text-sm text-muted-foreground">{q.hint}</p>
          <div className="flex items-end justify-center gap-2 py-2">
            <Input type="number" inputMode="numeric" value={v} min={q.min} max={q.max}
              onChange={(e) => setAns({ ...ans, [q.key]: Math.max(q.min, Math.min(q.max, Number(e.target.value) || 0)) })}
              className="h-16 w-32 text-center font-display text-4xl font-bold" />
            <span className="pb-3 text-sm text-muted-foreground">{q.unit}</span>
          </div>
          <Slider min={q.min} max={q.max} step={q.step} value={[v]} onValueChange={([n]) => setAns({ ...ans, [q.key]: n })} />
        </div>
      );
    }
    if (qi === QUESTIONS.length) {
      return (
        <div className="space-y-4">
          <h2 className="text-xl font-bold">Scegli il tuo tema</h2>
          <p className="text-sm text-muted-foreground">Con il Progetto Zero attivo l'app cambia faccia. Puoi cambiarlo quando vuoi dalle impostazioni.</p>
          <ThemePicker value={theme} onChange={setTheme} />
        </div>
      );
    }
    return (
      <div className="space-y-4 text-center">
        <p className="text-xs uppercase tracking-wider text-muted-foreground">Risultato</p>
        <motion.div initial={{ scale: 0.6, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} transition={{ type: "spring", stiffness: 200, damping: 14 }}
          className="mx-auto grid h-28 w-28 place-items-center rounded-full border-4 border-primary bg-primary/10">
          <span className="font-display text-lg font-bold text-primary">{LEAGUE_LABEL[league].replace("Lega ", "")}</span>
        </motion.div>
        <h2 className="text-2xl font-bold">{LEAGUE_LABEL[league]}</h2>
        <p className="text-sm text-muted-foreground">
          {league === "low" ? "Si parte dalle basi. Esercizi semplici per gli altri, una sfida vera per te." : league === "mid" ? "Sei nella media. Adesso si costruisce sopra." : "Sei sopra la media. Il piano non ti farà sconti."}
        </p>
        {!reassess && (
          <Card className="border-border bg-secondary/40 p-4 text-left">
            <p className="text-sm"><b>Limite iniziale:</b> {limit} a settimana</p>
            <p className="mt-1 text-xs text-muted-foreground">{limit > 3 ? `Scende di uno ogni due settimane fino a 3.` : "Il limite ideale. Stare sotto è ancora meglio."}</p>
          </Card>
        )}
      </div>
    );
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="border-border bg-card sm:max-w-md">
        <DialogHeader className="sr-only">
          <DialogTitle>Questionario Progetto Zero</DialogTitle>
          <DialogDescription>Calcola la tua lega di allenamento</DialogDescription>
        </DialogHeader>
        <div className="h-1 w-full overflow-hidden rounded-full bg-muted">
          <motion.div className="h-full bg-primary" animate={{ width: `${((step + 1) / total) * 100}%` }} />
        </div>
        <AnimatePresence mode="wait">
          <motion.div key={step} initial={{ x: 30, opacity: 0 }} animate={{ x: 0, opacity: 1 }} exit={{ x: -30, opacity: 0 }} transition={{ duration: 0.2 }} className="min-h-[280px] py-2">
            {body()}
          </motion.div>
        </AnimatePresence>
        <DialogFooter className="flex-row gap-2">
          {step > 0 && <Button variant="secondary" onClick={() => setStep(step - 1)}><ChevronLeft className="h-4 w-4" /></Button>}
          {step < total - 1 ? (
            <Button className="flex-1" onClick={() => setStep(step + 1)}>{step === 0 ? "Iniziamo" : "Avanti"} <ChevronRight className="ml-1 h-4 w-4" /></Button>
          ) : (
            <Button className="flex-1" onClick={() => onSubmit(ans, theme, league, limit)}>{reassess ? "Aggiorna il piano" : "Attiva il Progetto Zero"}</Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export function ThemePicker({ value, onChange }: { value: string; onChange: (t: string) => void }) {
  return (
    <div className="grid grid-cols-2 gap-2">
      {ZERO_THEMES.map((t) => (
        <button key={t.id} onClick={() => onChange(t.id)}
          className={`rounded-xl border p-3 text-left transition ${value === t.id ? "border-primary ring-2 ring-primary/40" : "border-border hover:border-primary/40"}`}>
          <div className="flex h-8 overflow-hidden rounded-md">
            {t.colors.map((c) => <span key={c} className="flex-1" style={{ background: c }} />)}
          </div>
          <p className="mt-2 text-sm font-medium">{t.name}</p>
        </button>
      ))}
    </div>
  );
}

// ---------------- Banner ----------------
export function ZeroBanner({ day, league, weekCount, limit, workoutsWeek }: { day: number; league: League; weekCount: number; limit: number; workoutsWeek: number }) {
  const status = weekCount > limit ? "over" : weekCount === limit ? "limit" : "ok";
  return (
    <motion.div initial={{ y: -10, opacity: 0 }} animate={{ y: 0, opacity: 1 }}
      className="relative overflow-hidden rounded-2xl border border-primary/30 bg-card/70 p-4 backdrop-blur">
      <div className="pointer-events-none absolute -right-10 -top-10 h-32 w-32 rounded-full bg-primary/20 blur-2xl" />
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.2em] text-primary">
            <span className="relative flex h-2 w-2"><span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-primary opacity-60" /><span className="relative inline-flex h-2 w-2 rounded-full bg-primary" /></span>
            Progetto Zero in corso
          </p>
          <p className="mt-1 font-display text-2xl font-bold">Giorno {day}</p>
          <p className="text-xs text-muted-foreground">{LEAGUE_LABEL[league]} · {workoutsWeek} allenamenti questa settimana</p>
        </div>
        <div className="text-right">
          <p className="text-[10px] uppercase tracking-wider text-muted-foreground">Settimana</p>
          <p className="font-display text-3xl font-bold" style={{ color: `var(--zero-${status})` }}>{weekCount}<span className="text-base text-muted-foreground">/{limit}</span></p>
          <div className="mt-1 flex justify-end gap-1">
            {Array.from({ length: Math.max(limit, weekCount) }, (_, i) => (
              <span key={i} className="h-1.5 w-3 rounded-full" style={{ background: i < weekCount ? `var(--zero-${i >= limit ? "over" : status})` : "var(--muted)" }} />
            ))}
          </div>
        </div>
      </div>
    </motion.div>
  );
}

// ---------------- Two buttons ----------------
export function ZeroButtons({ weekCount, limit, workoutDone, rest, onSbo, onWorkout }: {
  weekCount: number; limit: number; workoutDone: boolean; rest: boolean; onSbo: () => void; onWorkout: () => void;
}) {
  const status = weekCount > limit ? "over" : weekCount === limit ? "limit" : "ok";
  const label = status === "over" ? "Oltre il limite" : status === "limit" ? "Al limite" : `Ancora ${limit - weekCount} di margine`;
  const [hit, setHit] = useState(0);
  return (
    <div className="grid grid-cols-2 gap-4">
      <div className="flex flex-col items-center gap-2">
        <motion.button key={`sbo-${status}`} onClick={() => { setHit((h) => h + 1); onSbo(); }}
          whileTap={{ scale: 0.88 }}
          animate={status === "over" ? { x: [0, -4, 4, -3, 3, 0] } : {}}
          transition={status === "over" ? { duration: 0.5, repeat: Infinity, repeatDelay: 2.5 } : { type: "spring", stiffness: 400, damping: 15 }}
          className="relative grid aspect-square w-full max-w-[160px] place-items-center overflow-hidden rounded-full"
          style={{
            background: `radial-gradient(circle at 35% 30%, color-mix(in oklch, var(--zero-${status}) 70%, white 30%), var(--zero-${status}) 55%, color-mix(in oklch, var(--zero-${status}) 50%, black 50%))`,
            boxShadow: `0 20px 60px -15px var(--zero-${status}), 0 0 ${status === "over" ? 80 : 40}px -10px var(--zero-${status})`,
          }}
          aria-label="Aggiungi evento SBO">
          {status === "over" && <motion.span className="absolute inset-0 rounded-full border-4" style={{ borderColor: "var(--zero-over)" }} animate={{ scale: [1, 1.25], opacity: [0.8, 0] }} transition={{ duration: 1.2, repeat: Infinity }} />}
          <AnimatePresence>
            {hit > 0 && <motion.span key={hit} className="absolute inset-0 rounded-full bg-white/40" initial={{ scale: 0.2, opacity: 0.8 }} animate={{ scale: 1.4, opacity: 0 }} transition={{ duration: 0.6 }} />}
          </AnimatePresence>
          <div className="relative flex flex-col items-center text-white drop-shadow-[0_2px_6px_rgba(0,0,0,0.5)]">
            <Droplets className="h-10 w-10" strokeWidth={2.2} />
            <span className="mt-1 font-display text-2xl font-bold">{weekCount}/{limit}</span>
          </div>
        </motion.button>
        <p className="text-center text-xs font-medium" style={{ color: `var(--zero-${status})` }}>{label}</p>
      </div>
      <div className="flex flex-col items-center gap-2">
        <motion.button onClick={onWorkout} whileTap={{ scale: 0.88 }} whileHover={{ scale: 1.03 }}
          className="relative grid aspect-square w-full max-w-[160px] place-items-center overflow-hidden rounded-full border-2 border-primary/50"
          style={{
            background: workoutDone
              ? "radial-gradient(circle at 35% 30%, color-mix(in oklch, var(--zero-ok) 65%, white 35%), var(--zero-ok) 60%, color-mix(in oklch, var(--zero-ok) 45%, black 55%))"
              : "radial-gradient(circle at 35% 30%, color-mix(in oklch, var(--primary) 35%, var(--card) 65%), var(--card) 75%)",
            boxShadow: "var(--shadow-soft)",
          }}
          aria-label="Allenamento">
          {!workoutDone && !rest && <motion.span className="absolute inset-2 rounded-full border-2 border-dashed border-primary/60" animate={{ rotate: 360 }} transition={{ duration: 18, repeat: Infinity, ease: "linear" }} />}
          <div className={`relative flex flex-col items-center ${workoutDone ? "text-white" : "text-primary"}`}>
            {workoutDone ? <Check className="h-10 w-10" strokeWidth={3} /> : rest ? <Moon className="h-10 w-10" /> : <Dumbbell className="h-10 w-10" />}
            <span className="mt-1 text-xs font-bold uppercase tracking-wider">{workoutDone ? "Fatto" : rest ? "Riposo" : "Allenati"}</span>
          </div>
        </motion.button>
        <p className="text-center text-xs text-muted-foreground">{workoutDone ? "Allenamento completato" : rest ? "Tocca per allenamento libero" : "Apri la scheda di oggi"}</p>
      </div>
    </div>
  );
}

// ---------------- Today's plan ----------------
function PlanList({ plan, wd, onToggle }: { plan: DayPlan; wd: WorkoutDay | undefined; onToggle: (id: string) => void }) {
  const done = new Set(wd?.done_ids ?? []);
  if (plan.rest) {
    return (
      <div className="flex items-start gap-3 rounded-xl bg-secondary/40 p-4">
        <Moon className="mt-0.5 h-5 w-5 shrink-0 text-primary" />
        <p className="text-sm text-muted-foreground">{plan.restTip}</p>
      </div>
    );
  }
  return (
    <ul className="space-y-2">
      {plan.exercises.map((e) => {
        const ok = done.has(e.id);
        return (
          <li key={e.id}>
            <button onClick={() => onToggle(e.id)}
              className={`flex w-full items-center gap-3 rounded-xl border p-3 text-left transition ${ok ? "border-transparent bg-[var(--zero-ok)]/15" : "border-border bg-secondary/30 hover:border-primary/40"}`}>
              <span className={`grid h-7 w-7 shrink-0 place-items-center rounded-full border-2 transition ${ok ? "border-[var(--zero-ok)] bg-[var(--zero-ok)] text-white" : "border-muted-foreground/40"}`}>
                {ok && <Check className="h-4 w-4" strokeWidth={3} />}
              </span>
              <span className="min-w-0 flex-1">
                <span className={`block text-sm font-semibold ${ok ? "line-through opacity-60" : ""}`}>{e.name}</span>
                <span className="block truncate text-[11px] text-muted-foreground">{e.tip}</span>
              </span>
              <span className="shrink-0 text-right font-mono text-sm font-bold">{e.sets}×{e.amount}<span className="text-[10px] font-normal text-muted-foreground">{e.unit === "sec" ? "s" : ""}</span></span>
            </button>
          </li>
        );
      })}
    </ul>
  );
}

export function TodayPlanCard({ plan, wd, onToggle, onCustom, onExpand }: {
  plan: DayPlan; wd: WorkoutDay | undefined; onToggle: (id: string) => void; onCustom: () => void; onExpand: () => void;
}) {
  const doneN = plan.exercises.filter((e) => wd?.done_ids.includes(e.id)).length;
  return (
    <Card className="border-primary/25 bg-card/70 p-4 backdrop-blur">
      <div className="mb-3 flex items-center justify-between">
        <div>
          <p className="text-[10px] font-semibold uppercase tracking-[0.2em] text-primary">Obiettivi di oggi</p>
          <p className="font-display text-lg font-bold">{plan.title}</p>
        </div>
        <div className="flex items-center gap-1">
          {!plan.rest && <span className="mr-1 font-mono text-xs text-muted-foreground">{doneN}/{plan.exercises.length}</span>}
          <Button size="icon" variant="ghost" className="h-8 w-8" onClick={onExpand} aria-label="Apri a schermo intero"><Maximize2 className="h-4 w-4" /></Button>
        </div>
      </div>
      <PlanList plan={plan} wd={wd} onToggle={onToggle} />
      {(wd?.custom?.length ?? 0) > 0 && (
        <div className="mt-3 space-y-1">
          {wd!.custom.map((c, i) => (
            <p key={i} className="flex items-center gap-2 text-xs text-muted-foreground"><Check className="h-3.5 w-3.5" style={{ color: "var(--zero-ok)" }} /> {c.name}{c.note ? ` — ${c.note}` : ""}</p>
          ))}
        </div>
      )}
      <Button variant="ghost" size="sm" className="mt-3 w-full text-muted-foreground" onClick={onCustom}>
        <Plus className="mr-1.5 h-3.5 w-3.5" /> Aggiungi un allenamento tuo
      </Button>
    </Card>
  );
}

export function PlanOverlay({ open, plan, wd, name, day, onToggle, onCustom, onClose }: {
  open: boolean; plan: DayPlan; wd: WorkoutDay | undefined; name: string; day: number;
  onToggle: (id: string) => void; onCustom: () => void; onClose: () => void;
}) {
  const doneN = plan.exercises.filter((e) => wd?.done_ids.includes(e.id)).length;
  const pct = plan.rest ? 100 : Math.round((doneN / Math.max(1, plan.exercises.length)) * 100);
  return (
    <AnimatePresence>
      {open && (
        <motion.div className="fixed inset-0 z-50 overflow-y-auto bg-background/95 backdrop-blur-xl"
          initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
          <motion.div initial={{ y: 40, scale: 0.97 }} animate={{ y: 0, scale: 1 }} exit={{ y: 40, scale: 0.97 }} transition={{ type: "spring", stiffness: 220, damping: 24 }}
            className="mx-auto max-w-lg px-6 pb-16 pt-10">
            <div className="flex items-start justify-between">
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-[0.25em] text-primary">Progetto Zero · Giorno {day}</p>
                <h2 className="mt-2 font-display text-4xl font-bold leading-none">{plan.rest ? "Oggi riposi" : plan.title}</h2>
                <p className="mt-2 text-sm text-muted-foreground">{plan.rest ? `${name}, il recupero fa parte del piano.` : `${name}, ecco la tua scheda. Spunta ogni esercizio quando lo finisci.`}</p>
              </div>
              <Button size="icon" variant="secondary" className="rounded-full" onClick={onClose} aria-label="Chiudi"><X className="h-5 w-5" /></Button>
            </div>
            <div className="my-6 flex items-center gap-4">
              <div className="relative h-20 w-20">
                <svg viewBox="0 0 36 36" className="h-20 w-20 -rotate-90">
                  <circle cx="18" cy="18" r="15.5" fill="none" stroke="var(--muted)" strokeWidth="3" />
                  <motion.circle cx="18" cy="18" r="15.5" fill="none" stroke="var(--primary)" strokeWidth="3" strokeLinecap="round"
                    strokeDasharray="97.4" animate={{ strokeDashoffset: 97.4 - (97.4 * pct) / 100 }} transition={{ duration: 0.6 }} />
                </svg>
                <span className="absolute inset-0 grid place-items-center font-display text-lg font-bold">{pct}%</span>
              </div>
              <p className="text-sm text-muted-foreground">{plan.rest ? "Niente da spuntare oggi. Se vuoi comunque muoverti, aggiungi un allenamento libero." : pct === 100 ? "Fatto tutto. Il coach è fiero di te." : `${doneN} di ${plan.exercises.length} esercizi completati.`}</p>
            </div>
            <PlanList plan={plan} wd={wd} onToggle={onToggle} />
            <Button variant="secondary" className="mt-4 w-full" onClick={onCustom}><Plus className="mr-1.5 h-4 w-4" /> Aggiungi un allenamento tuo</Button>
            <Button className="mt-2 w-full" onClick={onClose}>Vai alla home</Button>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

export function CustomWorkoutDialog({ open, onClose, onSave }: { open: boolean; onClose: () => void; onSave: (name: string, note: string) => void }) {
  const [name, setName] = useState("");
  const [note, setNote] = useState("");
  const presets = ["Palestra", "Flessioni", "Corsa", "Calcio", "Nuoto", "Bici", "Pesi", "Arti marziali"];
  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="border-border bg-card">
        <DialogHeader>
          <DialogTitle>Allenamento libero</DialogTitle>
          <DialogDescription>Ti sei allenato per conto tuo? Vale esattamente come quello del piano.</DialogDescription>
        </DialogHeader>
        <div className="flex flex-wrap gap-2">
          {presets.map((p) => (
            <button key={p} onClick={() => setName(p)} className={`rounded-full border px-3 py-1 text-xs ${name === p ? "border-primary bg-primary/15 text-primary" : "border-border"}`}>{p}</button>
          ))}
        </div>
        <div className="space-y-2">
          <Label>Cosa hai fatto</Label>
          <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="Es. 100 flessioni in palestra" maxLength={60} />
          <Label>Note (facoltative)</Label>
          <Input value={note} onChange={(e) => setNote(e.target.value)} placeholder="Durata, serie, sensazioni…" maxLength={120} />
        </div>
        <DialogFooter>
          <Button className="w-full" disabled={!name.trim()} onClick={() => { onSave(name.trim(), note.trim()); setName(""); setNote(""); }}>Salva allenamento</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

// ---------------- Agent chat ----------------
export function AgentRow({ messages, onOpen }: { messages: AgentMessage[]; onOpen: () => void }) {
  const last = messages[messages.length - 1];
  const unread = messages.filter((m) => !m.read_at).length;
  return (
    <button onClick={onOpen} className="flex w-full items-center gap-3 rounded-2xl border border-primary/30 bg-card/70 p-3 text-left backdrop-blur transition hover:border-primary/60">
      <span className="relative grid h-11 w-11 shrink-0 place-items-center rounded-full bg-primary text-primary-foreground">
        <Bot className="h-5 w-5" />
        <span className="absolute -bottom-0.5 -right-0.5 h-3 w-3 rounded-full border-2 border-card" style={{ background: "var(--zero-ok)" }} />
      </span>
      <span className="min-w-0 flex-1">
        <span className="flex items-center gap-2 text-sm font-semibold">Coach Zero <span className="rounded-full bg-primary/15 px-1.5 text-[9px] uppercase tracking-wider text-primary">Trainer</span></span>
        <span className="block truncate text-xs text-muted-foreground">{last ? last.content : "Ti scriverà presto."}</span>
      </span>
      {unread > 0 && <span className="grid h-5 min-w-5 place-items-center rounded-full bg-destructive px-1.5 text-[10px] font-bold text-destructive-foreground">{unread}</span>}
    </button>
  );
}

export function AgentChatDialog({ messages, onClose }: { messages: AgentMessage[]; onClose: () => void }) {
  const end = useRef<HTMLDivElement>(null);
  useEffect(() => { end.current?.scrollIntoView({ behavior: "smooth" }); }, [messages.length]);
  const grouped = useMemo(() => {
    const out: { label: string; items: AgentMessage[] }[] = [];
    for (const m of messages) {
      const label = new Date(m.created_at).toLocaleDateString("it-IT", { weekday: "long", day: "numeric", month: "long" });
      if (!out.length || out[out.length - 1].label !== label) out.push({ label, items: [] });
      out[out.length - 1].items.push(m);
    }
    return out;
  }, [messages]);
  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="flex h-[85vh] flex-col border-border bg-card p-0 sm:max-w-md">
        <DialogHeader className="flex-row items-center gap-3 border-b border-border p-4 text-left">
          <span className="grid h-10 w-10 place-items-center rounded-full bg-primary text-primary-foreground"><Bot className="h-5 w-5" /></span>
          <div>
            <DialogTitle>Coach Zero</DialogTitle>
            <DialogDescription className="text-xs">Il tuo personal trainer. Ti scrive lui.</DialogDescription>
          </div>
        </DialogHeader>
        <div className="flex-1 space-y-4 overflow-y-auto p-4">
          {messages.length === 0 && <p className="py-10 text-center text-sm text-muted-foreground">Nessun messaggio ancora.</p>}
          {grouped.map((g) => (
            <div key={g.label} className="space-y-2">
              <p className="text-center text-[10px] uppercase tracking-wider text-muted-foreground">{g.label}</p>
              {g.items.map((m) => {
                const angry = m.kind === "rant" || m.kind === "quit";
                return (
                  <motion.div key={m.id} initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }}
                    className={`max-w-[85%] rounded-2xl rounded-tl-sm px-3.5 py-2 text-sm ${angry ? "border border-destructive/50 bg-destructive/15 font-semibold" : m.kind === "tip" ? "border border-primary/30 bg-primary/10" : "bg-secondary"}`}>
                    <p className="whitespace-pre-wrap">{m.content}</p>
                    <p className="mt-1 text-right text-[10px] text-muted-foreground">{new Date(m.created_at).toLocaleTimeString("it-IT", { hour: "2-digit", minute: "2-digit" })}</p>
                  </motion.div>
                );
              })}
            </div>
          ))}
          <div ref={end} />
        </div>
      </DialogContent>
    </Dialog>
  );
}
