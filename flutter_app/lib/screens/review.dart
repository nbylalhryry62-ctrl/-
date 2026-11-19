import 'package:flutter/material.dart';
import '../store.dart';
import '../widgets.dart';

class ReviewScreen extends StatelessWidget {
  const ReviewScreen({super.key, required this.store});
  final AppStore store;

  @override
  Widget build(BuildContext context) {
    final due = store.due(30);
    final mistakes = store.mistakeQuestions;
    void open(String title, List qs) => Navigator.push(
          context, MaterialPageRoute(builder: (_) => StudyPage(title: title, questions: qs.cast(), onDone: store.record)));
    return ListView(padding: const EdgeInsets.all(16), children: [
      Text('المراجعة', style: Theme.of(context).textTheme.headlineSmall),
      const SizedBox(height: 12),
      Card(
        child: ListTile(
          leading: const Icon(Icons.event_repeat),
          title: Text('مراجعة المستحق (${store.dueCount})'),
          subtitle: const Text('أسئلة حان موعد إعادتها بنظام التكرار المتباعد'),
          trailing: const Icon(Icons.chevron_left),
          enabled: due.isNotEmpty,
          onTap: due.isEmpty ? null : () => open('مراجعة المستحق', due),
        ),
      ),
      Card(
        child: ListTile(
          leading: const Icon(Icons.error_outline),
          title: Text('دفتر الأخطاء (${mistakes.length})'),
          subtitle: const Text('الأسئلة التي أخطأت فيها آخر مرة'),
          trailing: const Icon(Icons.chevron_left),
          enabled: mistakes.isNotEmpty,
          onTap: mistakes.isEmpty ? null : () => open('دفتر الأخطاء', mistakes.take(30).toList()),
        ),
      ),
      if (due.isEmpty && mistakes.isEmpty)
        const Padding(padding: EdgeInsets.all(32), child: Center(child: Text('لا شيء للمراجعة الآن. أجب عن بعض الأسئلة أولًا.'))),
    ]);
  }
}
