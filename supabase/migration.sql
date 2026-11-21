-- Aden Admission Portal: Supabase schema, security and server-side logic.
-- Run once in the Supabase SQL Editor (safe to re-run).

-- ---------- tables ----------
create table if not exists public.access_codes (
  code         text primary key check (code ~ '^[A-Z0-9][A-Z0-9-]{5,39}$'),
  is_used      boolean not null default false,
  device_id    text,
  is_active    boolean not null default true,
  user_name    text not null default '',
  created_at   timestamptz not null default now(),
  activated_at timestamptz,
  constraint used_iff_bound check (is_used = (device_id is not null))
);

create table if not exists public.admins (
  user_id uuid primary key references auth.users (id) on delete cascade
);

create table if not exists public.app_config (
  id                   text primary key default 'main' check (id = 'main'),
  content_version      integer not null default 0,
  maintenance_mode     boolean not null default false,
  maintenance_message  text not null default '',
  announcement         text not null default '',
  contact_url          text not null default '',
  default_exam_minutes integer not null default 20 check (default_exam_minutes between 5 and 180),
  hidden_colleges      text[] not null default '{}'
);
insert into public.app_config (id) values ('main') on conflict do nothing;

create table if not exists public.question_patches (
  id      text primary key,
  v       integer not null,
  deleted boolean not null default false,
  data    jsonb not null default '{}'::jsonb
);
create index if not exists question_patches_v_idx on public.question_patches (v);

create table if not exists public.activation_attempts (
  id        bigserial primary key,
  device_id text not null,
  at        timestamptz not null default now()
);
create index if not exists activation_attempts_idx on public.activation_attempts (device_id, at);

-- ---------- row level security ----------
alter table public.access_codes        enable row level security;
alter table public.admins              enable row level security;
alter table public.app_config          enable row level security;
alter table public.question_patches    enable row level security;
alter table public.activation_attempts enable row level security;

create or replace function public.is_admin() returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.admins a where a.user_id = auth.uid())
$$;

drop policy if exists codes_admin on public.access_codes;
create policy codes_admin on public.access_codes for all to authenticated
  using (public.is_admin()) with check (public.is_admin());

drop policy if exists config_read on public.app_config;
create policy config_read on public.app_config for select to anon, authenticated using (true);
drop policy if exists config_admin on public.app_config;
create policy config_admin on public.app_config for all to authenticated
  using (public.is_admin()) with check (public.is_admin());

drop policy if exists patches_read on public.question_patches;
create policy patches_read on public.question_patches for select to anon, authenticated using (true);
drop policy if exists patches_admin on public.question_patches;
create policy patches_admin on public.question_patches for all to authenticated
  using (public.is_admin()) with check (public.is_admin());

-- Students (anon) never touch the codes, admins or attempts tables directly.
revoke all on public.access_codes, public.admins, public.activation_attempts from anon;
revoke all on public.admins, public.activation_attempts from authenticated;
revoke insert, update, delete on public.app_config, public.question_patches from anon;

-- ---------- student functions (the only door to the codes) ----------
-- Returns: 'ok' | 'invalid' | 'other_device' | 'locked'
create or replace function public.activate_code(p_code text, p_device text) returns text
language plpgsql security definer set search_path = public as $$
declare
  v_code text := upper(btrim(coalesce(p_code, '')));
  r public.access_codes%rowtype;
begin
  if p_device is null or length(p_device) < 16 or length(p_device) > 64 then
    return 'invalid';
  end if;
  -- slow down guessing: 10 failures per device in 15 minutes
  if (select count(*) from public.activation_attempts
        where device_id = p_device and at > now() - interval '15 minutes') >= 10 then
    return 'locked';
  end if;
  delete from public.activation_attempts where at < now() - interval '1 day';

  select * into r from public.access_codes where code = v_code for update;  -- row lock: racing phones are serialized
  if not found or not r.is_active then
    insert into public.activation_attempts (device_id) values (p_device);
    return 'invalid';
  end if;
  if r.is_used then
    if r.device_id = p_device then return 'ok'; end if;
    insert into public.activation_attempts (device_id) values (p_device);
    return 'other_device';
  end if;
  update public.access_codes
     set is_used = true, device_id = p_device, activated_at = now()
   where code = v_code;
  return 'ok';
end $$;

-- Returns: 'ok' | 'invalid' (missing or suspended) | 'unbound' (admin reset the device) | 'other_device'
create or replace function public.verify_code(p_code text, p_device text) returns text
language plpgsql stable security definer set search_path = public as $$
declare r public.access_codes%rowtype;
begin
  select * into r from public.access_codes where code = upper(btrim(coalesce(p_code, '')));
  if not found or not r.is_active then return 'invalid'; end if;
  if not r.is_used then return 'unbound'; end if;
  if r.device_id = p_device then return 'ok'; end if;
  return 'other_device';
end $$;

-- ---------- admin functions ----------
-- Saves settings and bumps content_version atomically. Returns the new version.
create or replace function public.save_config(p jsonb) returns integer
language plpgsql security definer set search_path = public as $$
declare v integer;
begin
  if not public.is_admin() then raise exception 'forbidden' using errcode = '42501'; end if;
  update public.app_config set
    maintenance_mode     = coalesce((p->>'maintenance_mode')::boolean, maintenance_mode),
    maintenance_message  = coalesce(p->>'maintenance_message', maintenance_message),
    announcement         = coalesce(p->>'announcement', announcement),
    contact_url          = coalesce(p->>'contact_url', contact_url),
    default_exam_minutes = coalesce((p->>'default_exam_minutes')::integer, default_exam_minutes),
    hidden_colleges      = case when p ? 'hidden_colleges'
                                then array(select jsonb_array_elements_text(p->'hidden_colleges'))
                                else hidden_colleges end,
    content_version      = content_version + 1
  where id = 'main'
  returning content_version into v;
  return v;
end $$;

-- Adds, edits or deletes a question for every student and bumps content_version. Returns the new version.
create or replace function public.publish_question(p_id text, p_data jsonb, p_deleted boolean) returns integer
language plpgsql security definer set search_path = public as $$
declare v integer;
begin
  if not public.is_admin() then raise exception 'forbidden' using errcode = '42501'; end if;
  if p_id is null or btrim(p_id) = '' then raise exception 'bad id'; end if;
  update public.app_config set content_version = content_version + 1 where id = 'main'
  returning content_version into v;
  insert into public.question_patches (id, v, deleted, data)
  values (p_id, v, coalesce(p_deleted, false), coalesce(p_data, '{}'::jsonb))
  on conflict (id) do update set v = excluded.v, deleted = excluded.deleted, data = excluded.data;
  return v;
end $$;

-- ---------- function privileges ----------
revoke all on function public.is_admin(), public.activate_code(text, text), public.verify_code(text, text),
  public.save_config(jsonb), public.publish_question(text, jsonb, boolean) from public;
grant execute on function public.activate_code(text, text), public.verify_code(text, text) to anon, authenticated;
grant execute on function public.is_admin(), public.save_config(jsonb), public.publish_question(text, jsonb, boolean) to authenticated;
