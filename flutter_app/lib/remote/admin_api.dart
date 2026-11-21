import '../data.dart';
import '../sync.dart';
import 'supabase.dart';

enum CodeStatus { available, used, suspended }

class CodeRecord {
  CodeRecord(this.code, this.isUsed, this.isActive, this.deviceId, this.userName, this.createdAt, this.activatedAt);
  final String code, userName;
  final bool isUsed, isActive;
  final String? deviceId;
  final DateTime? createdAt, activatedAt;

  CodeStatus get status => !isActive ? CodeStatus.suspended : (isUsed ? CodeStatus.used : CodeStatus.available);

  static DateTime? _dt(dynamic v) => v is String ? DateTime.tryParse(v) : null;

  factory CodeRecord.fromRow(Map<String, dynamic> r) => CodeRecord(
        r['code'] as String, r['is_used'] == true, r['is_active'] == true, r['device_id'] as String?,
        (r['user_name'] as String?) ?? '', _dt(r['created_at']), _dt(r['activated_at']),
      );
}

/// Everything the admin dashboard does. The backend carries the admin's access token.
class AdminApi {
  AdminApi(this.db);
  final Backend db;

  /// True only for accounts listed in the `admins` table (the database decides, not the app).
  Future<bool> isAdmin() async => (await db.rpc('is_admin', {})) == true;

  Future<List<CodeRecord>> codes() async {
    final l = (await db.selectAll('access_codes', query: {'order': 'created_at.desc'})).map(CodeRecord.fromRow).toList();
    return l;
  }

  Future<void> createCode(String code, String userName) =>
      db.insert('access_codes', {'code': code, 'user_name': userName});

  Future<void> setActive(String code, bool active) => db.update('access_codes', {'code': 'eq.$code'}, {'is_active': active});

  /// Unbinds the code from the old phone so it can be activated on a new one.
  Future<void> resetDevice(String code) =>
      db.update('access_codes', {'code': 'eq.$code'}, {'is_used': false, 'device_id': null, 'activated_at': null});

  Future<void> deleteCode(String code) => db.delete('access_codes', {'code': 'eq.$code'});

  Future<RemoteConfig> getConfig() async {
    final rows = await db.select('app_config', query: {'id': 'eq.main'});
    return rows.isEmpty ? const RemoteConfig() : RemoteConfig.fromRow(rows.first);
  }

  /// Saves settings and bumps content_version so every device refreshes. Returns the new version.
  Future<int> saveConfig(RemoteConfig c) async =>
      (await db.rpc('save_config', {'p': c.toRow()}) as num).toInt();

  Map<String, dynamic> _questionMap(Question q) =>
      {'f': q.fid, 'ft': q.facultyTitle, 's': q.subject, 'u': q.unit, 'd': q.diff, 'l': q.lang, 'q': q.text, 'o': q.opts, 'c': q.ci, 'e': q.expl};

  /// Adds or edits a question for every student, and bumps content_version.
  Future<int> publishQuestion(Question q) async =>
      (await db.rpc('publish_question', {'p_id': q.id, 'p_data': _questionMap(q), 'p_deleted': false}) as num).toInt();

  Future<int> deleteQuestion(String id) async =>
      (await db.rpc('publish_question', {'p_id': id, 'p_data': <String, dynamic>{}, 'p_deleted': true}) as num).toInt();

  Future<int> patchCount() async => (await db.selectAll('question_patches', query: {'select': 'id'})).length;
}
