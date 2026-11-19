import '../data.dart';
import '../sync.dart';
import 'firestore.dart';

enum CodeStatus { available, used, suspended }

class CodeRecord {
  CodeRecord(this.code, this.isUsed, this.isActive, this.deviceId, this.userName, this.createdAt, this.activatedAt);
  final String code, userName;
  final bool isUsed, isActive;
  final String? deviceId;
  final DateTime? createdAt, activatedAt;

  CodeStatus get status => !isActive ? CodeStatus.suspended : (isUsed ? CodeStatus.used : CodeStatus.available);

  factory CodeRecord.fromDoc(FsDoc d) => CodeRecord(
        d.id, d.data['isUsed'] == true, d.data['isActive'] == true, d.data['deviceId'] as String?,
        (d.data['userName'] as String?) ?? '', d.data['createdAt'] as DateTime?, d.data['activatedAt'] as DateTime?,
      );
}

/// Everything the admin dashboard writes. The Firestore instance carries the admin's ID token.
class AdminApi {
  AdminApi(this.fs);
  final Firestore fs;

  Future<List<CodeRecord>> codes() async {
    final l = (await fs.list('access_codes')).map(CodeRecord.fromDoc).toList();
    l.sort((a, b) => (b.createdAt ?? DateTime(2000)).compareTo(a.createdAt ?? DateTime(2000)));
    return l;
  }

  Future<void> createCode(String code, String userName) => fs.create('access_codes', code, {
        'code': code, 'isUsed': false, 'deviceId': null, 'isActive': true, 'userName': userName,
        'createdAt': DateTime.now().toUtc(), 'activatedAt': null,
      });

  Future<void> setActive(String code, bool active) => fs.patch('access_codes/$code', {'isActive': active});

  /// Unbinds the code from the old phone so it can be activated on a new one.
  Future<void> resetDevice(String code) => fs.patch('access_codes/$code', {'isUsed': false, 'deviceId': null, 'activatedAt': null});

  Future<void> deleteCode(String code) => fs.delete('access_codes/$code');

  Future<RemoteConfig> getConfig() async => RemoteConfig.fromMap((await fs.get('app_config/main'))?.data ?? {});

  /// Saves settings and bumps contentVersion so every device refreshes. Returns the new version.
  Future<int> saveConfig(RemoteConfig c) async {
    final v = (await getConfig()).contentVersion + 1;
    await fs.patch('app_config/main', c.copyWith(contentVersion: v).toMap());
    return v;
  }

  Map<String, dynamic> _questionMap(Question q) =>
      {'f': q.fid, 'ft': q.facultyTitle, 's': q.subject, 'u': q.unit, 'd': q.diff, 'l': q.lang, 'q': q.text, 'o': q.opts, 'c': q.ci, 'e': q.expl};

  /// Adds or edits a question for every student, and bumps contentVersion.
  Future<int> publishQuestion(Question q) async {
    final cfg = await getConfig();
    final v = cfg.contentVersion + 1;
    await fs.patch('question_patches/${q.id}', {..._questionMap(q), 'v': v, 'deleted': false}, replace: true);
    await fs.patch('app_config/main', {'contentVersion': v});
    return v;
  }

  Future<int> deleteQuestion(String id) async {
    final cfg = await getConfig();
    final v = cfg.contentVersion + 1;
    await fs.patch('question_patches/$id', {'v': v, 'deleted': true}, replace: true);
    await fs.patch('app_config/main', {'contentVersion': v});
    return v;
  }

  Future<int> patchCount() async => (await fs.queryGreater('question_patches', 'v', 0)).length;
}
