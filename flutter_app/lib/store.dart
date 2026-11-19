import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'data.dart';

const _gapDays = [0, 1, 2, 4, 8, 16];

String dayKey([DateTime? d]) {
  final t = (d ?? DateTime.now());
  return '${t.year}-${t.month.toString().padLeft(2, '0')}-${t.day.toString().padLeft(2, '0')}';
}

class ExamResult {
  ExamResult(this.title, this.total, this.correct, this.wrong, this.blank, this.at);
  final String title;
  final int total, correct, wrong, blank;
  final int at;
  int get pct => total == 0 ? 0 : (correct * 100 / total).round();
  Map<String, dynamic> toJson() => {'t': title, 'n': total, 'c': correct, 'w': wrong, 'b': blank, 'at': at};
  factory ExamResult.fromJson(Map<String, dynamic> j) =>
      ExamResult(j['t'] as String, j['n'] as int, j['c'] as int, j['w'] as int, j['b'] as int, j['at'] as int);
}

/// App state persisted in SharedPreferences. Mirrors the web app's learning engine.
class AppStore extends ChangeNotifier {
  AppStore(this.bank, this._prefs) {
    _load();
  }
  final Bank bank;
  final SharedPreferences _prefs;

  final Map<String, List<int>> stats = {}; // id -> [box, dueMs]
  final Set<String> days = {};
  final Set<String> mistakes = {};
  final List<ExamResult> history = [];
  final Map<String, int> daily = {};
  final Set<String> badges = {};
  int goal = 20;
  ThemeMode theme = ThemeMode.system;

  void _load() {
    try {
      final j = jsonDecode(_prefs.getString('aden.v2') ?? '{}') as Map<String, dynamic>;
      (j['stats'] as Map<String, dynamic>? ?? {}).forEach((k, v) => stats[k] = (v as List).cast<int>());
      days.addAll((j['days'] as List? ?? []).cast<String>());
      mistakes.addAll((j['mistakes'] as List? ?? []).cast<String>());
      history.addAll((j['history'] as List? ?? []).map((e) => ExamResult.fromJson(e as Map<String, dynamic>)));
      (j['daily'] as Map<String, dynamic>? ?? {}).forEach((k, v) => daily[k] = v as int);
      badges.addAll((j['badges'] as List? ?? []).cast<String>());
      goal = (j['goal'] as int?) ?? 20;
      theme = ThemeMode.values[(j['theme'] as int?) ?? 0];
    } catch (_) {
      // corrupt data: start fresh rather than crash
    }
  }

  Future<void> _save() async {
    final j = {
      'stats': stats, 'days': days.toList(), 'mistakes': mistakes.toList(),
      'history': history.map((e) => e.toJson()).toList(), 'daily': daily, 'badges': badges.toList(), 'goal': goal, 'theme': theme.index,
    };
    await _prefs.setString('aden.v2', jsonEncode(j));
  }

  void _commit() {
    final fresh = _checkBadges();
    if (fresh.isNotEmpty) newBadges.addAll(fresh);
    notifyListeners();
    _save();
  }

  /// Record answers: right moves a question up one Leitner box, wrong sends it back to box 1.
  void record(Iterable<MapEntry<String, bool>> answers) {
    final now = DateTime.now();
    var n = 0;
    for (final a in answers) {
      final cur = stats[a.key]?[0] ?? 0;
      final box = a.value ? (cur + 1).clamp(1, 5) : 1;
      stats[a.key] = [box, now.add(Duration(days: _gapDays[box])).millisecondsSinceEpoch];
      a.value ? mistakes.remove(a.key) : mistakes.add(a.key);
      n++;
    }
    if (n > 0) {
      days.add(dayKey(now));
      daily[dayKey(now)] = (daily[dayKey(now)] ?? 0) + n;
    }
    _commit();
  }

  void addResult(ExamResult r) {
    history.insert(0, r);
    if (history.length > 50) history.removeLast();
    _commit();
  }

