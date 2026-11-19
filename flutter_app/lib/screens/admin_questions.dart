import 'package:flutter/material.dart';
import '../data.dart';
import '../remote/firestore.dart';
import '../services.dart';

class QuestionsTab extends StatefulWidget {
  const QuestionsTab(this.s, {super.key});
  final Services s;
  @override
  State<QuestionsTab> createState() => _QuestionsTabState();
}

class _QuestionsTabState extends State<QuestionsTab> with AutomaticKeepAliveClientMixin {
  String _search = '';

  @override
  bool get wantKeepAlive => true;

  Bank get bank => widget.s.store.bank;

  void _toast(String m) => ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(m)));

  Future<void> _edit(Question? q) async {
    final done = await Navigator.push<bool>(context, MaterialPageRoute(builder: (_) => QuestionEditor(widget.s, q)));
    if (done == true) {
      await widget.s.sync.refresh();
      if (mounted) setState(() {});
    }
  }

  Future<void> _delete(Question q) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(title: const Text('حذف السؤال'), content: Text(q.text, maxLines: 4, overflow: TextOverflow.ellipsis), actions: [
        TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('إلغاء')),
        FilledButton(onPressed: () => Navigator.pop(c, true), child: const Text('حذف للجميع')),
      ]),
    );
    if (ok != true) return;
    try {
      await widget.s.admin.deleteQuestion(q.id);
      await widget.s.sync.refresh();
      if (mounted) {
        setState(() {});
        _toast('تم الحذف');
      }
    } on FsException {
      _toast('تعذر الحذف');
    }
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final res = _search.trim().length < 2 ? <Question>[] : bank.query(search: _search, includeHidden: true).take(30).toList();
    return ListView(padding: const EdgeInsets.all(16), children: [
      FilledButton.icon(onPressed: () => _edit(null), icon: const Icon(Icons.add), label: const Text('إضافة سؤال جديد')),
      const SizedBox(height: 12),
      TextField(decoration: const InputDecoration(prefixIcon: Icon(Icons.search), hintText: 'ابحث في الأسئلة (حرفان على الأقل)', border: OutlineInputBorder()), onChanged: (v) => setState(() => _search = v)),
      const SizedBox(height: 8),
      for (final q in res)
        Card(
          child: ListTile(
            title: Text(q.text, maxLines: 3, overflow: TextOverflow.ellipsis),
            subtitle: Text('${q.facultyTitle} · ${q.subject}\n${q.id}'),
            isThreeLine: true,
            trailing: Row(mainAxisSize: MainAxisSize.min, children: [
              IconButton(tooltip: 'تعديل', icon: const Icon(Icons.edit_outlined), onPressed: () => _edit(q)),
              IconButton(tooltip: 'حذف', icon: const Icon(Icons.delete_outline), onPressed: () => _delete(q)),
            ]),
          ),
        ),
    ]);
  }
}

class QuestionEditor extends StatefulWidget {
  const QuestionEditor(this.s, this.q, {super.key});
  final Services s;
  final Question? q;
  @override
  State<QuestionEditor> createState() => _QuestionEditorState();
}

