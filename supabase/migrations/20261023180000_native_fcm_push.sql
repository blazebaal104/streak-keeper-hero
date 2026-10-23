-- Native instant push (FCM) + fewer, more important notifications.

-- 1) phones that can receive pushes
create table if not exists public.fcm_tokens (
  token text primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  app text not null default 'android',
  updated_at timestamptz not null default now()
);
create index if not exists fcm_tokens_user on public.fcm_tokens(user_id);
alter table public.fcm_tokens enable row level security;
drop policy if exists "own fcm tokens" on public.fcm_tokens;
create policy "own fcm tokens" on public.fcm_tokens for all to authenticated
  using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- 2) FCM delivery state, separate from web push's "sent"
alter table public.scheduled_notifications add column if not exists fcm_sent boolean not null default false;
update public.scheduled_notifications set fcm_sent = true where deliver_at <= now();

-- atomically claim due rows (concurrent calls never send twice)
create or replace function public.claim_fcm_batch(_limit int default 100)
returns setof public.scheduled_notifications
language sql security definer set search_path = public as $$
  -- stale rows (phone unreachable for a long time, sender down) are dropped, not delivered late
  update public.scheduled_notifications set fcm_sent = true where not fcm_sent and deliver_at < now() - interval '30 minutes';
  update public.scheduled_notifications s set fcm_sent = true
  where s.id in (
    select id from public.scheduled_notifications
    where not fcm_sent and deliver_at <= now() + interval '2 seconds'
    order by deliver_at limit _limit for update skip locked
  )
  returning s.*;
$$;
revoke all on function public.claim_fcm_batch(int) from public, anon, authenticated;

-- 3) wake the sender right away when something is queued for now
create or replace function public.kick_push()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  if exists (select 1 from new_rows where deliver_at <= now() + interval '2 seconds') then
    perform net.http_post(url := '__FN_URL__', headers := '{"Content-Type":"application/json"}'::jsonb, body := '{}'::jsonb);
  end if;
  return null;
end $$;
drop trigger if exists trg_kick_push on public.scheduled_notifications;
create trigger trg_kick_push after insert on public.scheduled_notifications
  referencing new table as new_rows for each statement execute function public.kick_push();

-- 4) fewer silly notifications: the delayed "over limit" shame push is dropped
create or replace function public.drop_silly_push()
returns trigger language plpgsql as $$
begin
  if new.tag = 'over-limit-shame' then return null; end if;
  return new;
end $$;
drop trigger if exists trg_drop_silly_push on public.scheduled_notifications;
create trigger trg_drop_silly_push before insert on public.scheduled_notifications
  for each row execute function public.drop_silly_push();

-- 5) important: a new medal of your own
create or replace function public.on_badge_push()
returns trigger language plpgsql security definer set search_path = public as $$
declare _name text; _emoji text; _what text;
begin
  if (select notifications_enabled from public.profiles where id = new.user_id) is distinct from true then return new; end if;
  select n, e into _name, _emoji from (values
    ('fire',3,'Scintilla','✨'),('fire',7,'Falò','🔥'),('fire',14,'Braciere','🪵'),('fire',21,'Camino','🏮'),('fire',30,'Fornace','⚒️'),
    ('fire',50,'Vulcano','🌋'),('fire',75,'Inferno','⚡'),('fire',100,'Sole','☀️'),('fire',150,'Cometa','☄️'),('fire',200,'Stella','🌟'),('fire',300,'Nova','👑'),
    ('ice',1,'Brina','🌨️'),('ice',3,'Gelo','❄️'),('ice',7,'Cristallo','💎'),('ice',14,'Iceberg','🏔️'),('ice',30,'Ghiacciaio','🧊'),('ice',60,'Polare','🐻‍❄️'),('ice',100,'Assoluto Zero','🌌'),
    ('workout',5,'Primi passi','👟'),('workout',10,'Principiante','💪'),('workout',25,'Costante','🏃'),('workout',50,'Atleta','🏅'),('workout',75,'Guerriero','⚔️'),
    ('workout',100,'Atleta professionista','🏆'),('workout',200,'Macchina','🦾'),('workout',365,'Leggenda','👑')
  ) as d(c, t, n, e) where d.c = new.category and d.t = new.threshold;
  _what := case new.category when 'fire' then new.threshold || ' giorni di fuoco di fila'
    when 'ice' then new.threshold || ' giorni puliti di fila' when 'workout' then new.threshold || ' allenamenti' else new.threshold::text end;
  insert into public.scheduled_notifications (user_id, deliver_at, title, body, tag, url)
  values (new.user_id, now(), '🏅 Nuova medaglia: ' || coalesce(_name, 'sbloccata') || ' ' || coalesce(_emoji, ''),
          'Hai sbloccato ' || coalesce(_name, 'una medaglia') || ' (' || _what || '). Equipaggiala dal profilo!', 'badge:' || new.id, '/?open=profile');
  return new;
end $$;
drop trigger if exists trg_badge_push on public.badges;
create trigger trg_badge_push after insert on public.badges for each row execute function public.on_badge_push();

-- 6) important: the fire streak is about to die (evening, Italian time, once a day)
create or replace function public.queue_streak_reminders()
returns int language plpgsql security definer set search_path = public as $$
declare _today date := (now() at time zone 'Europe/Rome')::date; r record; _n int; _d date; _count int := 0;
begin
  for r in
    select p.id, p.username from public.profiles p
    where p.notif_streak and p.notifications_enabled
      and exists (select 1 from public.events e where e.user_id = p.id and (e.ts at time zone 'Europe/Rome')::date = _today - 1)
      and not exists (select 1 from public.events e where e.user_id = p.id and (e.ts at time zone 'Europe/Rome')::date = _today)
      and not exists (select 1 from public.scheduled_notifications s where s.user_id = p.id and s.tag = 'streak:' || _today)
  loop
    _n := 0; _d := _today - 1;
    while exists (select 1 from public.events e where e.user_id = r.id and (e.ts at time zone 'Europe/Rome')::date = _d) and _n < 3650 loop
      _n := _n + 1; _d := _d - 1;
    end loop;
    insert into public.scheduled_notifications (user_id, deliver_at, title, body, tag, url)
    values (r.id, now(), '🔥 La tua streak sta per spegnersi!', coalesce(r.username, 'Ehi') || ', sei a ' || _n || ' giorni di fila. Se oggi non fai niente, a mezzanotte la perdi.', 'streak:' || _today, '/');
    _count := _count + 1;
  end loop;
  return _count;
end $$;
revoke all on function public.queue_streak_reminders() from public, anon, authenticated;

-- 7) schedules: streak reminder at 20:30 Italian time (cron runs in UTC: hourly at :30, filtered here),
--    and a once-a-minute sweep for anything scheduled for later
select cron.unschedule(jobid) from cron.job where jobname in ('push-fcm', 'streak-reminders');
select cron.schedule('push-fcm', '* * * * *', $$
  select net.http_post(url := '__FN_URL__', headers := '{"Content-Type":"application/json"}'::jsonb, body := '{}'::jsonb)
  where exists (select 1 from public.scheduled_notifications where not fcm_sent and deliver_at <= now())
$$);
select cron.schedule('streak-reminders', '30 * * * *', $$
  select public.queue_streak_reminders() where extract(hour from now() at time zone 'Europe/Rome') = 20
$$);
