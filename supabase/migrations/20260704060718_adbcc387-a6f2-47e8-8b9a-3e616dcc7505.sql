
-- 1. Profile enhancements: uploaded avatar + clan role
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS avatar_url text;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS clan_role text NOT NULL DEFAULT 'recruit';

-- 2. Reset clan_role when leaving clan
CREATE OR REPLACE FUNCTION public.on_profile_clan_change()
RETURNS trigger LANGUAGE plpgsql SET search_path = public AS $$
BEGIN
  IF NEW.clan_id IS DISTINCT FROM OLD.clan_id AND NEW.clan_id IS NULL THEN
    NEW.clan_role := 'recruit';
  END IF;
  RETURN NEW;
END; $$;
DROP TRIGGER IF EXISTS trg_profile_clan_change ON public.profiles;
CREATE TRIGGER trg_profile_clan_change BEFORE UPDATE ON public.profiles
FOR EACH ROW EXECUTE FUNCTION public.on_profile_clan_change();

-- 3. Clan invites
CREATE TABLE IF NOT EXISTS public.clan_invites (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  clan_id uuid NOT NULL REFERENCES public.clans(id) ON DELETE CASCADE,
  from_user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  to_user_id uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  status text NOT NULL DEFAULT 'pending',
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS clan_invites_pending_uidx
  ON public.clan_invites(clan_id, to_user_id) WHERE status = 'pending';

GRANT SELECT, INSERT, UPDATE, DELETE ON public.clan_invites TO authenticated;
GRANT ALL ON public.clan_invites TO service_role;
ALTER TABLE public.clan_invites ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "invites read own" ON public.clan_invites;
CREATE POLICY "invites read own" ON public.clan_invites FOR SELECT TO authenticated
USING (to_user_id = auth.uid() OR from_user_id = auth.uid());

DROP POLICY IF EXISTS "invites insert as member" ON public.clan_invites;
CREATE POLICY "invites insert as member" ON public.clan_invites FOR INSERT TO authenticated
WITH CHECK (from_user_id = auth.uid() AND public.is_clan_member(clan_id, auth.uid()));

DROP POLICY IF EXISTS "invites recipient updates" ON public.clan_invites;
CREATE POLICY "invites recipient updates" ON public.clan_invites FOR UPDATE TO authenticated
USING (to_user_id = auth.uid()) WITH CHECK (to_user_id = auth.uid());

ALTER PUBLICATION supabase_realtime ADD TABLE public.clan_invites;

-- 4. Friends-can-read visibility for events & badges (view friend profile)
CREATE OR REPLACE FUNCTION public.are_friends(_a uuid, _b uuid)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER SET search_path=public AS $$
  SELECT EXISTS(
    SELECT 1 FROM public.friendships
    WHERE user_a = LEAST(_a,_b) AND user_b = GREATEST(_a,_b)
  );
$$;

DROP POLICY IF EXISTS "events friends read" ON public.events;
CREATE POLICY "events friends read" ON public.events FOR SELECT TO authenticated
USING (user_id = auth.uid() OR public.are_friends(auth.uid(), user_id));

DROP POLICY IF EXISTS "badges friends read" ON public.badges;
CREATE POLICY "badges friends read" ON public.badges FOR SELECT TO authenticated
USING (user_id = auth.uid() OR public.are_friends(auth.uid(), user_id));

-- 5. Clan management RPCs
CREATE OR REPLACE FUNCTION public.set_clan_role(_target uuid, _role text)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE _my_clan uuid; _my_role text; _target_clan uuid;
BEGIN
  SELECT clan_id, clan_role INTO _my_clan, _my_role FROM public.profiles WHERE id = auth.uid();
  SELECT clan_id INTO _target_clan FROM public.profiles WHERE id = _target;
  IF _my_clan IS NULL OR _my_clan <> _target_clan THEN RAISE EXCEPTION 'not same clan'; END IF;
  IF _my_role <> 'leader' THEN RAISE EXCEPTION 'only leader can promote'; END IF;
  IF _role NOT IN ('leader','co_leader','recruit') THEN RAISE EXCEPTION 'bad role'; END IF;
  UPDATE public.profiles SET clan_role = _role WHERE id = _target;
END; $$;

CREATE OR REPLACE FUNCTION public.kick_clan_member(_target uuid)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE _my_clan uuid; _my_role text; _target_clan uuid;
BEGIN
  SELECT clan_id, clan_role INTO _my_clan, _my_role FROM public.profiles WHERE id = auth.uid();
  SELECT clan_id INTO _target_clan FROM public.profiles WHERE id = _target;
  IF _my_clan IS NULL OR _my_clan <> _target_clan THEN RAISE EXCEPTION 'not same clan'; END IF;
  IF _my_role <> 'leader' THEN RAISE EXCEPTION 'only leader can kick'; END IF;
  IF _target = auth.uid() THEN RAISE EXCEPTION 'cannot kick self'; END IF;
  UPDATE public.profiles SET clan_id = NULL, clan_role = 'recruit' WHERE id = _target;
END; $$;

CREATE OR REPLACE FUNCTION public.accept_clan_invite(_invite_id uuid)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE _inv record;
BEGIN
  SELECT * INTO _inv FROM public.clan_invites WHERE id = _invite_id;
  IF _inv IS NULL THEN RAISE EXCEPTION 'invite not found'; END IF;
  IF _inv.to_user_id <> auth.uid() THEN RAISE EXCEPTION 'not your invite'; END IF;
  IF _inv.status <> 'pending' THEN RAISE EXCEPTION 'already resolved'; END IF;
  UPDATE public.profiles SET clan_id = _inv.clan_id, clan_role = 'recruit' WHERE id = auth.uid();
  UPDATE public.clan_invites SET status = 'accepted' WHERE id = _invite_id;
END; $$;

CREATE OR REPLACE FUNCTION public.decline_clan_invite(_invite_id uuid)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
BEGIN
  UPDATE public.clan_invites SET status = 'declined'
  WHERE id = _invite_id AND to_user_id = auth.uid() AND status = 'pending';
END; $$;

-- 6. When creating a clan, upgrade creator to 'leader'
CREATE OR REPLACE FUNCTION public.on_clan_created()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
BEGIN
  UPDATE public.profiles SET clan_id = NEW.id, clan_role = 'leader' WHERE id = NEW.owner_id;
  RETURN NEW;
END; $$;
DROP TRIGGER IF EXISTS trg_clan_created ON public.clans;
CREATE TRIGGER trg_clan_created AFTER INSERT ON public.clans
FOR EACH ROW EXECUTE FUNCTION public.on_clan_created();

-- 7. Also allow reading a friend's profile in a wider way (already public inside the app,
-- but ensure username/avatar_url reads work). Profiles SELECT policy is already broad; no change.
