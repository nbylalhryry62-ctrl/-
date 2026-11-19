import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'firestore.dart';

class AuthException implements Exception {
  AuthException(this.message);
  final String message;
  @override
  String toString() => message;
}

/// Firebase Auth (email + password) over REST. The session lives in memory only.
class AdminAuth extends ChangeNotifier {
  AdminAuth(this.client, {this.apiKey = FirebaseConfig.apiKey, DateTime Function()? now}) : _now = now ?? DateTime.now;
  final http.Client client;
  final String apiKey;
  final DateTime Function() _now;
  String? _idToken, _refreshToken, email;
  DateTime _expires = DateTime.fromMillisecondsSinceEpoch(0);

  bool get signedIn => _refreshToken != null;

  Future<Map<String, dynamic>> _post(String url, Object body, {bool form = false}) async {
    http.Response res;
    try {
      res = await client
          .post(Uri.parse(url), headers: {'content-type': form ? 'application/x-www-form-urlencoded' : 'application/json'}, body: form ? body : jsonEncode(body))
          .timeout(const Duration(seconds: 15));
    } on Exception {
      throw AuthException('تعذر الاتصال بالإنترنت');
    }
    final j = jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
    if (res.statusCode >= 400) {
      final m = '${(j['error'] as Map?)?['message'] ?? ''}';
      if (m.contains('TOO_MANY')) throw AuthException('محاولات كثيرة، حاول لاحقًا');
      if (m.contains('USER_DISABLED')) throw AuthException('هذا الحساب موقوف');
      throw AuthException('البريد الإلكتروني أو كلمة المرور غير صحيحة');
    }
    return j;
  }

  void _store(String id, String refresh, String expiresIn) {
    _idToken = id;
    _refreshToken = refresh;
    _expires = _now().add(Duration(seconds: int.tryParse(expiresIn) ?? 3600));
  }

  Future<void> signIn(String mail, String password) async {
    final j = await _post('https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$apiKey',
        {'email': mail.trim(), 'password': password, 'returnSecureToken': true});
    _store(j['idToken'] as String, j['refreshToken'] as String, '${j['expiresIn']}');
    email = mail.trim();
    notifyListeners();
  }

  /// A valid ID token, refreshed when it is about to expire. Null when signed out.
  Future<String?> token() async {
    if (_refreshToken == null) return null;
    if (_now().isAfter(_expires.subtract(const Duration(minutes: 1)))) {
      final j = await _post('https://securetoken.googleapis.com/v1/token?key=$apiKey', 'grant_type=refresh_token&refresh_token=$_refreshToken', form: true);
      _store(j['id_token'] as String, j['refresh_token'] as String, '${j['expires_in']}');
    }
    return _idToken;
  }

  void signOut() {
    _idToken = _refreshToken = email = null;
    notifyListeners();
  }
}
