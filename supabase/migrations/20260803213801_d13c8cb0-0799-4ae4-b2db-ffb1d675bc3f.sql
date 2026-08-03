ALTER TABLE public.profiles
  ADD COLUMN IF NOT EXISTS notif_messages boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS notif_streak boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS notif_social boolean NOT NULL DEFAULT true;