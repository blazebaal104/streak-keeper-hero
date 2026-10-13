-- Project Zero: weekly limit editable from settings (0..5). limit_set_at marks a manual choice,
-- which then stays fixed instead of auto-decreasing.
ALTER TABLE public.project_zero ADD COLUMN IF NOT EXISTS limit_set_at timestamptz;

CREATE OR REPLACE FUNCTION public.pz_guard() RETURNS trigger LANGUAGE plpgsql SET search_path = public AS $$
BEGIN
  NEW.updated_at = now();
  IF NEW.start_limit < 0 THEN NEW.start_limit = 0; END IF;
  IF NEW.start_limit > 5 THEN NEW.start_limit = 5; END IF;
  IF TG_OP = 'UPDATE' AND OLD.active AND NOT NEW.active
     AND OLD.started_at IS NOT NULL AND now() < OLD.started_at + interval '7 days' THEN
    RAISE EXCEPTION 'Il Progetto Zero dura almeno 7 giorni';
  END IF;
  RETURN NEW;
END $$;
