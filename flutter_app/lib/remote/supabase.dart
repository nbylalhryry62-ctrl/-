import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;

/// Supabase project settings, injected at build time:
/// flutter build apk --dart-define=SUPABASE_URL=https://xxxx.supabase.co --dart-define=SUPABASE_ANON_KEY=...
/// The anon / publishable key is public by design; security is enforced by the database (see supabase/migration.sql).
/// When they are empty the app runs open (no activation gate, no remote sync).
class SupabaseConfig {
  static const url = String.fromEnvironment('SUPABASE_URL');
  static const anonKey = String.fromEnvironment('SUPABASE_ANON_KEY');
  static bool get enabled => url.isNotEmpty && anonKey.isNotEmpty;
}

class RemoteException implements Exception {
  RemoteException(this.status, this.message, [this.code = '']);
  final int status;
  final String message, code;
  bool get isNetwork => status == 0;
  bool get isConflict => status == 409 || code == '23505';
  bool get isDenied => status == 401 || status == 403 || code == '42501';
  @override
  String toString() => 'RemoteException($status $code): $message';
}

typedef TokenProvider = Future<String?> Function();

/// Minimal PostgREST client (no Supabase SDK, so it builds anywhere).
class Backend {
  Backend(this.client, {this.url = SupabaseConfig.url, this.anonKey = SupabaseConfig.anonKey, this.token});
  final http.Client client;
  final String url, anonKey;
  final TokenProvider? token;

  String get _base => url.endsWith('/') ? url.substring(0, url.length - 1) : url;

  Future<dynamic> _send(String method, String path, {Map<String, String>? query, Object? body, Map<String, String>? headers}) async {
    final uri = Uri.parse('$_base/rest/v1/$path').replace(queryParameters: query == null || query.isEmpty ? null : query);
    final req = http.Request(method, uri)
      ..headers['apikey'] = anonKey
      ..headers['content-type'] = 'application/json'
      ..headers.addAll(headers ?? const {});
    final t = token == null ? null : await token!();
    // New-style publishable keys are not JWTs and must not be sent as a bearer token.
    if (t != null) {
      req.headers['authorization'] = 'Bearer $t';
    } else if (anonKey.startsWith('eyJ')) {
      req.headers['authorization'] = 'Bearer $anonKey';
    }
    if (body != null) req.body = jsonEncode(body);
    http.Response res;
    try {
      res = await http.Response.fromStream(await client.send(req).timeout(const Duration(seconds: 15)));
    } on Exception catch (e) {
      throw RemoteException(0, 'network: $e');
    }
    final text = utf8.decode(res.bodyBytes);
    dynamic json;
    if (text.trim().isNotEmpty) {
      try {
        json = jsonDecode(text);
      } catch (_) {}
    }
    if (res.statusCode >= 400) {
      final m = json is Map ? json : const {};
      throw RemoteException(res.statusCode, '${m['message'] ?? text}', '${m['code'] ?? ''}');
    }
    return json;
  }

  /// Calls a Postgres function. Returns the decoded JSON result (a string for text functions).
  Future<dynamic> rpc(String fn, Map<String, dynamic> args) => _send('POST', 'rpc/$fn', body: args);

  Future<List<Map<String, dynamic>>> select(String table, {Map<String, String>? query}) async {
    final j = await _send('GET', table, query: {'select': '*', ...?query});
    return ((j as List?) ?? const []).cast<Map<String, dynamic>>();
  }

  /// Reads every row, page by page (PostgREST caps one response at 1000 rows by default).
  Future<List<Map<String, dynamic>>> selectAll(String table, {Map<String, String>? query, int pageSize = 1000}) async {
    final out = <Map<String, dynamic>>[];
    for (var from = 0;; from += pageSize) {
      final j = await _send('GET', table,
          query: {'select': '*', ...?query},
          headers: {'range-unit': 'items', 'range': '$from-${from + pageSize - 1}'});
      final rows = ((j as List?) ?? const []).cast<Map<String, dynamic>>();
      out.addAll(rows);
      if (rows.length < pageSize) return out;
    }
  }

  Future<void> insert(String table, Object rows) =>
      _send('POST', table, body: rows, headers: {'prefer': 'return=minimal'});

  Future<void> update(String table, Map<String, String> filter, Map<String, dynamic> data) =>
      _send('PATCH', table, query: filter, body: data, headers: {'prefer': 'return=minimal'});

  Future<void> delete(String table, Map<String, String> filter) =>
      _send('DELETE', table, query: filter, headers: {'prefer': 'return=minimal'});
}
