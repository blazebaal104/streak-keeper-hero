
CREATE OR REPLACE FUNCTION public.check_streak_badges(_user_id uuid)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
  _today date := CURRENT_DATE;
  _last_day date;
  _cur date;
  _fire int := 0;
  _ice int := 0;
  _days_since int;
  _fire_thresholds int[] := ARRAY[3,7,14,21,30,50,75,100,150,200,300];
  _fire_tiers text[] := ARRAY['common','common','uncommon','uncommon','rare','super_rare','epic','mythic','mythic','legendary','legendary'];
  _ice_thresholds int[] := ARRAY[1,3,7,14,30,60,100];
  _ice_tiers text[] := ARRAY['rare','epic','epic','mythic','mythic','legendary','legendary'];
  i int;
BEGIN
  -- Latest event day (local-ignoring; matches client which uses local day but for badges UTC-day is close enough)
  SELECT date_trunc('day', MAX(ts))::date INTO _last_day
    FROM public.events WHERE user_id = _user_id;
  IF _last_day IS NULL THEN RETURN; END IF;

  _days_since := (_today - _last_day);

  -- FIRE: consecutive days with events ending at _last_day, but only alive if today or yesterday.
  IF _days_since <= 1 THEN
    _cur := _last_day;
    LOOP
      EXIT WHEN NOT EXISTS(SELECT 1 FROM public.events WHERE user_id = _user_id AND date_trunc('day', ts)::date = _cur);
      _fire := _fire + 1;
      _cur := _cur - 1;
    END LOOP;
    _ice := 0;
  ELSE
    _fire := 0;
    -- ICE with 24h grace: activates only from the day after a fully empty day.
    _ice := GREATEST(_days_since - 1, 0);
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
