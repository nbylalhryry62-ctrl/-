import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:aden_portal/access.dart';
import 'package:aden_portal/data.dart';
import 'package:aden_portal/remote/firestore.dart';

/// A tiny fake of the Firestore REST API: documents, updateMask and the updateTime precondition.
class FakeFirestore {
  final docs = <String, Map<String, dynamic>>{}; // path -> decoded fields
  final times = <String, int>{};
  int clock = 1;

  void put(String path, Map<String, dynamic> data) {
    docs[path] = data;
    times[path] = clock++;
  }

  http.Client client() => MockClient((req) async {
        final path = Uri.decodeFull(req.url.path).split('/documents/').last;
        Map<String, dynamic> doc(String p) => {
              'name': 'projects/p/databases/(default)/documents/$p',
              'fields': encodeFields(docs[p]!),
              'updateTime': '2026-01-01T00:00:${times[p]!.toString().padLeft(2, '0')}Z',
            };
        if (req.method == 'GET') {
          if (!docs.containsKey(path)) return http.Response(jsonEncode({'error': {'code': 404, 'status': 'NOT_FOUND', 'message': 'nf'}}), 404);
          return http.Response(jsonEncode(doc(path)), 200);
        }
        if (req.method == 'PATCH') {
          final pre = req.url.queryParameters['currentDocument.updateTime'];
          if (pre != null && (!docs.containsKey(path) || doc(path)['updateTime'] != pre)) {
            return http.Response(jsonEncode({'error': {'code': 400, 'status': 'FAILED_PRECONDITION', 'message': 'changed'}}), 400);
          }
          final mask = req.url.queryParametersAll['updateMask.fieldPaths'];
          final fields = decodeFields((jsonDecode(req.body)['fields'] as Map).cast<String, dynamic>());
          final cur = Map<String, dynamic>.from(docs[path] ?? {});
          if (mask == null) {
            cur
              ..clear()
              ..addAll(fields);
          } else {
            for (final k in mask) {
              cur[k] = fields[k];
            }
          }
          put(path, cur);
          return http.Response(jsonEncode(doc(path)), 200);
        }
        return http.Response('{}', 400);
      });
}

Map<String, dynamic> code({bool used = false, bool active = true, String? dev}) =>
    {'code': 'X', 'isUsed': used, 'isActive': active, 'deviceId': dev, 'userName': '', 'createdAt': DateTime.utc(2026), 'activatedAt': null};

AccessService svc(FakeFirestore f, MemoryKv kv, {DateTime Function()? now}) =>
    AccessService(Firestore(f.client(), apiKey: 'k', projectId: 'p'), kv, now: now);

