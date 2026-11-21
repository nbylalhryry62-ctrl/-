import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'data.dart';
import 'remote/supabase.dart';

/// Remote settings row: app_config where id = 'main'
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

  /// A row of the `app_config` table (snake_case columns).
  factory RemoteConfig.fromRow(Map<String, dynamic> r) => RemoteConfig(
        contentVersion: (r['content_version'] as num?)?.toInt() ?? 0,
        maintenanceMode: r['maintenance_mode'] == true,
        maintenanceMessage: (r['maintenance_message'] as String?) ?? '',
        announcement: (r['announcement'] as String?) ?? '',
        contactUrl: (r['contact_url'] as String?) ?? '',
        defaultExamMinutes: ((r['default_exam_minutes'] as num?)?.toInt() ?? 20).clamp(5, 180),
        hiddenColleges: ((r['hidden_colleges'] as List?) ?? const []).cast<String>(),
      );

  /// The settings the admin may change, as accepted by the `save_config` function.
  Map<String, dynamic> toRow() => {
        'maintenance_mode': maintenanceMode, 'maintenance_message': maintenanceMessage, 'announcement': announcement,
        'contact_url': contactUrl, 'default_exam_minutes': defaultExamMinutes, 'hidden_colleges': hiddenColleges,
      };

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

/// Keeps settings and question edits in step with Supabase without reinstalling the app.
/// Only the settings document and the patches newer than the local contentVersion are downloaded;
/// everything is cached so the app also works offline.
class ContentSync extends ChangeNotifier {
  ContentSync(this.db, this.prefs, this.bank);
  final Backend db;
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
    if (db.url.isEmpty || db.anonKey.isEmpty) return false;
    try {
      final rows = await db.select('app_config', query: {'id': 'eq.main'});
      if (rows.isEmpty) return false;
      final remote = RemoteConfig.fromRow(rows.first);
      var changed = remote.toMap().toString() != config.toMap().toString();
      if (remote.contentVersion > _localVersion) {
        final patches = await db.selectAll('question_patches', query: {'v': 'gt.$_localVersion'});
        for (final p in patches) {
          _patches[p['id'] as String] = {...((p['data'] as Map?)?.cast<String, dynamic>() ?? const {}), 'deleted': p['deleted'] == true};
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
    } on RemoteException {
      return false; // offline: keep what we have
    }
  }
}
