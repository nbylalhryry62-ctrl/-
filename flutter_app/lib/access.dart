import 'dart:async';
import 'dart:convert';
import 'dart:math';
import 'package:flutter/widgets.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'remote/firestore.dart';

abstract class SecureKv {
  Future<String?> read(String k);
  Future<void> write(String k, String v);
  Future<void> delete(String k);
}

class FlutterSecureKv implements SecureKv {
  final _s = const FlutterSecureStorage();
  @override
  Future<String?> read(String k) => _s.read(key: k);
  @override
  Future<void> write(String k, String v) => _s.write(key: k, value: v);
  @override
  Future<void> delete(String k) => _s.delete(key: k);
}

class MemoryKv implements SecureKv {
  final Map<String, String> m = {};
  @override
  Future<String?> read(String k) async => m[k];
  @override
  Future<void> write(String k, String v) async => m[k] = v;
  @override
  Future<void> delete(String k) async => m.remove(k);
}

enum Verdict { ok, activate, invalid, otherDevice }

const msgInvalid = 'الرمز غير صحيح أو موقوف';
const msgOtherDevice = 'هذا الرمز مستخدم مسبقاً على جهاز آخر';
const msgUnbound = 'تم فك ارتباط هذا الجهاز بالرمز. أدخل الرمز من جديد';

/// The one rule that decides access, kept pure so it can be tested.
Verdict judge(Map<String, dynamic>? doc, String deviceId) {
  if (doc == null || doc['isActive'] != true) return Verdict.invalid;
  if (doc['isUsed'] != true) return Verdict.activate;
  return doc['deviceId'] == deviceId ? Verdict.ok : Verdict.otherDevice;
}

const _alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; // 32 symbols, no 0/O/1/I

/// ADEN-XXXX-XXXX-XXXX with 60 bits of randomness.
String generateCode([Random? rnd]) {
  final r = rnd ?? Random.secure();
  String group() => List.generate(4, (_) => _alphabet[r.nextInt(32)]).join();
  return 'ADEN-${group()}-${group()}-${group()}';
}

/// Uppercases, maps Arabic digits and odd dashes, and inserts dashes for a bare 16-character code.
String normalizeCode(String raw) {
  const ar = '٠١٢٣٤٥٦٧٨٩';
  var s = raw.trim().toUpperCase();
  for (var i = 0; i < 10; i++) {
    s = s.replaceAll(ar[i], '$i');
  }
  s = s.replaceAll(RegExp(r'[\s_‐-―]+'), '-');
  if (RegExp(r'^ADEN[A-Z0-9]{12}$').hasMatch(s)) {
    s = 'ADEN-${s.substring(4, 8)}-${s.substring(8, 12)}-${s.substring(12)}';
  }
  return s;
}

bool validCodeFormat(String c) => RegExp(r'^[A-Z0-9][A-Z0-9-]{5,39}$').hasMatch(c);

class ActivationException implements Exception {
  ActivationException(this.message);
  final String message;
  @override
  String toString() => message;
}

enum VerifyKind { ok, offline, revoked, expired }

class VerifyResult {
  const VerifyResult(this.kind, [this.message]);
  final VerifyKind kind;
  final String? message;
}

class AccessService {
  AccessService(this.fs, this.kv, {DateTime Function()? now, this.offlineGrace = const Duration(days: 30)}) : _now = now ?? DateTime.now;
  final Firestore fs;
  final SecureKv kv;
  final DateTime Function() _now;
  final Duration offlineGrace;

  static const _kState = 'aden.access';
  static const _kDevice = 'aden.device';
  static const _kFails = 'aden.fails';

  /// Random per-install identifier kept in secure storage.
  Future<String> deviceId() async {
    var id = await kv.read(_kDevice);
    if (id == null || id.length < 16) {
      final r = Random.secure();
      id = List.generate(32, (_) => r.nextInt(16).toRadixString(16)).join();
      await kv.write(_kDevice, id);
    }
    return id;
  }

  Future<Map<String, dynamic>?> localState() async {
    try {
      final raw = await kv.read(_kState);
      return raw == null ? null : jsonDecode(raw) as Map<String, dynamic>;
    } catch (_) {
      return null;
    }
  }

  Future<void> _saveLocal(String code) =>
      kv.write(_kState, jsonEncode({'code': code, 'verifiedAt': _now().millisecondsSinceEpoch}));

  // Slow down guessing: 5 wrong tries, then a growing wait.
  Future<void> _checkLock() async {
    final j = jsonDecode(await kv.read(_kFails) ?? '{}') as Map<String, dynamic>;
    final until = (j['until'] as int?) ?? 0;
    final left = ((until - _now().millisecondsSinceEpoch) / 1000).ceil();
    if (left > 0) throw ActivationException('محاولات كثيرة. انتظر $left ثانية ثم أعد المحاولة');
  }

