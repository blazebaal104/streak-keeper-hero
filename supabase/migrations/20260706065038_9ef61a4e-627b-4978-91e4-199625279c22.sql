-- 1. DM read receipts
ALTER TABLE public.direct_messages ADD COLUMN IF NOT EXISTS read_at timestamptz;

CREATE OR REPLACE FUNCTION public.mark_dms_read(_other uuid)
RETURNS void LANGUAGE sql SECURITY DEFINER SET search_path=public AS $$
  UPDATE public.direct_messages SET read_at = now()
  WHERE recipient_id = auth.uid() AND sender_id = _other AND read_at IS NULL;
$$;

-- 2. Push subscriptions
CREATE TABLE IF NOT EXISTS public.push_subscriptions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  endpoint text NOT NULL UNIQUE,
  p256dh text NOT NULL,
  auth text NOT NULL,
  user_agent text,
  created_at timestamptz NOT NULL DEFAULT now()
);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.push_subscriptions TO authenticated;
GRANT ALL ON public.push_subscriptions TO service_role;
ALTER TABLE public.push_subscriptions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "own push subs" ON public.push_subscriptions;
CREATE POLICY "own push subs" ON public.push_subscriptions FOR ALL
  USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

-- 3. Scheduled notifications (server sender picks these up)
CREATE TABLE IF NOT EXISTS public.scheduled_notifications (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  deliver_at timestamptz NOT NULL DEFAULT now(),
  title text NOT NULL,
  body text NOT NULL,
  tag text,
  url text,
  sent boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_sched_notifs_due ON public.scheduled_notifications (sent, deliver_at);
GRANT SELECT, INSERT ON public.scheduled_notifications TO authenticated;
GRANT ALL ON public.scheduled_notifications TO service_role;
ALTER TABLE public.scheduled_notifications ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "own sched read" ON public.scheduled_notifications;
CREATE POLICY "own sched read" ON public.scheduled_notifications FOR SELECT USING (auth.uid() = user_id);
DROP POLICY IF EXISTS "own sched insert" ON public.scheduled_notifications;
CREATE POLICY "own sched insert" ON public.scheduled_notifications FOR INSERT WITH CHECK (auth.uid() = user_id);

-- 4. Badges: ensure unique constraint exists
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='badges_user_id_category_threshold_key') THEN
    ALTER TABLE public.badges ADD CONSTRAINT badges_user_id_category_threshold_key UNIQUE (user_id, category, threshold);
  END IF;
END $$;

-- 5. Recompute streak badges: FIRE (consecutive event days, today required) + ICE (consecutive no-event days)
CREATE OR REPLACE FUNCTION public.check_streak_badges(_user_id uuid)
RETURNS void
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  _today date := CURRENT_DATE;
  _cur date;
  _fire int := 0;
  _ice int := 0;
  _has_today boolean;
  _fire_thresholds int[] := ARRAY[3,7,14,21,30,50,75,100,150,200,300];
  _fire_tiers text[] := ARRAY['common','common','uncommon','uncommon','rare','super_rare','epic','mythic','mythic','legendary','legendary'];
  _ice_thresholds int[] := ARRAY[1,3,7,14,30,60,100];
  _ice_tiers text[] := ARRAY['rare','epic','epic','mythic','mythic','legendary','legendary'];
  i int;
BEGIN
  SELECT EXISTS(SELECT 1 FROM public.events WHERE user_id = _user_id AND date_trunc('day', ts)::date = _today) INTO _has_today;

  IF _has_today THEN
    _cur := _today;
    LOOP
      EXIT WHEN NOT EXISTS(SELECT 1 FROM public.events WHERE user_id = _user_id AND date_trunc('day', ts)::date = _cur);
      _fire := _fire + 1;
      _cur := _cur - 1;
      EXIT WHEN _fire > 3650;
    END LOOP;
    _ice := 0;
  ELSE
    _fire := 0;
    _cur := _today;
    LOOP
      EXIT WHEN EXISTS(SELECT 1 FROM public.events WHERE user_id = _user_id AND date_trunc('day', ts)::date = _cur);
      _ice := _ice + 1;
      _cur := _cur - 1;
      EXIT WHEN _ice > 3650;
    END LOOP;
  END IF;

  FOR i IN 1 .. array_length(_fire_thresholds, 1) LOOP
    IF _fire >= _fire_thresholds[i] THEN
      INSERT INTO public.badges (user_id, category, tier, threshold)
      VALUES (_user_id, 'fire', _fire_tiers[i], _fire_thresholds[i])
      ON CONFLICT (user_id, category, threshold) DO NOTHING;
    END IF;
  END LOOP;

  FOR i IN 1 .. array_length(_ice_thresholds, 1) LOOP
    IF _ice >= _ice_thresholds[i] THEN
      INSERT INTO public.badges (user_id, category, tier, threshold)
      VALUES (_user_id, 'ice', _ice_tiers[i], _ice_thresholds[i])
      ON CONFLICT (user_id, category, threshold) DO NOTHING;
    END IF;
  END LOOP;
END; $$;

-- 6. Auto-run on events changes so the bug can't recur
CREATE OR REPLACE FUNCTION public.on_event_change()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
BEGIN
  PERFORM public.check_streak_badges(COALESCE(NEW.user_id, OLD.user_id));
  RETURN COALESCE(NEW, OLD);
END; $$;

DROP TRIGGER IF EXISTS trg_events_streak ON public.events;
CREATE TRIGGER trg_events_streak AFTER INSERT OR DELETE ON public.events
FOR EACH ROW EXECUTE FUNCTION public.on_event_change();

-- 7. Update badge announcer for new categories + tiers
CREATE OR REPLACE FUNCTION public.on_badge_awarded()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE _clan uuid; _uname text; _tier_label text; _cat text;
BEGIN
  IF NEW.tier NOT IN ('epic','mythic','legendary','super_rare') THEN RETURN NEW; END IF;
  SELECT clan_id, username INTO _clan, _uname FROM public.profiles WHERE id = NEW.user_id;
  IF _clan IS NULL THEN RETURN NEW; END IF;
  _tier_label := CASE NEW.tier
    WHEN 'super_rare' THEN 'SUPER RARA ✨'
    WHEN 'epic' THEN 'EPICA ✨'
    WHEN 'mythic' THEN 'MITICA 🌟'
    WHEN 'legendary' THEN 'LEGGENDARIA 👑'
  END;
  _cat := CASE NEW.category WHEN 'ice' THEN 'di GHIACCIO ❄️' WHEN 'fire' THEN 'di FUOCO 🔥' ELSE '' END;
  INSERT INTO public.clan_messages (clan_id, sender_id, content, is_system)
  VALUES (_clan, NULL, '🏆 ' || _uname || ' ha ottenuto una medaglia ' || _tier_label || ' ' || _cat || ' (' || NEW.threshold || ' giorni)!', true);
  RETURN NEW;
END; $$;

DROP TRIGGER IF EXISTS trg_badge_awarded ON public.badges;
CREATE TRIGGER trg_badge_awarded AFTER INSERT ON public.badges
FOR EACH ROW EXECUTE FUNCTION public.on_badge_awarded();

-- 8. Realtime on push-relevant tables so the SW/client picks up new stuff
ALTER PUBLICATION supabase_realtime ADD TABLE public.scheduled_notifications;