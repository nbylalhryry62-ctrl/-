-- Aden Medical Test — schema تأسيسي idempotent للمشروعات الجديدة فقط.
-- لا تشغّل هذا الملف على قاعدة aden-med-prod الحالية؛ مخططها وسياسات RLS منشأة بالفعل.

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text,
  full_name text not null default '',
  role text not null default 'student' check (role in ('student', 'admin')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.profiles add column if not exists email text;
alter table public.profiles add column if not exists full_name text not null default '';
alter table public.profiles add column if not exists role text not null default 'student';
alter table public.profiles add column if not exists created_at timestamptz not null default now();
alter table public.profiles add column if not exists updated_at timestamptz not null default now();

create table if not exists public.results (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  subject text not null check (subject in ('biology', 'chemistry', 'english')),
  level text not null default '1',
  score integer not null check (score >= 0),
  total integer not null check (total > 0),
  percentage integer not null check (percentage between 0 and 100),
  created_at timestamptz not null default now()
);

alter table public.results add column if not exists level text not null default '1';
alter table public.results add column if not exists percentage integer;
create index if not exists results_user_created_idx on public.results(user_id, created_at desc);

alter table public.profiles enable row level security;
alter table public.results enable row level security;
grant select on public.profiles to authenticated;
grant update (full_name, updated_at) on public.profiles to authenticated;
grant select, insert on public.results to authenticated;

do $$
begin
  if not exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'profiles' and policyname = 'aden_profiles_select_own') then
    execute 'create policy aden_profiles_select_own on public.profiles for select to authenticated using (auth.uid() = id)';
  end if;
  if not exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'profiles' and policyname = 'aden_profiles_insert_own') then
    execute 'create policy aden_profiles_insert_own on public.profiles for insert to authenticated with check (auth.uid() = id)';
  end if;
  if not exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'profiles' and policyname = 'aden_profiles_update_own') then
    execute 'create policy aden_profiles_update_own on public.profiles for update to authenticated using (auth.uid() = id) with check (auth.uid() = id)';
  end if;
  if not exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'results' and policyname = 'aden_results_select_own') then
    execute 'create policy aden_results_select_own on public.results for select to authenticated using (auth.uid() = user_id)';
  end if;
  if not exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'results' and policyname = 'aden_results_insert_own') then
    execute 'create policy aden_results_insert_own on public.results for insert to authenticated with check (auth.uid() = user_id)';
  end if;
end $$;

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, email, full_name)
  values (new.id, new.email, coalesce(new.raw_user_meta_data ->> 'full_name', ''))
  on conflict (id) do nothing;
  return new;
end;
$$;

do $$
begin
  if not exists (select 1 from pg_trigger where tgname = 'aden_on_auth_user_created' and tgrelid = 'auth.users'::regclass) then
    execute 'create trigger aden_on_auth_user_created after insert on auth.users for each row execute procedure public.handle_new_user()';
  end if;
end $$;