  Future<ActivationException> _fail(String msg) async {
    final j = jsonDecode(await kv.read(_kFails) ?? '{}') as Map<String, dynamic>;
    final n = ((j['n'] as int?) ?? 0) + 1;
    final wait = n >= 5 ? min(900, 30 * (n - 4)) : 0;
    await kv.write(_kFails, jsonEncode({'n': n, 'until': _now().millisecondsSinceEpoch + wait * 1000}));
    return ActivationException(msg);
  }

  /// Activates a code on this device. Throws [ActivationException] with an Arabic message.
  Future<void> activate(String raw) async {
    await _checkLock();
    final code = normalizeCode(raw);
    if (!validCodeFormat(code)) throw await _fail(msgInvalid);
    final dev = await deviceId();
    try {
      var doc = await fs.get('access_codes/$code');
      for (var attempt = 0; attempt < 3; attempt++) {
        switch (judge(doc?.data, dev)) {
          case Verdict.invalid:
            throw await _fail(msgInvalid);
          case Verdict.otherDevice:
            throw await _fail(msgOtherDevice);
          case Verdict.ok:
            await _saveLocal(code);
            await kv.delete(_kFails);
            return;
          case Verdict.activate:
            try {
              // The update time is a precondition: if two phones race, only one write wins.
              await fs.patch('access_codes/$code', {'isUsed': true, 'deviceId': dev, 'activatedAt': _now().toUtc()}, ifUpdateTime: doc!.updateTime);
              await _saveLocal(code);
              await kv.delete(_kFails);
              return;
            } on FsException catch (e) {
              if (!e.isConflict) rethrow;
              doc = await fs.get('access_codes/$code');
            }
        }
      }
      throw await _fail(msgOtherDevice);
    } on FsException catch (e) {
      if (e.isNetwork) throw ActivationException('تعذر الاتصال بالإنترنت. التفعيل لأول مرة يحتاج اتصالًا');
      if (e.isDenied) throw ActivationException('تعذر التحقق من الرمز (صلاحيات الخادم). راجع المشرف');
      throw ActivationException('حدث خطأ غير متوقع، حاول مرة أخرى');
    }
  }

  /// Background check of a locally activated code against the server.
  Future<VerifyResult> verify() async {
    final st = await localState();
    if (st == null) return const VerifyResult(VerifyKind.revoked, msgInvalid);
    final code = st['code'] as String;
    final dev = await deviceId();
    try {
      final doc = await fs.get('access_codes/$code');
      switch (judge(doc?.data, dev)) {
        case Verdict.ok:
          await _saveLocal(code);
          return const VerifyResult(VerifyKind.ok);
        case Verdict.activate:
          await kv.delete(_kState);
          return const VerifyResult(VerifyKind.revoked, msgUnbound);
        case Verdict.otherDevice:
          await kv.delete(_kState);
          return const VerifyResult(VerifyKind.revoked, msgOtherDevice);
        case Verdict.invalid:
          await kv.delete(_kState);
          return const VerifyResult(VerifyKind.revoked, 'تم إيقاف هذا الرمز من قبل المشرف');
      }
    } on FsException {
      // Offline or server trouble: keep working, but not forever without a successful check.
      final last = DateTime.fromMillisecondsSinceEpoch((st['verifiedAt'] as int?) ?? 0);
      if (_now().difference(last) > offlineGrace) {
        await kv.delete(_kState);
        return const VerifyResult(VerifyKind.expired, 'انتهت مهلة العمل دون اتصال. اتصل بالإنترنت لإعادة التحقق');
      }
      return const VerifyResult(VerifyKind.offline);
    }
  }
}

enum AccessStatus { loading, locked, active }

class AccessController extends ChangeNotifier with WidgetsBindingObserver {
  AccessController(this.service);
  final AccessService service;
  AccessStatus status = AccessStatus.loading;
  String? message;
  DateTime? _last;

  Future<void> init() async {
    WidgetsBinding.instance.addObserver(this);
    final st = await service.localState();
    status = st == null ? AccessStatus.locked : AccessStatus.active;
    notifyListeners();
    if (st != null) unawaited(recheck(force: true));
  }

  Future<void> activate(String code) async {
    await service.activate(code);
    status = AccessStatus.active;
    message = null;
    _last = DateTime.now();
    notifyListeners();
  }

  Future<void> recheck({bool force = false}) async {
    if (status != AccessStatus.active) return;
    final now = DateTime.now();
    if (!force && _last != null && now.difference(_last!) < const Duration(minutes: 5)) return;
    _last = now;
    final r = await service.verify();
    if (r.kind == VerifyKind.revoked || r.kind == VerifyKind.expired) {
      status = AccessStatus.locked;
      message = r.message;
      notifyListeners();
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState s) {
    if (s == AppLifecycleState.resumed) recheck();
  }
}