class _QuestionEditorState extends State<QuestionEditor> {
  final _form = GlobalKey<FormState>();
  late String fid = widget.q?.fid ?? widget.s.store.bank.allFacultyIds.first;
  late String diff = widget.q?.diff ?? 'متوسط';
  late String lang = widget.q?.lang ?? 'ar';
  late int ci = widget.q?.ci ?? 0;
  late final text = TextEditingController(text: widget.q?.text);
  late final subject = TextEditingController(text: widget.q?.subject);
  late final unit = TextEditingController(text: widget.q?.unit);
  late final expl = TextEditingController(text: widget.q?.expl);
  late final opts = List.generate(4, (i) => TextEditingController(text: (widget.q?.opts.length ?? 0) > i ? widget.q!.opts[i] : ''));
  bool _saving = false;

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    final list = opts.map((c) => c.text.trim()).toList();
    if (list.toSet().length != list.length) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('الخيارات يجب ألا تتكرر')));
      return;
    }
    setState(() => _saving = true);
    final bank = widget.s.store.bank;
    final q = Question(
      widget.q?.id ?? 'a_${DateTime.now().millisecondsSinceEpoch.toRadixString(36)}',
      fid, bank.facultyTitles[fid] ?? '', subject.text.trim(), unit.text.trim(), diff, lang, text.text.trim(), list, ci, expl.text.trim(),
    );
    try {
      await widget.s.admin.publishQuestion(q);
      if (mounted) Navigator.pop(context, true);
    } on FsException {
      if (mounted) {
        setState(() => _saving = false);
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('تعذر الحفظ')));
      }
    }
  }

  String? _req(String? v) => (v == null || v.trim().isEmpty) ? 'مطلوب' : null;

  Widget _pick(TextEditingController c, String label, List<String> options) => TextFormField(
        controller: c,
        validator: _req,
        decoration: InputDecoration(
          labelText: label, border: const OutlineInputBorder(),
          suffixIcon: PopupMenuButton<String>(icon: const Icon(Icons.arrow_drop_down), onSelected: (v) => c.text = v, itemBuilder: (_) => [for (final o in options.take(40)) PopupMenuItem(value: o, child: Text(o, overflow: TextOverflow.ellipsis))]),
        ),
      );

  @override
  Widget build(BuildContext context) {
    final bank = widget.s.store.bank;
    final subjects = bank.subjects(fid);
    final units = ({for (final x in bank.all.where((x) => x.fid == fid && (subject.text.isEmpty || x.subject == subject.text)) ) x.unit}).toList()..sort();
    return Scaffold(
      appBar: AppBar(title: Text(widget.q == null ? 'سؤال جديد' : 'تعديل سؤال')),
      body: Form(
        key: _form,
        child: ListView(padding: const EdgeInsets.all(16), children: [
          DropdownButtonFormField<String>(
            initialValue: fid, isExpanded: true, decoration: const InputDecoration(labelText: 'الكلية', border: OutlineInputBorder()),
            items: [for (final f in bank.allFacultyIds) DropdownMenuItem(value: f, child: Text(bank.facultyTitles[f]!, overflow: TextOverflow.ellipsis))],
            onChanged: (v) => setState(() => fid = v!),
          ),
          const SizedBox(height: 12),
          _pick(subject, 'المادة', subjects),
          const SizedBox(height: 12),
          _pick(unit, 'الوحدة / المحور', units),
          const SizedBox(height: 12),
          Row(children: [
            Expanded(
              child: DropdownButtonFormField<String>(
                initialValue: diff, decoration: const InputDecoration(labelText: 'الصعوبة', border: OutlineInputBorder()),
                items: const [for (final d in ['سهل', 'متوسط', 'صعب']) DropdownMenuItem(value: d, child: Text(d))],
                onChanged: (v) => setState(() => diff = v!),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: DropdownButtonFormField<String>(
                initialValue: lang, decoration: const InputDecoration(labelText: 'اللغة', border: OutlineInputBorder()),
                items: const [DropdownMenuItem(value: 'ar', child: Text('العربية')), DropdownMenuItem(value: 'en', child: Text('English'))],
                onChanged: (v) => setState(() => lang = v!),
              ),
            ),
          ]),
          const SizedBox(height: 12),
          TextFormField(controller: text, validator: _req, maxLines: 4, decoration: const InputDecoration(labelText: 'نص السؤال', border: OutlineInputBorder())),
          const SizedBox(height: 12),
          RadioGroup<int>(
            groupValue: ci,
            onChanged: (v) => setState(() => ci = v!),
            child: Column(children: [
              for (var i = 0; i < 4; i++)
                Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: Row(children: [
                    Radio<int>(value: i),
                    Expanded(child: TextFormField(controller: opts[i], validator: _req, decoration: InputDecoration(labelText: 'الخيار ${i + 1}${ci == i ? ' (الصحيح)' : ''}', border: const OutlineInputBorder()))),
                  ]),
                ),
            ]),
          ),
          TextFormField(controller: expl, maxLines: 3, decoration: const InputDecoration(labelText: 'الشرح (اختياري)', border: OutlineInputBorder())),
          const SizedBox(height: 16),
          FilledButton(onPressed: _saving ? null : _save, child: _saving ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2)) : const Text('حفظ ونشر للطلاب')),
        ]),
      ),
    );
  }
}
