import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;

/// Firebase project settings, injected at build time:
/// flutter build apk --dart-define=FIREBASE_API_KEY=... --dart-define=FIREBASE_PROJECT_ID=...
/// When they are empty the app runs open (no activation gate, no remote sync).
class FirebaseConfig {
  static const apiKey = String.fromEnvironment('FIREBASE_API_KEY');
  static const projectId = String.fromEnvironment('FIREBASE_PROJECT_ID');
  static bool get enabled => apiKey.isNotEmpty && projectId.isNotEmpty;
}

class FsException implements Exception {
  FsException(this.status, this.message, [this.code = '']);
  final int status;
  final String message, code;
  bool get isNetwork => status == 0;
  bool get isConflict => status == 409 || code == 'FAILED_PRECONDITION' || code == 'ABORTED';
  bool get isDenied => status == 401 || status == 403;
  @override
  String toString() => 'FsException($status $code): $message';
}

class FsDoc {
  FsDoc(this.id, this.data, this.updateTime);
  final String id;
  final Map<String, dynamic> data;
  final String updateTime;
}

Map<String, dynamic> encodeValue(dynamic x) {
  if (x == null) return {'nullValue': null};
  if (x is bool) return {'booleanValue': x};
  if (x is int) return {'integerValue': '$x'};
  if (x is double) return {'doubleValue': x};
  if (x is String) return {'stringValue': x};
  if (x is DateTime) return {'timestampValue': x.toUtc().toIso8601String()};
  if (x is List) return {'arrayValue': {'values': x.map(encodeValue).toList()}};
  if (x is Map) return {'mapValue': {'fields': encodeFields(x.cast<String, dynamic>())}};
  throw ArgumentError('Unsupported value: ${x.runtimeType}');
}

Map<String, dynamic> encodeFields(Map<String, dynamic> m) => m.map((k, v) => MapEntry(k, encodeValue(v)));

dynamic decodeValue(Map<String, dynamic> v) {
  if (v.containsKey('nullValue')) return null;
  if (v.containsKey('booleanValue')) return v['booleanValue'] as bool;
  if (v.containsKey('integerValue')) return int.parse('${v['integerValue']}');
  if (v.containsKey('doubleValue')) return (v['doubleValue'] as num).toDouble();
  if (v.containsKey('stringValue')) return v['stringValue'] as String;
  if (v.containsKey('timestampValue')) return DateTime.parse(v['timestampValue'] as String);
  if (v.containsKey('arrayValue')) {
    final vals = (v['arrayValue'] as Map<String, dynamic>)['values'] as List? ?? const [];
    return vals.map((e) => decodeValue(e as Map<String, dynamic>)).toList();
  }
  if (v.containsKey('mapValue')) {
    return decodeFields(((v['mapValue'] as Map<String, dynamic>)['fields'] as Map?)?.cast<String, dynamic>());
  }
  return null;
}

Map<String, dynamic> decodeFields(Map<String, dynamic>? f) =>
    (f ?? const <String, dynamic>{}).map((k, v) => MapEntry(k, decodeValue(v as Map<String, dynamic>)));

typedef TokenProvider = Future<String?> Function();

/// Minimal Firestore REST client (no Firebase SDK, so it builds anywhere).
class Firestore {
  Firestore(this.client, {this.apiKey = FirebaseConfig.apiKey, this.projectId = FirebaseConfig.projectId, this.token, this.host = 'https://firestore.googleapis.com'});
  final http.Client client;
  final String apiKey, projectId, host;
  final TokenProvider? token;

  String get _root => 'projects/$projectId/databases/(default)/documents';

  Uri _uri(String path, [Map<String, List<String>>? q]) =>
      Uri.parse('$host/v1/$_root/$path').replace(queryParameters: <String, dynamic>{'key': apiKey, ...?q});

  Future<Map<String, dynamic>> _send(String method, Uri uri, {Object? body}) async {
    final req = http.Request(method, uri)..headers['content-type'] = 'application/json';
    final t = token == null ? null : await token!();
    if (t != null) req.headers['authorization'] = 'Bearer $t';
    if (body != null) req.body = jsonEncode(body);
    http.Response res;
    try {
      res = await http.Response.fromStream(await client.send(req).timeout(const Duration(seconds: 15)));
    } on Exception catch (e) {
      throw FsException(0, 'network: $e');
    }
    final text = utf8.decode(res.bodyBytes);
    var json = <String, dynamic>{};
    try {
      final d = jsonDecode(text);
      json = d is Map<String, dynamic> ? d : {'_list': d};
    } catch (_) {}
    if (res.statusCode >= 400) {
      final err = (json['error'] as Map?) ?? const {};
      throw FsException(res.statusCode, '${err['message'] ?? text}', '${err['status'] ?? ''}');
    }
    return json;
  }

  FsDoc _doc(Map<String, dynamic> j) => FsDoc(
        (j['name'] as String).split('/').last,
        decodeFields((j['fields'] as Map?)?.cast<String, dynamic>()),
        (j['updateTime'] ?? '') as String,
      );

  /// Returns null when the document does not exist.
  Future<FsDoc?> get(String path) async {
    try {
      return _doc(await _send('GET', _uri(path)));
    } on FsException catch (e) {
      if (e.status == 404) return null;
      rethrow;
    }
  }

  Future<List<FsDoc>> list(String collection) async {
    final out = <FsDoc>[];
    String? page;
    do {
      final j = await _send('GET', _uri(collection, {'pageSize': ['300'], if (page != null) 'pageToken': [page]}));
      out.addAll(((j['documents'] as List?) ?? const []).map((d) => _doc(d as Map<String, dynamic>)));
      page = j['nextPageToken'] as String?;
    } while (page != null);
    return out;
  }

  /// Documents whose integer [field] is greater than [value].
  Future<List<FsDoc>> queryGreater(String collection, String field, int value) async {
    final uri = Uri.parse('$host/v1/$_root:runQuery').replace(queryParameters: {'key': apiKey});
    final j = await _send('POST', uri, body: {
      'structuredQuery': {
        'from': [{'collectionId': collection}],
        'where': {'fieldFilter': {'field': {'fieldPath': field}, 'op': 'GREATER_THAN', 'value': {'integerValue': '$value'}}},
      },
    });
    return ((j['_list'] as List?) ?? const [])
        .where((e) => e is Map && e['document'] != null)
        .map((e) => _doc((e as Map)['document'] as Map<String, dynamic>))
        .toList();
  }

  Future<FsDoc> create(String collection, String id, Map<String, dynamic> data) async =>
      _doc(await _send('POST', _uri(collection, {'documentId': [id]}), body: {'fields': encodeFields(data)}));

  /// Updates only the given fields (or replaces the whole document with [replace]).
  /// [ifUpdateTime] makes the write fail with a conflict if someone changed the document meanwhile.
  Future<FsDoc> patch(String path, Map<String, dynamic> data, {bool replace = false, String? ifUpdateTime}) async {
    final q = <String, List<String>>{
      if (!replace) 'updateMask.fieldPaths': data.keys.toList(),
      if (ifUpdateTime != null) 'currentDocument.updateTime': [ifUpdateTime],
    };
    return _doc(await _send('PATCH', _uri(path, q), body: {'fields': encodeFields(data)}));
  }

  Future<void> delete(String path) async {
    await _send('DELETE', _uri(path));
  }
}
