
-- Wipe previously mis-awarded ice medals (given for free to every new account)
DELETE FROM public.badges WHERE category = 'ice';

-- Rewrite streak check so ice streak only counts when the user has at least one event ever,
-- and never runs off into the past for an account with zero activity.
CREATE OR REPLACE FUNCTION public.check_streak_badges(_user_id uuid)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
  _today date := CURRENT_DATE;
  _cur date;
  _first_day date;
  _fire int := 0;
  _ice int := 0;
  _has_today boolean;
  _fire_thresholds int[] := ARRAY[3,7,14,21,30,50,75,100,150,200,300];
  _fire_tiers text[] := ARRAY['common','common','uncommon','uncommon','rare','super_rare','epic','mythic','mythic','legendary','legendary'];
  _ice_thresholds int[] := ARRAY[1,3,7,14,30,60,100];
  _ice_tiers text[] := ARRAY['rare','epic','epic','mythic','mythic','legendary','legendary'];
  i int;
BEGIN
  -- Earliest event date; if the user has never logged, no streak of any kind exists.
  SELECT date_trunc('day', MIN(ts))::date INTO _first_day
    FROM public.events WHERE user_id = _user_id;
  IF _first_day IS NULL THEN RETURN; END IF;

  SELECT EXISTS(SELECT 1 FROM public.events WHERE user_id = _user_id AND date_trunc('day', ts)::date = _today) INTO _has_today;

  IF _has_today THEN
    _cur := _today;
    LOOP
      EXIT WHEN _cur < _first_day;
      EXIT WHEN NOT EXISTS(SELECT 1 FROM public.events WHERE user_id = _user_id AND date_trunc('day', ts)::date = _cur);
      _fire := _fire + 1;
      _cur := _cur - 1;
    END LOOP;
    _ice := 0;
  ELSE
    _fire := 0;
    _cur := _today;
    LOOP
      EXIT WHEN _cur < _first_day;
      EXIT WHEN EXISTS(SELECT 1 FROM public.events WHERE user_id = _user_id AND date_trunc('day', ts)::date = _cur);
      _ice := _ice + 1;
      _cur := _cur - 1;
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
END; $function$;
