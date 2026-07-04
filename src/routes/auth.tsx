import { createFileRoute, useNavigate, redirect } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { z } from "zod";
import { toast } from "sonner";
import { supabase } from "@/integrations/supabase/client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card } from "@/components/ui/card";
import { Toaster } from "@/components/ui/sonner";
import logo from "@/assets/sbo-logo.png";
import { Loader2 } from "lucide-react";

export const Route = createFileRoute("/auth")({
  head: () => ({
    meta: [
      { title: "SBO — Accedi o crea un account" },
      { name: "description", content: "Accedi a SBO o crea un nuovo account per iniziare a monitorare la tua streak, sbloccare medaglie e sfidare i tuoi amici." },
    ],
  }),
  ssr: false,
  beforeLoad: async () => {
    const { data } = await supabase.auth.getUser();
    if (data.user) throw redirect({ to: "/" });
  },
  component: AuthPage,
});

const signupSchema = z.object({
  username: z.string().trim().min(3, "Almeno 3 caratteri").max(20, "Massimo 20 caratteri").regex(/^[a-zA-Z0-9_]+$/, "Solo lettere, numeri e _"),
  email: z.string().trim().email("Email non valida").max(255),
  password: z.string().min(8, "Almeno 8 caratteri").max(72),
});

const loginSchema = z.object({
  email: z.string().trim().email("Email non valida"),
  password: z.string().min(1),
});

function AuthPage() {
  const nav = useNavigate();
  const [mode, setMode] = useState<"login" | "signup">("signup");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const { data: sub } = supabase.auth.onAuthStateChange((_e, session) => {
      if (session?.user) nav({ to: "/" });
    });
    return () => sub.subscription.unsubscribe();
  }, [nav]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      if (mode === "signup") {
        const parsed = signupSchema.safeParse({ username, email, password });
        if (!parsed.success) { toast.error(parsed.error.issues[0].message); return; }
        const { data: existing } = await supabase.from("profiles").select("id").eq("username", parsed.data.username.toLowerCase()).maybeSingle();
        if (existing) { toast.error("Username già preso, scegline un altro."); return; }
        const { error } = await supabase.auth.signUp({
          email: parsed.data.email,
          password: parsed.data.password,
          options: {
            emailRedirectTo: `${window.location.origin}/`,
            data: { username: parsed.data.username.toLowerCase() },
          },
        });
        if (error) { toast.error(error.message); return; }
        toast.success("Account creato! Benvenuto in SBO 💧");
      } else {
        const parsed = loginSchema.safeParse({ email, password });
        if (!parsed.success) { toast.error(parsed.error.issues[0].message); return; }
        const { error } = await supabase.auth.signInWithPassword({ email: parsed.data.email, password: parsed.data.password });
        if (error) { toast.error("Email o password errata"); return; }
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-10">
      <Toaster position="top-center" theme="dark" />
      <Card className="w-full max-w-md border-border/50 bg-card/60 backdrop-blur p-8">
        <div className="flex flex-col items-center mb-6">
          <img src={logo} alt="SBO" width={72} height={72} className="drop-shadow-glow" />
          <h1 className="mt-4 text-3xl font-bold tracking-tight">SBO</h1>
          <p className="mt-1 text-sm text-muted-foreground text-center">
            {mode === "signup" ? "Crea il tuo account" : "Bentornato"}
          </p>
        </div>

        <form onSubmit={submit} className="space-y-4">
          {mode === "signup" && (
            <div>
              <Label htmlFor="username">Username</Label>
              <Input id="username" value={username} onChange={(e) => setUsername(e.target.value)} placeholder="il_tuo_nome" autoComplete="username" maxLength={20} />
              <p className="mt-1 text-[11px] text-muted-foreground">Puoi modificarlo dopo.</p>
            </div>
          )}
          <div>
            <Label htmlFor="email">Email</Label>
            <Input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="tu@esempio.com" autoComplete="email" />
          </div>
          <div>
            <Label htmlFor="password">Password</Label>
            <Input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete={mode === "signup" ? "new-password" : "current-password"} minLength={8} />
          </div>

          <Button type="submit" disabled={loading} className="w-full h-11">
            {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {mode === "signup" ? "Crea account" : "Accedi"}
          </Button>
        </form>

        <p className="mt-5 text-center text-sm text-muted-foreground">
          {mode === "signup" ? "Hai già un account?" : "Nuovo su SBO?"}{" "}
          <button className="text-primary hover:underline" onClick={() => setMode(mode === "signup" ? "login" : "signup")}>
            {mode === "signup" ? "Accedi" : "Crea un account"}
          </button>
        </p>
      </Card>
    </div>
  );
}
