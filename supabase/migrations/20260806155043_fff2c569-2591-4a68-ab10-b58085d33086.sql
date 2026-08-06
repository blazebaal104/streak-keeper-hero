CREATE OR REPLACE FUNCTION public.on_dm_push()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE _sender text; _enabled boolean;
BEGIN
  SELECT notif_messages INTO _enabled FROM public.profiles WHERE id = NEW.recipient_id;
  IF _enabled IS DISTINCT FROM true THEN RETURN NEW; END IF;
  SELECT username INTO _sender FROM public.profiles WHERE id = NEW.sender_id;
  INSERT INTO public.scheduled_notifications (user_id, deliver_at, title, body, tag, url)
  VALUES (NEW.recipient_id, now(), COALESCE(_sender,'Un amico') || ' ti ha scritto',
          left(NEW.content, 180), 'dm:' || NEW.sender_id::text, '/');
  RETURN NEW;
END; $$;

DROP TRIGGER IF EXISTS trg_dm_push ON public.direct_messages;
CREATE TRIGGER trg_dm_push AFTER INSERT ON public.direct_messages
FOR EACH ROW EXECUTE FUNCTION public.on_dm_push();

CREATE OR REPLACE FUNCTION public.on_clan_message_push()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE _sender text; _clan text;
BEGIN
  SELECT name INTO _clan FROM public.clans WHERE id = NEW.clan_id;
  IF NEW.sender_id IS NOT NULL THEN
    SELECT username INTO _sender FROM public.profiles WHERE id = NEW.sender_id;
  END IF;
  INSERT INTO public.scheduled_notifications (user_id, deliver_at, title, body, tag, url)
  SELECT p.id, now(),
         COALESCE(_clan, 'Clan') || (CASE WHEN NEW.is_system THEN ' — annuncio' ELSE '' END),
         CASE WHEN NEW.is_system THEN left(NEW.content,180) ELSE COALESCE(_sender,'Un membro') || ': ' || left(NEW.content,180) END,
         'clan:' || NEW.clan_id::text, '/'
  FROM public.profiles p
  WHERE p.clan_id = NEW.clan_id
    AND (NEW.sender_id IS NULL OR p.id <> NEW.sender_id)
    AND ((NEW.is_system AND p.notif_social) OR ((NOT NEW.is_system) AND p.notif_messages));
  RETURN NEW;
END; $$;

DROP TRIGGER IF EXISTS trg_clan_message_push ON public.clan_messages;
CREATE TRIGGER trg_clan_message_push AFTER INSERT ON public.clan_messages
FOR EACH ROW EXECUTE FUNCTION public.on_clan_message_push();

CREATE OR REPLACE FUNCTION public.on_clan_invite_push()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE _from text; _clan text; _enabled boolean;
BEGIN
  SELECT notif_social INTO _enabled FROM public.profiles WHERE id = NEW.to_user_id;
  IF _enabled IS DISTINCT FROM true THEN RETURN NEW; END IF;
  SELECT username INTO _from FROM public.profiles WHERE id = NEW.from_user_id;
  SELECT name INTO _clan FROM public.clans WHERE id = NEW.clan_id;
  INSERT INTO public.scheduled_notifications (user_id, deliver_at, title, body, tag, url)
  VALUES (NEW.to_user_id, now(), 'Invito al clan',
          COALESCE(_from,'Un amico') || ' ti ha invitato in ' || COALESCE(_clan,'un clan'),
          'invite:' || NEW.id::text, '/');
  RETURN NEW;
END; $$;

DROP TRIGGER IF EXISTS trg_clan_invite_push ON public.clan_invites;
CREATE TRIGGER trg_clan_invite_push AFTER INSERT ON public.clan_invites
FOR EACH ROW EXECUTE FUNCTION public.on_clan_invite_push();

CREATE OR REPLACE FUNCTION public.on_friendship_push()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE _other uuid; _me uuid; _name text; _enabled boolean;
BEGIN
  _me := auth.uid();
  IF _me IS NULL THEN RETURN NEW; END IF;
  _other := CASE WHEN NEW.user_a = _me THEN NEW.user_b ELSE NEW.user_a END;
  SELECT notif_social INTO _enabled FROM public.profiles WHERE id = _other;
  IF _enabled IS DISTINCT FROM true THEN RETURN NEW; END IF;
  SELECT username INTO _name FROM public.profiles WHERE id = _me;
  INSERT INTO public.scheduled_notifications (user_id, deliver_at, title, body, tag, url)
  VALUES (_other, now(), 'Nuovo amico', COALESCE(_name,'Qualcuno') || ' ti ha aggiunto tra gli amici!', 'friend:' || _me::text, '/');
  RETURN NEW;
END; $$;

DROP TRIGGER IF EXISTS trg_friendship_push ON public.friendships;
CREATE TRIGGER trg_friendship_push AFTER INSERT ON public.friendships
FOR EACH ROW EXECUTE FUNCTION public.on_friendship_push();