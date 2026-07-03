
-- Set search_path on generate_friend_code
CREATE OR REPLACE FUNCTION public.generate_friend_code()
RETURNS text LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
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

-- Convert is_clan_member to SECURITY INVOKER (RLS allows profile reads anyway)
CREATE OR REPLACE FUNCTION public.is_clan_member(_clan_id uuid, _user_id uuid)
RETURNS boolean LANGUAGE sql STABLE SECURITY INVOKER SET search_path = public AS $$
  SELECT EXISTS (SELECT 1 FROM public.profiles WHERE id = _user_id AND clan_id = _clan_id);
$$;

-- Revoke public execution on all SECURITY DEFINER functions (only triggers / RPC use them)
REVOKE EXECUTE ON FUNCTION public.generate_friend_code() FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.handle_new_user() FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.on_badge_awarded() FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.check_streak_badges(uuid) FROM PUBLIC;
-- Re-grant only check_streak_badges to authenticated (it's an RPC)
GRANT EXECUTE ON FUNCTION public.check_streak_badges(uuid) TO authenticated;
