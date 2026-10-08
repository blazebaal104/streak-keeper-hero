CREATE OR REPLACE FUNCTION public.pz_guard()
 RETURNS trigger LANGUAGE plpgsql SET search_path TO 'public'
AS $function$
BEGIN
  NEW.updated_at = now();
  IF NEW.start_limit < 3 THEN NEW.start_limit = 3; END IF;
  IF NEW.start_limit > 5 THEN NEW.start_limit = 5; END IF;
  IF TG_OP = 'UPDATE' AND OLD.active AND NOT NEW.active
     AND NOT (NEW.answers ? '_redo')
     AND OLD.started_at IS NOT NULL AND now() < OLD.started_at + interval '7 days' THEN
    RAISE EXCEPTION 'Il Progetto Zero dura almeno 7 giorni';
  END IF;
  RETURN NEW;
END $function$;

CREATE OR REPLACE FUNCTION public.check_workout_badges(_user_id uuid)
 RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public'
AS $function$
DECLARE
  _n int;
  _th int[] := ARRAY[5,10,25,50,75,100,200,365];
  _tiers text[] := ARRAY['common','common','uncommon','rare','super_rare','epic','mythic','legendary'];
  i int;
BEGIN
  SELECT count(*) INTO _n FROM public.workout_days
   WHERE user_id = _user_id AND (completed OR jsonb_array_length(custom) > 0);
  FOR i IN 1 .. array_length(_th,1) LOOP
    IF _n >= _th[i] THEN
      INSERT INTO public.badges (user_id, category, tier, threshold)
      VALUES (_user_id, 'workout', _tiers[i], _th[i])
      ON CONFLICT (user_id, category, threshold) DO NOTHING;
    END IF;
  END LOOP;
END $function$;

CREATE OR REPLACE FUNCTION public.on_workout_change()
 RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public'
AS $function$
BEGIN
  PERFORM public.check_workout_badges(NEW.user_id);
  RETURN NEW;
END $function$;

DROP TRIGGER IF EXISTS trg_workout_badges ON public.workout_days;
CREATE TRIGGER trg_workout_badges AFTER INSERT OR UPDATE ON public.workout_days
FOR EACH ROW EXECUTE FUNCTION public.on_workout_change();

DROP TRIGGER IF EXISTS on_badge_insert_announce ON public.badges;

CREATE OR REPLACE FUNCTION public.on_badge_awarded()
 RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public'
AS $function$
DECLARE _clan uuid; _uname text; _tier_label text; _cat text; _unit text;
BEGIN
  IF NEW.tier NOT IN ('epic','mythic','legendary','super_rare') THEN RETURN NEW; END IF;
  SELECT clan_id, username INTO _clan, _uname FROM public.profiles WHERE id = NEW.user_id;
  IF _clan IS NULL THEN RETURN NEW; END IF;
  _tier_label := CASE NEW.tier WHEN 'super_rare' THEN 'SUPER RARA ✨' WHEN 'epic' THEN 'EPICA ✨' WHEN 'mythic' THEN 'MITICA 🌟' WHEN 'legendary' THEN 'LEGGENDARIA 👑' END;
  _cat := CASE NEW.category WHEN 'ice' THEN 'di GHIACCIO ❄️' WHEN 'fire' THEN 'di FUOCO 🔥' WHEN 'workout' THEN 'da ATLETA 💪' ELSE '' END;
  _unit := CASE NEW.category WHEN 'workout' THEN ' allenamenti' ELSE ' giorni' END;
  INSERT INTO public.clan_messages (clan_id, sender_id, content, is_system)
  VALUES (_clan, NULL, '🏆 ' || _uname || ' ha ottenuto una medaglia ' || _tier_label || ' ' || _cat || ' (' || NEW.threshold || _unit || ')!', true);
  RETURN NEW;
END $function$;