import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:aden_portal/access.dart';
import 'package:aden_portal/data.dart';
import 'package:aden_portal/remote/admin_api.dart';
import 'package:aden_portal/remote/supabase.dart';
import 'package:aden_portal/sync.dart';

/// A small fake of Supabase (PostgREST + the SQL functions), mirroring supabase/migration.sql.
class FakeSupabase {
  final codes = <String, Map<String, dynamic>>{};
  final patches = <String, Map<String, dynamic>>{};
  var config = <String, dynamic>{
    'id': 'main', 'content_version': 0, 'maintenance_mode': false, 'maintenance_message': '', 'announcement': '',
    'contact_url': '', 'default_exam_minutes': 20, 'hidden_colleges': <String>[],
  };
  bool admin = false;
  int rpcCalls = 0;

  void addCode(String c, {bool used = false, bool active = true, String? dev}) =>
      codes[c] = {'code': c, 'is_used': used, 'is_active': active, 'device_id': dev, 'user_name': '', 'created_at': '2026-01-01T00:00:00Z', 'activated_at': null};

  String _activate(String code, String dev) {
    final r = codes[code.trim().toUpperCase()];
    if (r == null || r['is_active'] != true) return 'invalid';
    if (r['is_used'] == true) return r['device_id'] == dev ? 'ok' : 'other_device';
    r..['is_used'] = true..['device_id'] = dev..['activated_at'] = '2026-01-02T00:00:00Z';
    return 'ok';
  }

  String _verify(String code, String dev) {
    final r = codes[code.trim().toUpperCase()];
    if (r == null || r['is_active'] != true) return 'invalid';
    if (r['is_used'] != true) return 'unbound';
    return r['device_id'] == dev ? 'ok' : 'other_device';
  }

  Map<String, dynamic>? _table(String t) => switch (t) { 'access_codes' => codes, 'question_patches' => patches, _ => null };

  bool _match(Map<String, dynamic> row, Map<String, String> q) {
    for (final e in q.entries) {
      if (e.key == 'select' || e.key == 'order') continue;
      final v = e.value;
      if (v.startsWith('eq.') && '${row[e.key]}' != v.substring(3)) return false;
      if (v.startsWith('gt.') && !((row[e.key] as num) > num.parse(v.substring(3)))) return false;
    }
    return true;
  }

  http.Response _json(Object? o, [int code = 200]) => http.Response(jsonEncode(o), code, headers: {'content-type': 'application/json'});

  http.Client client() => MockClient((req) async {
        final path = req.url.path.split('/rest/v1/').last;
        final q = req.url.queryParameters;
        final body = req.body.isEmpty ? null : jsonDecode(req.body);
        if (path.startsWith('rpc/')) {
          rpcCalls++;
          final a = body as Map<String, dynamic>;
          switch (path.substring(4)) {
            case 'activate_code':
              return _json(_activate(a['p_code'] as String, a['p_device'] as String));
            case 'verify_code':
              return _json(_verify(a['p_code'] as String, a['p_device'] as String));
            case 'is_admin':
              return _json(admin);
            case 'save_config':
              if (!admin) return _json({'code': '42501', 'message': 'forbidden'}, 403);
              final p = a['p'] as Map<String, dynamic>;
              config = {...config, ...p, 'content_version': (config['content_version'] as int) + 1};
              return _json(config['content_version']);
            case 'publish_question':
              if (!admin) return _json({'code': '42501', 'message': 'forbidden'}, 403);
              final v = (config['content_version'] as int) + 1;
              config['content_version'] = v;
              patches[a['p_id'] as String] = {'id': a['p_id'], 'v': v, 'deleted': a['p_deleted'], 'data': a['p_data']};
              return _json(v);
          }
          return _json({'message': 'no such function'}, 404);
        }
        if (path == 'app_config') return _json([config]);
        final t = _table(path);
        if (t == null) return _json({'message': 'no table'}, 404);
        switch (req.method) {
          case 'GET':
            return _json(t.values.where((r) => _match(r as Map<String, dynamic>, q)).toList());
          case 'POST':
            final row = (body as Map<String, dynamic>);
            final key = (row['code'] ?? row['id']) as String;
            if (t.containsKey(key)) return _json({'code': '23505', 'message': 'duplicate key'}, 409);
            t[key] = {'is_used': false, 'is_active': true, 'device_id': null, 'user_name': '', 'created_at': '2026-01-03T00:00:00Z', 'activated_at': null, ...row};
            return http.Response('', 201);
          case 'PATCH':
            for (final r in t.values) {
              if (_match(r as Map<String, dynamic>, q)) r.addAll(body as Map<String, dynamic>);
            }
            return http.Response('', 204);
          case 'DELETE':
            t.removeWhere((k, r) => _match(r as Map<String, dynamic>, q));
            return http.Response('', 204);
        }
        return _json({}, 400);
      });
}

