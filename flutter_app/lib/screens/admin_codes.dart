import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../access.dart';
import '../remote/admin_api.dart';
import '../remote/firestore.dart';

class CodesTab extends StatefulWidget {
  const CodesTab(this.api, {super.key});
  final AdminApi api;
  @override
  State<CodesTab> createState() => _CodesTabState();
}

class _CodesTabState extends State<CodesTab> with AutomaticKeepAliveClientMixin {
  final _name = TextEditingController(), _custom = TextEditingController(), _count = TextEditingController(text: '1');
  List<CodeRecord> _all = [];
  bool _loading = true, _busy = false;
  String _search = '';
  CodeStatus? _filter;
  List<String> _fresh = [];

  @override
  bool get wantKeepAlive => true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _toast(String m) => ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(m)));

  Future<void> _load() async {
    try {
      _all = await widget.api.codes();
    } on FsException {
      if (mounted) _toast('تعذر تحميل الرموز');
    }
    if (mounted) setState(() => _loading = false);
  }

  Future<void> _generate() async {
    final n = (int.tryParse(_count.text.trim()) ?? 1).clamp(1, 50);
    final custom = normalizeCode(_custom.text);
    if (_custom.text.trim().isNotEmpty && (n > 1 || !validCodeFormat(custom))) {
      _toast(n > 1 ? 'الرمز المخصص لا يصلح للدفعات' : 'الرمز المخصص: 6 إلى 40 حرفًا (حروف إنجليزية وأرقام وشرطات)');
      return;
    }
    setState(() => _busy = true);
    final made = <String>[];
    try {
      for (var i = 0; i < n; i++) {
        final code = _custom.text.trim().isNotEmpty ? custom : generateCode();
        try {
          await widget.api.createCode(code, _name.text.trim());
          made.add(code);
        } on FsException catch (e) {
          if (e.isConflict || e.status == 409) {
            if (_custom.text.trim().isNotEmpty) {
              _toast('هذا الرمز موجود مسبقًا');
              break;
            }
            i--; // random clash: try another
          } else {
            rethrow;
          }
        }
      }
    } on FsException {
      _toast('تعذر إنشاء الرمز');
    }
    _fresh = made;
    if (made.isNotEmpty) {
      _name.clear();
      _custom.clear();
      await _load();
    }
    if (mounted) setState(() => _busy = false);
  }

  Future<void> _copy(String text) async {
    await Clipboard.setData(ClipboardData(text: text));
    if (mounted) _toast('تم النسخ');
  }

  Future<bool> _confirm(String title, String text) async =>
      await showDialog<bool>(
        context: context,
        builder: (c) => AlertDialog(title: Text(title), content: Text(text), actions: [
          TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('إلغاء')),
          FilledButton(onPressed: () => Navigator.pop(c, true), child: const Text('تأكيد')),
        ]),
      ) ??
      false;

  Future<void> _act(Future<void> Function() f, String ok) async {
    try {
      await f();
      _toast(ok);
      await _load();
    } on FsException {
      _toast('تعذر تنفيذ العملية');
    }
  }

  String _date(DateTime? d) => d == null ? '—' : '${d.toLocal().year}-${d.toLocal().month.toString().padLeft(2, '0')}-${d.toLocal().day.toString().padLeft(2, '0')}';

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final cs = Theme.of(context).colorScheme;
    final shown = _all.where((r) {
      if (_filter != null && r.status != _filter) return false;
      final t = _search.trim().toLowerCase();
      return t.isEmpty || r.code.toLowerCase().contains(t) || r.userName.toLowerCase().contains(t);
    }).toList();
    return ListView(padding: const EdgeInsets.all(16), children: [
      Card(
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            Text('توليد رموز جديدة', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 10),
            TextField(controller: _name, decoration: const InputDecoration(labelText: 'اسم المستفيد (اختياري)', border: OutlineInputBorder())),
            const SizedBox(height: 10),
            Row(children: [
              Expanded(child: TextField(controller: _custom, textDirection: TextDirection.ltr, decoration: const InputDecoration(labelText: 'رمز مخصص (اختياري)', border: OutlineInputBorder()))),
              const SizedBox(width: 10),
              SizedBox(width: 90, child: TextField(controller: _count, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: 'العدد', border: OutlineInputBorder()))),
            ]),
            const SizedBox(height: 10),
            FilledButton.icon(onPressed: _busy ? null : _generate, icon: const Icon(Icons.add), label: const Text('توليد')),
            if (_fresh.isNotEmpty) ...[
              const Divider(height: 24),
              Row(children: [
                Expanded(child: Text('تم إنشاء ${_fresh.length} رمز', style: const TextStyle(fontWeight: FontWeight.w700))),
                TextButton.icon(onPressed: () => _copy(_fresh.join('\n')), icon: const Icon(Icons.copy_all, size: 18), label: const Text('نسخ الكل')),
              ]),
              for (final c in _fresh.take(10)) SelectableText(c, textDirection: TextDirection.ltr, style: const TextStyle(fontFamily: 'monospace')),
              if (_fresh.length > 10) Text('… و${_fresh.length - 10} أخرى (انسخها كلها)'),
            ],
          ]),
        ),
      ),
      const SizedBox(height: 12),
      TextField(decoration: const InputDecoration(prefixIcon: Icon(Icons.search), hintText: 'ابحث بالرمز أو الاسم', border: OutlineInputBorder()), onChanged: (v) => setState(() => _search = v)),
      const SizedBox(height: 8),
      Wrap(spacing: 8, children: [
        ChoiceChip(label: Text('الكل (${_all.length})'), selected: _filter == null, onSelected: (_) => setState(() => _filter = null)),
        for (final (s, label) in [(CodeStatus.available, 'متاح'), (CodeStatus.used, 'مفعّل'), (CodeStatus.suspended, 'موقوف')])
          ChoiceChip(label: Text('$label (${_all.where((r) => r.status == s).length})'), selected: _filter == s, onSelected: (_) => setState(() => _filter = s)),
      ]),
      const SizedBox(height: 8),
      if (_loading) const Center(child: Padding(padding: EdgeInsets.all(24), child: CircularProgressIndicator())),
      if (!_loading && shown.isEmpty) const Padding(padding: EdgeInsets.all(24), child: Center(child: Text('لا رموز مطابقة'))),
      for (final r in shown)
        Card(
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Row(children: [
                Expanded(child: SelectableText(r.code, textDirection: TextDirection.ltr, style: const TextStyle(fontFamily: 'monospace', fontWeight: FontWeight.w700))),
                IconButton(tooltip: 'نسخ', icon: const Icon(Icons.copy, size: 18), onPressed: () => _copy(r.code)),
                Chip(
                  label: Text(switch (r.status) { CodeStatus.available => 'متاح', CodeStatus.used => 'مفعّل', CodeStatus.suspended => 'موقوف' }),
                  backgroundColor: switch (r.status) { CodeStatus.available => Colors.green.withValues(alpha: .15), CodeStatus.used => cs.primaryContainer, CodeStatus.suspended => cs.errorContainer },
                  visualDensity: VisualDensity.compact,
                ),
              ]),
              if (r.userName.isNotEmpty) Text(r.userName),
              Text('أُنشئ ${_date(r.createdAt)} · فُعّل ${_date(r.activatedAt)}', style: TextStyle(color: cs.onSurfaceVariant, fontSize: 12.5)),
              const SizedBox(height: 6),
              Wrap(spacing: 8, children: [
                OutlinedButton(
                  onPressed: () => _act(() => widget.api.setActive(r.code, !r.isActive), r.isActive ? 'تم الإيقاف' : 'تم التفعيل'),
                  child: Text(r.isActive ? 'إيقاف' : 'تفعيل'),
                ),
                if (r.isUsed)
                  OutlinedButton(
                    onPressed: () async {
                      if (await _confirm('إعادة ضبط الجهاز', 'سيُفك ارتباط الرمز بالجهاز الحالي ليُفعَّل على جهاز جديد.')) {
                        await _act(() => widget.api.resetDevice(r.code), 'تم فك الارتباط');
                      }
                    },
                    child: const Text('إعادة ضبط الجهاز'),
                  ),
                TextButton(
                  onPressed: () async {
                    if (await _confirm('حذف الرمز', 'سيفقد صاحب الرمز الوصول عند أول تحقق. لا يمكن التراجع.')) {
                      await _act(() => widget.api.deleteCode(r.code), 'تم الحذف');
                    }
                  },
                  style: TextButton.styleFrom(foregroundColor: cs.error),
                  child: const Text('حذف'),
                ),
              ]),
            ]),
          ),
        ),
    ]);
  }
}
