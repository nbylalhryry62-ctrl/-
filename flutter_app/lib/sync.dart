import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'data.dart';
import 'remote/firestore.dart';

/// Remote settings document: app_config/main
class RemoteConfig {
  const RemoteConfig({
    this.contentVersion = 0, this.maintenanceMode = false, this.maintenanceMessage = '', this.announcement = '',
    this.contactUrl = '', this.defaultExamMinutes = 20, this.hiddenColleges = const [],
  });
  final int contentVersion, defaultExamMinutes;
  final bool maintenanceMode;
  final String maintenanceMessage, announcement, contactUrl;
  final List<String> hiddenColleges;

  factory RemoteConfig.fromMap(Map<String, dynamic> m) => RemoteConfig(
        contentVersion: (m['contentVersion'] as int?) ?? 0,
        maintenanceMode: m['maintenanceMode'] == true,
        maintenanceMessage: (m['maintenanceMessage'] as String?) ?? '',
        announcement: (m['announcement'] as String?) ?? '',
        contactUrl: (m['contactUrl'] as String?) ?? '',
        defaultExamMinutes: ((m['defaultExamMinutes'] as int?) ?? 20).clamp(5, 180),
        hiddenColleges: ((m['hiddenColleges'] as List?) ?? const []).cast<String>(),
      );

  Map<String, dynamic> toMap() => {
        'contentVersion': contentVersion, 'maintenanceMode': maintenanceMode, 'maintenanceMessage': maintenanceMessage,
        'announcement': announcement, 'contactUrl': contactUrl, 'defaultExamMinutes': defaultExamMinutes, 'hiddenColleges': hiddenColleges,
      };

  RemoteConfig copyWith({bool? maintenanceMode, String? maintenanceMessage, String? announcement, String? contactUrl, int? defaultExamMinutes, List<String>? hiddenColleges, int? contentVersion}) =>
      RemoteConfig(
        contentVersion: contentVersion ?? this.contentVersion,
        maintenanceMode: maintenanceMode ?? this.maintenanceMode,
        maintenanceMessage: maintenanceMessage ?? this.maintenanceMessage,
        announcement: announcement ?? this.announcement,
        contactUrl: contactUrl ?? this.contactUrl,
        defaultExamMinutes: defaultExamMinutes ?? this.defaultExamMinutes,
        hiddenColleges: hiddenColleges ?? this.hiddenColleges,
      );
}

/// Keeps settings and question edits in step with Firestore without reinstalling the app.
/// Only the settings document and the patches newer than the local contentVersion are downloaded;
/// everything is cached so the app also works offline.
class ContentSync extends ChangeNotifier {
  ContentSync(this.fs, this.prefs, this.bank);
  final Firestore fs;
  final SharedPreferences prefs;
  final Bank bank;

  RemoteConfig config = const RemoteConfig();
  final Map<String, Map<String, dynamic>> _patches = {};
  int _localVersion = 0;
  int get localVersion => _localVersion;
  int get patchCount => _patches.length;

  void loadLocal() {
    try {
      final c = prefs.getString('aden.config');
      if (c != null) config = RemoteConfig.fromMap(jsonDecode(c) as Map<String, dynamic>);
      final p = prefs.getString('aden.patches');
      if (p != null) {
        (jsonDecode(p) as Map<String, dynamic>).forEach((k, v) => _patches[k] = (v as Map).cast<String, dynamic>());
      }
      _localVersion = prefs.getInt('aden.contentVersion') ?? 0;
    } catch (_) {
      // damaged cache: start from the bundled bank
    }
    _apply();
  }

  void _apply() {
    bank.hidden = config.hiddenColleges.toSet();
    bank.applyPatches(_patches);
  }

  Future<void> _save() async {
    await prefs.setString('aden.config', jsonEncode(config.toMap()));
    await prefs.setString('aden.patches', jsonEncode(_patches));
    await prefs.setInt('aden.contentVersion', _localVersion);
  }

  /// Returns true when new content or settings arrived.
  Future<bool> refresh() async {
    if (!FirebaseConfig.enabled) return false;
    try {
      final d = await fs.get('app_config/main');
      if (d == null) return false;
      final remote = RemoteConfig.fromMap(d.data);
      var changed = remote.toMap().toString() != config.toMap().toString();
      if (remote.contentVersion > _localVersion) {
        final docs = await fs.queryGreater('question_patches', 'v', _localVersion);
        for (final p in docs) {
          _patches[p.id] = {...p.data};
        }
        _localVersion = remote.contentVersion;
        changed = true;
      }
      config = remote;
      if (changed) {
        _apply();
        await _save();
        notifyListeners();
      }
      return changed;
    } on FsException {
      return false; // offline: keep what we have
    }
  }
}
