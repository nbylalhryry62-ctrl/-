import 'package:flutter/material.dart';
import 'store.dart';
import 'widgets.dart';

/// Searches the whole bank by question text, id or unit.
class QuestionSearch extends SearchDelegate<void> {
  QuestionSearch(this.store) : super(searchFieldLabel: 'ابحث في كل الأسئلة');
  final AppStore store;

  @override
  List<Widget> buildActions(BuildContext context) => [if (query.isNotEmpty) IconButton(icon: const Icon(Icons.clear), onPressed: () => query = '')];

  @override
  Widget buildLeading(BuildContext context) => IconButton(icon: const Icon(Icons.arrow_back), onPressed: () => close(context, null));

  @override
  Widget buildResults(BuildContext context) => _list(context);

  @override
  Widget buildSuggestions(BuildContext context) => _list(context);

  Widget _list(BuildContext context) {
    if (query.trim().length < 2) return const Center(child: Text('اكتب كلمتين على الأقل للبحث'));
    final res = store.bank.query(search: query);
    if (res.isEmpty) return const Center(child: Text('لا نتائج'));
    final shown = res.take(30).toList();
    return ListView(padding: const EdgeInsets.all(12), children: [
      Padding(padding: const EdgeInsets.only(bottom: 8), child: Text('${res.length} نتيجة${res.length > 30 ? ' (أول 30)' : ''}')),
      for (final q in shown) QuestionCard(key: ValueKey(q.id), q: q, onResult: (ok) => store.record([MapEntry(q.id, ok)])),
    ]);
  }
}