AccessService svc(FakeSupabase f, MemoryKv kv, {DateTime Function()? now}) =>
    AccessService(Backend(f.client(), url: 'https://x.supabase.co', anonKey: 'k'), kv, now: now);

void main() {
  const c1 = 'ADEN-ABCD-EFGH-JKLM';

  test('codes are random, well formed and normalised', () {
    final seen = {for (var i = 0; i < 200; i++) generateCode()};
    expect(seen.length, 200);
    expect(seen.every((c) => RegExp(r'^ADEN-[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$').hasMatch(c)), isTrue);
    expect(normalizeCode(' aden abcd efgh jklm '), c1);
    expect(normalizeCode('adenabcdefghjklm'), c1);
    expect(normalizeCode('ADEN-٢٣٤٥-EFGH-JKLM'), 'ADEN-2345-EFGH-JKLM');
  });

  test('unknown, inactive codes are refused with a clear message', () async {
    final f = FakeSupabase()..addCode(c1, active: false);
    final s = svc(f, MemoryKv());
    await expectLater(s.activate('ADEN-ZZZZ-ZZZZ-ZZZZ'), throwsA(isA<ActivationException>().having((e) => e.message, 'm', msgInvalid)));
    await expectLater(s.activate(c1), throwsA(isA<ActivationException>().having((e) => e.message, 'm', msgInvalid)));
  });

  test('first use binds the device; the same device may re-enter; another is refused', () async {
    final f = FakeSupabase()..addCode(c1);
    final kvA = MemoryKv(), kvB = MemoryKv();
    await svc(f, kvA).activate(c1);
    expect(f.codes[c1]!['is_used'], true);
    final devA = await svc(f, kvA).deviceId();
    expect(f.codes[c1]!['device_id'], devA);
    expect(await svc(f, kvA).localState(), isNotNull);
    await svc(f, kvA).activate(c1); // same phone again
    await expectLater(svc(f, kvB).activate(c1), throwsA(isA<ActivationException>().having((e) => e.message, 'm', msgOtherDevice)));
    expect(await svc(f, kvB).localState(), isNull);
  });

  test('background check locks when the admin suspends, resets or deletes the code', () async {
    final f = FakeSupabase()..addCode(c1);
    final kv = MemoryKv();
    final s = svc(f, kv);
    await s.activate(c1);
    expect((await s.verify()).kind, VerifyKind.ok);

    f.codes[c1]!['is_active'] = false;
    var r = await s.verify();
    expect(r.kind, VerifyKind.revoked);
    expect(await s.localState(), isNull);

    f.codes[c1]!['is_active'] = true;
    await s.activate(c1); // works again: it is still bound to this device
    f.codes[c1]!..['is_used'] = false..['device_id'] = null; // admin "reset device"
    r = await s.verify();
    expect(r.kind, VerifyKind.revoked);
    expect(r.message, msgUnbound);

    await s.activate(c1);
    f.codes.remove(c1);
    expect((await s.verify()).kind, VerifyKind.revoked);
  });

  test('offline keeps the app working, but not past the grace period', () async {
    final f = FakeSupabase()..addCode(c1);
    final kv = MemoryKv();
    var t = DateTime(2026, 1, 1);
    await svc(f, kv, now: () => t).activate(c1);
    final offline = AccessService(Backend(MockClient((_) async => throw Exception('no net')), url: 'https://x.supabase.co', anonKey: 'k'), kv, now: () => t);
    expect((await offline.verify()).kind, VerifyKind.offline);
    t = t.add(const Duration(days: 31));
    expect((await offline.verify()).kind, VerifyKind.expired);
  });

  test('five wrong tries trigger a wait', () async {
    final f = FakeSupabase();
    final s = svc(f, MemoryKv());
    for (var i = 0; i < 5; i++) {
      await expectLater(s.activate('ADEN-ZZZZ-ZZZZ-ZZZ$i'), throwsA(isA<ActivationException>()));
    }
    await expectLater(s.activate('ADEN-ZZZZ-ZZZZ-ZZZZ'), throwsA(isA<ActivationException>().having((e) => e.message, 'm', contains('انتظر'))));
  });

  test('a second phone entering the same code is refused even with spaces and lowercase', () async {
    final f = FakeSupabase()..addCode(c1);
    await svc(f, MemoryKv()).activate(' aden-abcd-efgh-jklm ');
    await expectLater(svc(f, MemoryKv()).activate(c1), throwsA(isA<ActivationException>().having((e) => e.message, 'm', msgOtherDevice)));
  });

  test('malformed codes never reach the server', () async {
    final f = FakeSupabase();
    await expectLater(svc(f, MemoryKv()).activate('x'), throwsA(isA<ActivationException>()));
    expect(f.rpcCalls, 0);
  });

  test('network failure while activating gives an Arabic message', () async {
    final s = AccessService(Backend(MockClient((_) async => throw Exception('no net')), url: 'https://x.supabase.co', anonKey: 'k'), MemoryKv());
    await expectLater(s.activate(c1), throwsA(isA<ActivationException>().having((e) => e.message, 'm', contains('الاتصال'))));
  });

  test('admin api: codes, suspend, reset, delete, duplicate detection', () async {
    final f = FakeSupabase()..admin = true;
    final api = AdminApi(Backend(f.client(), url: 'https://x.supabase.co', anonKey: 'k'));
    expect(await api.isAdmin(), isTrue);
    await api.createCode(c1, 'سالم');
    await expectLater(api.createCode(c1, ''), throwsA(isA<RemoteException>().having((e) => e.isConflict, 'conflict', true)));
    var l = await api.codes();
    expect(l.single.userName, 'سالم');
    expect(l.single.status, CodeStatus.available);
    await svc(f, MemoryKv()).activate(c1);
    expect((await api.codes()).single.status, CodeStatus.used);
    await api.setActive(c1, false);
    expect((await api.codes()).single.status, CodeStatus.suspended);
    await api.setActive(c1, true);
    await api.resetDevice(c1);
    final r = (await api.codes()).single;
    expect(r.isUsed, false);
    expect(r.deviceId, isNull);
    await api.deleteCode(c1);
    expect(await api.codes(), isEmpty);
  });

  test('a non-admin is not an admin and cannot publish', () async {
    final f = FakeSupabase();
    final api = AdminApi(Backend(f.client(), url: 'https://x.supabase.co', anonKey: 'k'));
    expect(await api.isAdmin(), isFalse);
    await expectLater(api.saveConfig(const RemoteConfig()), throwsA(isA<RemoteException>().having((e) => e.isDenied, 'denied', true)));
  });

  test('settings and question edits reach a student device through ContentSync', () async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    final f = FakeSupabase()..admin = true;
    final db = Backend(f.client(), url: 'https://x.supabase.co', anonKey: 'k');
    final api = AdminApi(db);
    Question q(String id, String fid) => Question(id, fid, 'T$fid', 'مادة', 'وحدة', 'سهل', 'ar', 'س $id', ['أ', 'ب'], 0, '');
    final bank = Bank.forTest([q('1', 'law'), q('2', 'law')]);
    final sync = ContentSync(db, prefs, bank)..loadLocal();

    await api.saveConfig(const RemoteConfig(announcement: 'مرحبا', maintenanceMode: true, maintenanceMessage: 'صيانة', defaultExamMinutes: 30, hiddenColleges: ['cs_it']));
    await api.publishQuestion(q('a_9', 'law'));
    await api.deleteQuestion('2');
    expect(await sync.refresh(), isTrue);
    expect(sync.config.announcement, 'مرحبا');
    expect(sync.config.maintenanceMode, isTrue);
    expect(sync.config.defaultExamMinutes, 30);
    expect(sync.localVersion, 3);
    expect(bank.all.map((e) => e.id), ['1', 'a_9']);
    expect(bank.hidden, {'cs_it'});
    expect(await sync.refresh(), isFalse); // nothing new

    // a restarted app starts from the cache, offline
    final again = ContentSync(Backend(MockClient((_) async => throw Exception('no net')), url: 'https://x.supabase.co', anonKey: 'k'), prefs, Bank.forTest([q('1', 'law'), q('2', 'law')]))..loadLocal();
    expect(again.config.announcement, 'مرحبا');
    expect(again.bank.all.map((e) => e.id), ['1', 'a_9']);
    expect(await again.refresh(), isFalse);
  });

  test('remote patches add, edit and delete questions; hidden faculties vanish', () {
    Question q(String id, String f) => Question(id, f, 'T$f', 'مادة', 'وحدة', 'سهل', 'ar', 'س $id', ['أ', 'ب'], 0, '');
    final bank = Bank.forTest([q('1', 'law'), q('2', 'law'), q('3', 'cs_it')]);
    bank.applyPatches({
      '2': {'deleted': true},
      '1': {'f': 'law', 'ft': 'Tlaw', 's': 'م', 'u': 'و', 'd': 'صعب', 'l': 'ar', 'q': 'معدّل', 'o': ['أ', 'ب'], 'c': 1, 'e': ''},
      'a_new': {'f': 'law', 'ft': 'Tlaw', 's': 'م', 'u': 'و', 'd': 'سهل', 'l': 'ar', 'q': 'جديد', 'o': ['1', '2'], 'c': 0, 'e': ''},
    });
    expect(bank.all.map((e) => e.id), ['1', '3', 'a_new']);
    expect(bank.byId['1']!.text, 'معدّل');
    bank.hidden = {'law'};
    expect(bank.query().map((e) => e.id), ['3']);
    expect(bank.query(includeHidden: true).length, 3);
  });
}