void main() {
  const c1 = 'ADEN-ABCD-EFGH-JKLM';

  test('judge covers every case', () {
    expect(judge(null, 'd'), Verdict.invalid);
    expect(judge(code(active: false), 'd'), Verdict.invalid);
    expect(judge(code(), 'd'), Verdict.activate);
    expect(judge(code(used: true, dev: 'd'), 'd'), Verdict.ok);
    expect(judge(code(used: true, dev: 'other'), 'd'), Verdict.otherDevice);
  });

  test('codes are random, well formed and normalised', () {
    final seen = {for (var i = 0; i < 200; i++) generateCode()};
    expect(seen.length, 200);
    expect(seen.every((c) => RegExp(r'^ADEN-[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}$').hasMatch(c)), isTrue);
    expect(normalizeCode(' aden abcd efgh jklm '), c1);
    expect(normalizeCode('adenabcdefghjklm'), c1);
    expect(normalizeCode('ADEN-٢٣٤٥-EFGH-JKLM'), 'ADEN-2345-EFGH-JKLM');
  });

  test('unknown, inactive codes are refused with a clear message', () async {
    final f = FakeFirestore()..put('access_codes/$c1', code(active: false));
    final s = svc(f, MemoryKv());
    await expectLater(s.activate('ADEN-ZZZZ-ZZZZ-ZZZZ'), throwsA(isA<ActivationException>().having((e) => e.message, 'm', msgInvalid)));
    await expectLater(s.activate(c1), throwsA(isA<ActivationException>().having((e) => e.message, 'm', msgInvalid)));
  });

  test('first use binds the device; the same device may re-enter; another is refused', () async {
    final f = FakeFirestore()..put('access_codes/$c1', code());
    final kvA = MemoryKv(), kvB = MemoryKv();
    await svc(f, kvA).activate(c1);
    expect(f.docs['access_codes/$c1']!['isUsed'], true);
    final devA = await svc(f, kvA).deviceId();
    expect(f.docs['access_codes/$c1']!['deviceId'], devA);
    expect(await svc(f, kvA).localState(), isNotNull);
    await svc(f, kvA).activate(c1); // same phone again
    await expectLater(svc(f, kvB).activate(c1), throwsA(isA<ActivationException>().having((e) => e.message, 'm', msgOtherDevice)));
    expect(await svc(f, kvB).localState(), isNull);
  });

  test('two phones racing for one code: exactly one wins', () async {
    final f = FakeFirestore()..put('access_codes/$c1', code());
    final a = svc(f, MemoryKv()), b = svc(f, MemoryKv());
    final r = await Future.wait([
      a.activate(c1).then((_) => true).catchError((_) => false),
      b.activate(c1).then((_) => true).catchError((_) => false),
    ]);
    expect(r.where((x) => x).length, 1);
  });

  test('background check locks when the admin suspends, resets or deletes the code', () async {
    final f = FakeFirestore()..put('access_codes/$c1', code());
    final kv = MemoryKv();
    final s = svc(f, kv);
    await s.activate(c1);
    expect((await s.verify()).kind, VerifyKind.ok);

    f.docs['access_codes/$c1']!['isActive'] = false;
    var r = await s.verify();
    expect(r.kind, VerifyKind.revoked);
    expect(await s.localState(), isNull);

    f.docs['access_codes/$c1']!['isActive'] = true;
    await s.activate(c1); // works again: it is still bound to this device
    f.docs['access_codes/$c1']!..['isUsed'] = false..['deviceId'] = null; // admin "reset device"
    r = await s.verify();
    expect(r.kind, VerifyKind.revoked);
    expect(r.message, msgUnbound);

    await s.activate(c1);
    f.docs.remove('access_codes/$c1');
    expect((await s.verify()).kind, VerifyKind.revoked);
  });

  test('offline keeps the app working, but not past the grace period', () async {
    final f = FakeFirestore()..put('access_codes/$c1', code());
    final kv = MemoryKv();
    var t = DateTime(2026, 1, 1);
    await svc(f, kv, now: () => t).activate(c1);
    final offline = AccessService(Firestore(MockClient((_) async => throw Exception('no net')), apiKey: 'k', projectId: 'p'), kv, now: () => t);
    expect((await offline.verify()).kind, VerifyKind.offline);
    t = t.add(const Duration(days: 31));
    expect((await offline.verify()).kind, VerifyKind.expired);
  });

  test('five wrong tries trigger a wait', () async {
    final f = FakeFirestore();
    final s = svc(f, MemoryKv());
    for (var i = 0; i < 5; i++) {
      await expectLater(s.activate('ADEN-ZZZZ-ZZZZ-ZZZ$i'), throwsA(isA<ActivationException>()));
    }
    await expectLater(s.activate('ADEN-ZZZZ-ZZZZ-ZZZZ'), throwsA(isA<ActivationException>().having((e) => e.message, 'm', contains('انتظر'))));
  });

  test('firestore value codec round-trips', () {
    final m = {'a': 1, 'b': 'x', 'c': true, 'd': null, 'e': ['p', 'q'], 'f': DateTime.utc(2026, 1, 2), 'g': 2.5};
    expect(decodeFields(encodeFields(m)), m);
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
