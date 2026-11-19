import 'package:flutter/material.dart';
import '../store.dart';
import '../widgets.dart';

class BankScreen extends StatefulWidget {
  const BankScreen({super.key, required this.store});
  final AppStore store;
  @override
  State<BankScreen> createState() => _BankScreenState();
}

class _BankScreenState extends State<BankScreen> {
  String? fid, subject, diff;
  String search = '';
  int page = 0;
  static const perPage = 15;

  @override
  Widget build(BuildContext context) {
    final bank = widget.store.bank;
    final subjects = bank.subjects(fid);
    final res = bank.query(fid: fid, subject: subject, diff: diff, search: search);
    final pages = (res.length / perPage).ceil().clamp(1, 1 << 30);
    if (page >= pages) page = 0;
    final slice = res.skip(page * perPage).take(perPage).toList();
    return ListView(padding: const EdgeInsets.all(16), children: [
      TextField(
        decoration: const InputDecoration(prefixIcon: Icon(Icons.search), hintText: 'ابحث في نص السؤال أو رقمه', border: OutlineInputBorder()),
        onChanged: (v) => setState(() {
          search = v;
          page = 0;
        }),
      ),
      const SizedBox(height: 10),
      DropdownButtonFormField<String?>(
        value: fid, isExpanded: true, decoration: const InputDecoration(labelText: 'الكلية', border: OutlineInputBorder()),
        items: [const DropdownMenuItem(value: null, child: Text('كل الكليات')), for (final f in bank.facultyIds) DropdownMenuItem(value: f, child: Text(bank.facultyTitles[f]!, overflow: TextOverflow.ellipsis))],
        onChanged: (v) => setState(() {
          fid = v;
          subject = null;
          page = 0;
        }),
      ),
      const SizedBox(height: 10),
      DropdownButtonFormField<String?>(
        key: ValueKey('$fid-$subject'), value: subject, isExpanded: true, decoration: const InputDecoration(labelText: 'المادة', border: OutlineInputBorder()),
        items: [const DropdownMenuItem(value: null, child: Text('كل المواد')), for (final s in subjects) DropdownMenuItem(value: s, child: Text(s, overflow: TextOverflow.ellipsis))],
        onChanged: (v) => setState(() {
          subject = v;
          page = 0;
        }),
      ),
      const SizedBox(height: 10),
      SegmentedButton<String?>(
        showSelectedIcon: false,
        segments: const [
          ButtonSegment(value: null, label: Text('الكل')),
          ButtonSegment(value: 'سهل', label: Text('سهل')),
          ButtonSegment(value: 'متوسط', label: Text('متوسط')),
          ButtonSegment(value: 'صعب', label: Text('صعب')),
        ],
        selected: {diff},
        onSelectionChanged: (s) => setState(() {
          diff = s.first;
          page = 0;
        }),
      ),
      const SizedBox(height: 10),
      Row(children: [
        Expanded(child: Text('${res.length} سؤال')),
        if (res.isNotEmpty)
          FilledButton.tonal(
            onPressed: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => StudyPage(title: 'تدريب', questions: bank.sample(res, 20), onDone: widget.store.record)),
            ),
            child: const Text('تدرّب على هذه النتائج'),
          ),
      ]),
      const SizedBox(height: 8),
      for (final q in slice) QuestionCard(key: ValueKey(q.id), q: q, onResult: (ok) => widget.store.record([MapEntry(q.id, ok)])),
      Row(mainAxisAlignment: MainAxisAlignment.center, children: [
        IconButton(onPressed: page > 0 ? () => setState(() => page--) : null, icon: const Icon(Icons.chevron_right)),
        Text('${page + 1} / $pages'),
        IconButton(onPressed: page < pages - 1 ? () => setState(() => page++) : null, icon: const Icon(Icons.chevron_left)),
      ]),
    ]);
  }
}
