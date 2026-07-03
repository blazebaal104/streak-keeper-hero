
REVOKE EXECUTE ON FUNCTION public.generate_friend_code() FROM anon, authenticated, PUBLIC;
REVOKE EXECUTE ON FUNCTION public.handle_new_user() FROM anon, authenticated, PUBLIC;
REVOKE EXECUTE ON FUNCTION public.on_badge_awarded() FROM anon, authenticated, PUBLIC;
REVOKE EXECUTE ON FUNCTION public.check_streak_badges(uuid) FROM anon, PUBLIC;
-- check_streak_badges stays callable by authenticated (it's a legitimate RPC).
