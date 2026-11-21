\set ON_ERROR_STOP on
-- seed as the table owner
insert into auth.users values ('00000000-0000-0000-0000-000000000001'), ('00000000-0000-0000-0000-000000000002');
insert into public.admins values ('00000000-0000-0000-0000-000000000001');
insert into public.access_codes (code, user_name) values ('ADEN-TEST-1111-2222', 'طالب'), ('ADEN-TEST-3333-4444', ''), ('ADEN-TEST-5555-6666', '');
insert into public.access_codes (code, is_active) values ('ADEN-TEST-SUSP-0000', false);

set role anon;
do $$ begin
  assert activate_code('ADEN-ZZZZ-ZZZZ-ZZZZ', 'devAAAAAAAAAAAAAAAA') = 'invalid', 'unknown code';
  assert activate_code('ADEN-TEST-SUSP-0000', 'devAAAAAAAAAAAAAAAA') = 'invalid', 'suspended code';
  assert activate_code('ADEN-TEST-1111-2222', 'short') = 'invalid', 'bad device id';
  assert activate_code(' aden-test-1111-2222 ', 'devAAAAAAAAAAAAAAAA') = 'ok', 'first activation (normalised)';
  assert activate_code('ADEN-TEST-1111-2222', 'devAAAAAAAAAAAAAAAA') = 'ok', 'same device again';
  assert activate_code('ADEN-TEST-1111-2222', 'devBBBBBBBBBBBBBBBB') = 'other_device', 'second device refused';
  assert verify_code('ADEN-TEST-1111-2222', 'devAAAAAAAAAAAAAAAA') = 'ok', 'verify bound device';
  assert verify_code('ADEN-TEST-1111-2222', 'devBBBBBBBBBBBBBBBB') = 'other_device', 'verify other device';
  assert verify_code('ADEN-TEST-3333-4444', 'devAAAAAAAAAAAAAAAA') = 'unbound', 'verify unused code';
  assert verify_code('ADEN-TEST-SUSP-0000', 'devAAAAAAAAAAAAAAAA') = 'invalid', 'verify suspended';
  assert verify_code('NOPE-NOPE', 'devAAAAAAAAAAAAAAAA') = 'invalid', 'verify unknown';
end $$;

-- throttling: 10 failures, then even a valid code is refused
do $$ declare i int; begin
  for i in 1..10 loop perform activate_code('ADEN-ZZZZ-ZZZZ-ZZZZ', 'devCCCCCCCCCCCCCCCC'); end loop;
  assert activate_code('ADEN-TEST-5555-6666', 'devCCCCCCCCCCCCCCCC') = 'locked', 'throttled';
  assert activate_code('ADEN-TEST-5555-6666', 'devDDDDDDDDDDDDDDDD') = 'ok', 'other devices unaffected';
end $$;

-- anon can never read or write the sensitive tables, nor call admin functions
do $$ begin
  begin perform 1 from access_codes; assert false, 'anon read codes'; exception when insufficient_privilege then null; end;
  begin update access_codes set is_active = false; assert false, 'anon update codes'; exception when insufficient_privilege then null; end;
  begin perform 1 from admins; assert false, 'anon read admins'; exception when insufficient_privilege then null; end;
  begin perform save_config('{}'::jsonb); assert false, 'anon save_config'; exception when insufficient_privilege then null; end;
  begin perform publish_question('x', '{}'::jsonb, false); assert false, 'anon publish'; exception when insufficient_privilege then null; end;
  begin insert into app_config(id) values ('other'); assert false, 'anon write config'; exception when insufficient_privilege then null; end;
  assert (select count(*) from app_config) = 1, 'anon reads config';
  assert (select count(*) from question_patches) = 0, 'anon reads patches';
end $$;
reset role;

-- a signed-in user who is NOT an admin sees nothing and changes nothing
set role authenticated;
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000002', false);
do $$ begin
  assert (select count(*) from access_codes) = 0, 'non-admin sees no codes';
  update access_codes set is_active = false;
  begin perform save_config('{}'::jsonb); assert false, 'non-admin save_config'; exception when insufficient_privilege then null; end;
  begin insert into access_codes(code) values ('ADEN-HACK-0000'); assert false, 'non-admin insert'; exception when insufficient_privilege or check_violation or others then null; end;
end $$;
reset role;
do $$ begin assert (select count(*) from access_codes where is_active) = 3, 'non-admin update changed nothing'; end $$;

-- the admin can do everything
set role authenticated;
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000001', false);
do $$ declare v int; begin
  assert is_admin(), 'is_admin';
  assert (select count(*) from access_codes) = 4, 'admin lists codes';
  insert into access_codes(code, user_name) values ('ADEN-NEW0-0000-0001', 'جديد');
  update access_codes set is_used = false, device_id = null, activated_at = null where code = 'ADEN-TEST-1111-2222';  -- reset device
  update access_codes set is_active = false where code = 'ADEN-TEST-3333-4444';
  delete from access_codes where code = 'ADEN-NEW0-0000-0001';
  v := save_config('{"announcement":"مرحبا","maintenance_mode":true,"hidden_colleges":["law","cs_it"],"default_exam_minutes":30}'::jsonb);
  assert v = 1, 'save_config bumps version';
  assert publish_question('a_1', '{"q":"س"}'::jsonb, false) = 2, 'publish bumps version';
  assert publish_question('a_1', '{"q":"س2"}'::jsonb, false) = 3, 'republish bumps again';
  assert publish_question('u_9', '{}'::jsonb, true) = 4, 'delete patch';
  assert (select hidden_colleges from app_config) = array['law','cs_it'], 'hidden colleges saved';
  assert (select announcement from app_config) = 'مرحبا', 'announcement saved';
end $$;
reset role;

-- after the reset the old phone is unbound and the code can move to a new device
set role anon;
do $$ begin
  assert verify_code('ADEN-TEST-1111-2222', 'devAAAAAAAAAAAAAAAA') = 'unbound', 'old device unbound after reset';
  assert activate_code('ADEN-TEST-1111-2222', 'devBBBBBBBBBBBBBBBB') = 'ok', 'new device activates';
  assert verify_code('ADEN-TEST-3333-4444', 'devAAAAAAAAAAAAAAAA') = 'invalid', 'suspended by admin';
  assert (select content_version from app_config) = 4, 'students read the version';
  assert (select count(*) from question_patches where v > 2) = 2, 'students read new patches';
end $$;
reset role;
select 'ALL SQL TESTS PASSED' as result;
