CREATE TABLE public.project_zero (
  user_id uuid PRIMARY KEY,
  active boolean NOT NULL DEFAULT false,
  started_at timestamptz,
  league text NOT NULL DEFAULT 'low',
  start_limit int NOT NULL DEFAULT 3,
  theme text NOT NULL DEFAULT 'obsidian',
  answers jsonb NOT NULL DEFAULT '{}'::jsonb,
  last_assessment_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
GRANT SELECT, INSERT, UPDATE ON public.project_zero TO authenticated;
GRANT ALL ON public.project_zero TO service_role;
ALTER TABLE public.project_zero ENABLE ROW LEVEL SECURITY;
CREATE POLICY "pz own select" ON public.project_zero FOR SELECT TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "pz own insert" ON public.project_zero FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);
CREATE POLICY "pz own update" ON public.project_zero FOR UPDATE TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE OR REPLACE FUNCTION public.pz_guard() RETURNS trigger LANGUAGE plpgsql SET search_path = public AS $$
BEGIN
  NEW.updated_at = now();
  IF NEW.start_limit < 3 THEN NEW.start_limit = 3; END IF;
  IF NEW.start_limit > 5 THEN NEW.start_limit = 5; END IF;
  IF TG_OP = 'UPDATE' AND OLD.active AND NOT NEW.active
     AND OLD.started_at IS NOT NULL AND now() < OLD.started_at + interval '7 days' THEN
    RAISE EXCEPTION 'Il Progetto Zero dura almeno 7 giorni';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER pz_guard BEFORE INSERT OR UPDATE ON public.project_zero FOR EACH ROW EXECUTE FUNCTION public.pz_guard();

CREATE TABLE public.workout_days (
  user_id uuid NOT NULL,
  day text NOT NULL,
  done_ids text[] NOT NULL DEFAULT '{}',
  plan_size int NOT NULL DEFAULT 0,
  custom jsonb NOT NULL DEFAULT '[]'::jsonb,
  completed boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, day)
);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.workout_days TO authenticated;
GRANT ALL ON public.workout_days TO service_role;
ALTER TABLE public.workout_days ENABLE ROW LEVEL SECURITY;
CREATE POLICY "wd own all" ON public.workout_days FOR ALL TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE TABLE public.agent_messages (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL,
  content text NOT NULL,
  kind text NOT NULL DEFAULT 'daily',
  created_at timestamptz NOT NULL DEFAULT now(),
  read_at timestamptz
);
CREATE INDEX agent_messages_user_idx ON public.agent_messages (user_id, created_at DESC);
GRANT SELECT, UPDATE ON public.agent_messages TO authenticated;
GRANT ALL ON public.agent_messages TO service_role;
ALTER TABLE public.agent_messages ENABLE ROW LEVEL SECURITY;
CREATE POLICY "am own select" ON public.agent_messages FOR SELECT TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "am own update" ON public.agent_messages FOR UPDATE TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
ALTER PUBLICATION supabase_realtime ADD TABLE public.agent_messages;

CREATE TABLE public.agent_state (
  user_id uuid PRIMARY KEY,
  used_keys text[] NOT NULL DEFAULT '{}',
  flags jsonb NOT NULL DEFAULT '{}'::jsonb,
  updated_at timestamptz NOT NULL DEFAULT now()
);
GRANT ALL ON public.agent_state TO service_role;
ALTER TABLE public.agent_state ENABLE ROW LEVEL SECURITY;

CREATE TABLE public.ai_budget (
  id int PRIMARY KEY DEFAULT 1,
  day text NOT NULL DEFAULT '',
  calls int NOT NULL DEFAULT 0,
  paused_until timestamptz
);
GRANT ALL ON public.ai_budget TO service_role;
ALTER TABLE public.ai_budget ENABLE ROW LEVEL SECURITY;
INSERT INTO public.ai_budget (id) VALUES (1);