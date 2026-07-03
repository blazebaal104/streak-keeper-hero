
-- ============ CLANS ============
CREATE TABLE public.clans (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text NOT NULL,
  tag text NOT NULL UNIQUE,
  owner_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now()
);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.clans TO authenticated;
GRANT ALL ON public.clans TO service_role;
ALTER TABLE public.clans ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone auth can view clans" ON public.clans FOR SELECT TO authenticated USING (true);
CREATE POLICY "Users create clans" ON public.clans FOR INSERT TO authenticated WITH CHECK (auth.uid() = owner_id);
CREATE POLICY "Owner updates clan" ON public.clans FOR UPDATE TO authenticated USING (auth.uid() = owner_id);
CREATE POLICY "Owner deletes clan" ON public.clans FOR DELETE TO authenticated USING (auth.uid() = owner_id);

-- ============ PROFILES ============
CREATE TABLE public.profiles (
  id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  username text NOT NULL UNIQUE,
  avatar text NOT NULL DEFAULT '💧',
  friend_code text NOT NULL UNIQUE,
  daily_limit int NOT NULL DEFAULT 3,
  use_adaptive_limit boolean NOT NULL DEFAULT true,
  notifications_enabled boolean NOT NULL DEFAULT true,
  clan_id uuid REFERENCES public.clans(id) ON DELETE SET NULL,
  slot_1 uuid,
  slot_2 uuid,
  slot_3 uuid,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON public.profiles(username);
CREATE INDEX ON public.profiles(friend_code);
CREATE INDEX ON public.profiles(clan_id);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.profiles TO authenticated;
GRANT ALL ON public.profiles TO service_role;
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Auth can view all profiles" ON public.profiles FOR SELECT TO authenticated USING (true);
CREATE POLICY "Users update own profile" ON public.profiles FOR UPDATE TO authenticated USING (auth.uid() = id);
CREATE POLICY "Users insert own profile" ON public.profiles FOR INSERT TO authenticated WITH CHECK (auth.uid() = id);

-- ============ EVENTS ============
CREATE TABLE public.events (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  ts timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON public.events(user_id, ts DESC);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.events TO authenticated;
GRANT ALL ON public.events TO service_role;
ALTER TABLE public.events ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users own events" ON public.events FOR ALL TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

-- ============ BADGES ============
-- category: 'streak' | 'friends' | 'wars'
-- tier: 'common' | 'rare' | 'epic' | 'legendary' | 'mythic'
CREATE TABLE public.badges (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  category text NOT NULL,
  tier text NOT NULL,
  threshold int NOT NULL,
  awarded_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(user_id, category, threshold)
);
CREATE INDEX ON public.badges(user_id);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.badges TO authenticated;
GRANT ALL ON public.badges TO service_role;
ALTER TABLE public.badges ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Auth can view all badges" ON public.badges FOR SELECT TO authenticated USING (true);
-- No client insert; only trigger/service inserts.

-- ============ FRIENDSHIPS ============ (accepted pairs, user_a < user_b enforced)
CREATE TABLE public.friendships (
  user_a uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  user_b uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (user_a, user_b),
  CHECK (user_a < user_b)
);
CREATE INDEX ON public.friendships(user_b);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.friendships TO authenticated;
GRANT ALL ON public.friendships TO service_role;
ALTER TABLE public.friendships ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users see own friendships" ON public.friendships FOR SELECT TO authenticated USING (auth.uid() = user_a OR auth.uid() = user_b);
CREATE POLICY "Users add friendships involving self" ON public.friendships FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_a OR auth.uid() = user_b);
CREATE POLICY "Users remove own friendships" ON public.friendships FOR DELETE TO authenticated USING (auth.uid() = user_a OR auth.uid() = user_b);

-- ============ DIRECT MESSAGES ============
CREATE TABLE public.direct_messages (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  sender_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  recipient_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  content text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON public.direct_messages(sender_id, recipient_id, created_at);
CREATE INDEX ON public.direct_messages(recipient_id, sender_id, created_at);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.direct_messages TO authenticated;
GRANT ALL ON public.direct_messages TO service_role;
ALTER TABLE public.direct_messages ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users read own direct messages" ON public.direct_messages FOR SELECT TO authenticated USING (auth.uid() = sender_id OR auth.uid() = recipient_id);
CREATE POLICY "Users send own direct messages" ON public.direct_messages FOR INSERT TO authenticated WITH CHECK (auth.uid() = sender_id);

-- ============ CLAN MESSAGES ============
CREATE TABLE public.clan_messages (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  clan_id uuid NOT NULL REFERENCES public.clans(id) ON DELETE CASCADE,
  sender_id uuid REFERENCES auth.users(id) ON DELETE SET NULL,
  content text NOT NULL,
  is_system boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON public.clan_messages(clan_id, created_at);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.clan_messages TO authenticated;
GRANT ALL ON public.clan_messages TO service_role;
ALTER TABLE public.clan_messages ENABLE ROW LEVEL SECURITY;

-- Helper: is user in this clan?
CREATE OR REPLACE FUNCTION public.is_clan_member(_clan_id uuid, _user_id uuid)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT EXISTS (SELECT 1 FROM public.profiles WHERE id = _user_id AND clan_id = _clan_id);
$$;

CREATE POLICY "Clan members read clan messages" ON public.clan_messages FOR SELECT TO authenticated USING (public.is_clan_member(clan_id, auth.uid()));
CREATE POLICY "Clan members send clan messages" ON public.clan_messages FOR INSERT TO authenticated WITH CHECK (sender_id = auth.uid() AND is_system = false AND public.is_clan_member(clan_id, auth.uid()));

-- ============ FRIEND CODE GENERATOR ============
CREATE OR REPLACE FUNCTION public.generate_friend_code()
RETURNS text LANGUAGE plpgsql AS $$
DECLARE
  chars text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  code text;
  i int;
  attempts int := 0;
BEGIN
  LOOP
    code := '';
    FOR i IN 1..6 LOOP
      code := code || substr(chars, 1 + floor(random() * length(chars))::int, 1);
    END LOOP;
    EXIT WHEN NOT EXISTS (SELECT 1 FROM public.profiles WHERE friend_code = code);
    attempts := attempts + 1;
    IF attempts > 100 THEN RAISE EXCEPTION 'Cannot generate unique friend code'; END IF;
  END LOOP;
  RETURN code;
END; $$;

-- ============ handle_new_user: create profile on signup ============
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  _username text;
  _base text;
  _n int := 0;
BEGIN
  _base := lower(regexp_replace(COALESCE(NEW.raw_user_meta_data->>'username', split_part(NEW.email,'@',1)), '[^a-z0-9_]', '', 'g'));
  IF _base = '' THEN _base := 'sbo_user'; END IF;
  _username := _base;
  WHILE EXISTS (SELECT 1 FROM public.profiles WHERE username = _username) LOOP
    _n := _n + 1;
    _username := _base || _n::text;
  END LOOP;

  INSERT INTO public.profiles (id, username, friend_code)
  VALUES (NEW.id, _username, public.generate_friend_code());
  RETURN NEW;
END; $$;

CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ============ Badge auto-announce trigger ============
CREATE OR REPLACE FUNCTION public.on_badge_awarded()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  _clan uuid;
  _uname text;
  _tier_label text;
BEGIN
  IF NEW.tier NOT IN ('epic','legendary','mythic') THEN RETURN NEW; END IF;
  SELECT clan_id, username INTO _clan, _uname FROM public.profiles WHERE id = NEW.user_id;
  IF _clan IS NULL THEN RETURN NEW; END IF;
  _tier_label := CASE NEW.tier
    WHEN 'epic' THEN 'EPICA ✨'
    WHEN 'legendary' THEN 'LEGGENDARIA 👑'
    WHEN 'mythic' THEN 'MITICA 🌟'
  END;
  INSERT INTO public.clan_messages (clan_id, sender_id, content, is_system)
  VALUES (_clan, NULL, '🏆 ' || _uname || ' ha ottenuto una medaglia ' || _tier_label || ' (streak ' || NEW.threshold || ' giorni)!', true);
  RETURN NEW;
END; $$;

CREATE TRIGGER on_badge_insert_announce
  AFTER INSERT ON public.badges
  FOR EACH ROW EXECUTE FUNCTION public.on_badge_awarded();

-- ============ Streak-badge award function (idempotent, called by client) ============
CREATE OR REPLACE FUNCTION public.check_streak_badges(_user_id uuid)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  _last_event_day date;
  _streak int;
  _thresholds int[] := ARRAY[10, 25, 50, 100, 150];
  _tiers text[] := ARRAY['common','rare','epic','legendary','mythic'];
  i int;
BEGIN
  IF auth.uid() IS NULL OR auth.uid() <> _user_id THEN RETURN; END IF;
  SELECT max(date_trunc('day', ts))::date INTO _last_event_day FROM public.events WHERE user_id = _user_id;
  IF _last_event_day IS NULL THEN
    _streak := 0;
  ELSE
    _streak := GREATEST(0, (CURRENT_DATE - _last_event_day)::int);
    -- If there's an event today, streak = 0
    IF _last_event_day = CURRENT_DATE THEN _streak := 0; END IF;
  END IF;

  FOR i IN 1 .. array_length(_thresholds, 1) LOOP
    IF _streak >= _thresholds[i] THEN
      INSERT INTO public.badges (user_id, category, tier, threshold)
      VALUES (_user_id, 'streak', _tiers[i], _thresholds[i])
      ON CONFLICT (user_id, category, threshold) DO NOTHING;
    END IF;
  END LOOP;
END; $$;

GRANT EXECUTE ON FUNCTION public.check_streak_badges(uuid) TO authenticated;

-- ============ Realtime ============
ALTER PUBLICATION supabase_realtime ADD TABLE public.direct_messages;
ALTER PUBLICATION supabase_realtime ADD TABLE public.clan_messages;
ALTER PUBLICATION supabase_realtime ADD TABLE public.badges;
