import 'dart:convert';
import 'dart:io';
import 'dart:math';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class Question {
  Question(this.id, this.fid, this.facultyTitle, this.subject, this.unit, this.diff, this.lang, this.text, this.opts, this.ci, this.expl);
  final String id, fid, facultyTitle, subject, unit, diff, lang, text, expl;
  final List<String> opts;
  final int ci;
  bool get en => lang == 'en';

  factory Question.fromJson(Map<String, dynamic> j) => Question(
        j['id'] as String, j['f'] as String, j['ft'] as String, j['s'] as String, j['u'] as String,
        j['d'] as String, j['l'] as String, j['q'] as String, (j['o'] as List).cast<String>(), j['c'] as int, (j['e'] ?? '') as String);
}

class FacultyMeta {
  const FacultyMeta(this.id, this.color, this.icon);
  final String id;
  final Color color;
  final IconData icon;
}

const facultyMeta = <String, FacultyMeta>{
  'medical': FacultyMeta('medical', Color(0xFF0A7A62), Icons.medical_services_outlined),
  'sports_nutrition': FacultyMeta('sports_nutrition', Color(0xFF0E8C8C), Icons.favorite_outline),
  'law': FacultyMeta('law', Color(0xFFB07A10), Icons.balance_outlined),
  'cs_it': FacultyMeta('cs_it', Color(0xFF2F6FD6), Icons.memory_outlined),
  'economics_politics': FacultyMeta('economics_politics', Color(0xFF5A4FCF), Icons.trending_up),
  'languages': FacultyMeta('languages', Color(0xFF9145C4), Icons.translate),
  'engineering': FacultyMeta('engineering', Color(0xFFC2415A), Icons.architecture),
};

FacultyMeta metaOf(String fid) => facultyMeta[fid] ?? const FacultyMeta('x', Color(0xFF0A7A62), Icons.school_outlined);

class Bank {
  Bank._(List<Question> base) : _base = base, all = base {
    _index();
  }
  final List<Question> _base;
  List<Question> all;
  factory Bank.forTest(List<Question> qs) => Bank._(qs);
  final Map<String, Question> byId = {};
  final Map<String, String> facultyTitles = {};

  /// Faculties the admin has hidden remotely. Their questions disappear from browsing and exams.
  Set<String> hidden = {};

  void _index() {
    byId.clear();
    for (final q in all) {
      byId[q.id] = q;
      facultyTitles.putIfAbsent(q.fid, () => q.facultyTitle);
    }
  }

  /// Applies remote question edits on top of the bundled bank (add, change or delete by id).
  void applyPatches(Map<String, Map<String, dynamic>> patches) {
    final m = {for (final q in _base) q.id: q};
    patches.forEach((id, p) {
      if (p['deleted'] == true) {
        m.remove(id);
      } else {
        try {
          m[id] = Question.fromJson({...p, 'id': id});
        } catch (_) {
          // ignore a malformed patch rather than crash the app
        }
      }
    });
    all = m.values.toList(growable: false);
    _index();
  }

  static Future<Bank> load() async {
    final data = await rootBundle.load('assets/qbank.json.gz');
    final bytes = gzip.decode(data.buffer.asUint8List(data.offsetInBytes, data.lengthInBytes));
    final list = jsonDecode(utf8.decode(bytes)) as List;
    return Bank._(list.map((e) => Question.fromJson(e as Map<String, dynamic>)).toList(growable: false));
  }

  List<String> get facultyIds => facultyMeta.keys.where((f) => facultyTitles.containsKey(f) && !hidden.contains(f)).toList();
  int count(String fid) => all.where((q) => q.fid == fid).length;
  List<String> get allFacultyIds => facultyMeta.keys.where(facultyTitles.containsKey).toList();
  List<String> subjects(String? fid) {
    final s = <String>{};
    for (final q in all) {
      if (fid == null || q.fid == fid) s.add(q.subject);
    }
    return s.toList()..sort();
  }

  List<Question> query({String? fid, String? subject, String? diff, String search = '', bool includeHidden = false}) {
    final t = search.trim().toLowerCase();
    return all.where((q) {
      if (!includeHidden && hidden.contains(q.fid)) return false;
      if (fid != null && q.fid != fid) return false;
      if (subject != null && q.subject != subject) return false;
      if (diff != null && q.diff != diff) return false;
      if (t.isNotEmpty && !(q.text.toLowerCase().contains(t) || q.id.toLowerCase() == t || q.unit.contains(t))) return false;
      return true;
    }).toList();
  }

  List<Question> sample(List<Question> pool, int n, [Random? rnd]) {
    final l = [...pool]..shuffle(rnd);
    return l.take(n).toList();
  }
}
