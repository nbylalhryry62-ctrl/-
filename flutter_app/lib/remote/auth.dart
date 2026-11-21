import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'supabase.dart';

class AuthException implements Exception {
  AuthException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// Supabase Auth (email + password) over REST. The session lives in memory only.
class AdminAuth extends ChangeNotifier {
  AdminAuth(this.client, {this.url = SupabaseConfig.url, this.anonKey = SupabaseConfig.anonKey, DateTime Function()? now}) : _now = now ?? DateTime.now;
  final http.Client client;
  final String url, anonKey;
  final DateTime Function() _now;
  String? _accessToken, _refreshToken, email;
  DateTime _expires = DateTime.fromMillisecondsSinceEpoch(0);

  bool get signedIn => _refreshToken != null;

  Future<Map<String, dynamic>> _grant(String type, Map<String, String> body) async {
    final base = url.endsWith('/') ? url.substring(0, url.length - 1) : url;
    http.Response res;
    try {
      res = await client
          .post(Uri.parse('$base/auth/v1/token?grant_type=$type'),
              headers: {'content-type': 'application/json', 'apikey': anonKey}, body: jsonEncode(body))
          .timeout(const Duration(seconds: 15));
    } on Exception {
      throw AuthException('تعذر الاتصال بالإنترنت');
    }
    Map<String, dynamic> j = {};
    try {
      final d = jsonDecode(utf8.decode(res.bodyBytes));
      if (d is Map<String, dynamic>) j = d;
    } catch (_) {}
    if (res.statusCode >= 400) {
      if (res.statusCode == 429) throw AuthException('محاولات كثيرة، حاول لاحقًا');
      final c = '${j['error_code'] ?? j['code'] ?? ''}';
      if (c.contains('banned') || c.contains('disabled')) throw AuthException('هذا الحساب موقوف');
      if (c.contains('not_confirmed')) throw AuthException('البريد الإلكتروني غير مؤكد. أكّده من لوحة Supabase');
      throw AuthException('البريد الإلكتروني أو كلمة المرور غير صحيحة');
    }
    return j;
  }

  void _store(Map<String, dynamic> j) {
    _accessToken = j['access_token'] as String;
    _refreshToken = j['refresh_token'] as String;
    _expires = _now().add(Duration(seconds: (j['expires_in'] as num?)?.toInt() ?? 3600));
  }

  Future<void> signIn(String mail, String password) async {
    _store(await _grant('password', {'email': mail.trim(), 'password': password}));
    email = mail.trim();
    notifyListeners();
  }

  /// A valid access token, refreshed when it is about to expire. Null when signed out.
  Future<String?> token() async {
    if (_refreshToken == null) return null;
    if (_now().isAfter(_expires.subtract(const Duration(minutes: 1)))) {
      _store(await _grant('refresh_token', {'refresh_token': _refreshToken!}));
    }
    return _accessToken;
  }

  void signOut() {
    _accessToken = _refreshToken = email = null;
    notifyListeners();
  }
}