  /// Badges unlocked since the UI last showed them.
  final List<Award> newBadges = [];

  List<Award> _checkBadges() {
    final got = <Badge>[];
    for (final b in allBadges) {
      if (!badges.contains(b.id) && b.test(this)) {
        badges.add(b.id);
        got.add(b);
      }
    }
    return got;
  }

  // Running exam, so it survives the app being closed.
  void saveExam(Map<String, dynamic>? s) {
    s == null ? _prefs.remove('aden.exam') : _prefs.setString('aden.exam', jsonEncode(s));
  }

  Map<String, dynamic>? loadExam() {
    try {
      final raw = _prefs.getString('aden.exam');
      return raw == null ? null : jsonDecode(raw) as Map<String, dynamic>;
    } catch (_) {
      return null;
    }
  }

  int get todayCount => daily[dayKey()] ?? 0;

  int get streak {
    var d = DateTime.now();
    if (!days.contains(dayKey(d))) d = d.subtract(const Duration(days: 1));
    var n = 0;
    while (days.contains(dayKey(d))) {
      n++;
      d = d.subtract(const Duration(days: 1));
    }
    return n;
  }

  List<Question> due([int limit = 30]) {
    final now = DateTime.now().millisecondsSinceEpoch;
    final ids = stats.entries.where((e) => e.value[1] <= now).map((e) => e.key).toList();
    return ids.map((i) => bank.byId[i]).whereType<Question>().take(limit).toList();
  }

  int get dueCount => due(100000).length;
  List<Question> get mistakeQuestions => mistakes.map((i) => bank.byId[i]).whereType<Question>().toList();

  /// Share of a faculty's questions answered at box 3 or above.
  double mastery(String fid) {
    final total = bank.count(fid);
    if (total == 0) return 0;
    final m = stats.entries.where((e) => e.value[0] >= 3 && bank.byId[e.key]?.fid == fid).length;
    return m / total;
  }

  int get answeredTotal => stats.length;
  double get avgScore => history.isEmpty ? 0 : history.map((e) => e.pct).reduce((a, b) => a + b) / history.length;

  void setGoal(int g) {
    goal = g;
    _commit();
  }

  void setTheme(ThemeMode m) {
    theme = m;
    _commit();
  }
}

class Award {
  const Award(this.id, this.title, this.hint, this.icon, this.test);
  final String id, title, hint;
  final IconData icon;
  final bool Function(AppStore) test;
}

final allBadges = <Badge>[
  Award('first', 'البداية', 'أجب عن أول سؤال', Icons.rocket_launch_outlined, (s) => s.answeredTotal >= 1),
  Award('q100', '100 سؤال', 'أجب عن 100 سؤال مختلف', Icons.looks_one_outlined, (s) => s.answeredTotal >= 100),
  Award('q500', '500 سؤال', 'أجب عن 500 سؤال مختلف', Icons.military_tech_outlined, (s) => s.answeredTotal >= 500),
  Award('exam', 'أول اختبار', 'أنهِ اختبارًا تجريبيًا', Icons.assignment_turned_in_outlined, (s) => s.history.isNotEmpty),
  Award('perfect', 'علامة كاملة', 'احصل على 100% في اختبار من 10 أسئلة فأكثر', Icons.emoji_events_outlined, (s) => s.history.any((r) => r.pct == 100 && r.total >= 10)),
  Award('goal', 'هدف اليوم', 'حقّق هدفك اليومي', Icons.flag_outlined, (s) => s.todayCount >= s.goal),
  Award('streak3', '3 أيام', 'ادرس 3 أيام متتالية', Icons.local_fire_department_outlined, (s) => s.streak >= 3),
  Award('streak7', 'أسبوع كامل', 'ادرس 7 أيام متتالية', Icons.whatshot_outlined, (s) => s.streak >= 7),
];
