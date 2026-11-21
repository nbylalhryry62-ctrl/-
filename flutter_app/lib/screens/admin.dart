import 'package:flutter/material.dart';
import '../data.dart';
import '../remote/auth.dart';
import '../remote/supabase.dart';
import '../services.dart';
import '../sync.dart';
import 'admin_codes.dart';
import 'admin_questions.dart';

/// Opens the admin dashboard, asking for the admin login first when needed.
Future<void> openAdmin(BuildContext context, Services s) async {
  if (!SupabaseConfig.enabled) {
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('لم تُضبط إعدادات Supabase في هذه النسخة')));
    return;
  }
  if (!s.auth.signedIn) {
    final ok = await showDialog<bool>(context: context, builder: (_) => _LoginDialog(s));
    if (ok != true || !context.mounted) return;
  }
  await Navigator.push(context, MaterialPageRoute(builder: (_) => AdminPage(s)));
}

class _LoginDialog extends StatefulWidget {
  const _LoginDialog(this.s);
  final Services s;
  @override
  State<_LoginDialog> createState() => _LoginDialogState();
}

class _LoginDialogState extends State<_LoginDialog> {
  final _mail = TextEditingController(), _pw = TextEditingController();
  bool _busy = false;
  String? _err;

  Future<void> _go() async {
    setState(() {
      _busy = true;
      _err = null;
    });
    try {
      await widget.s.auth.signIn(_mail.text, _pw.text);
      // The account must be listed in the `admins` table; the database answers, not the app.
      if (!await widget.s.admin.isAdmin()) {
        widget.s.auth.signOut();
        _fail('هذا الحساب ليس حساب مشرف');
        return;
      }
      if (mounted) Navigator.pop(context, true);
    } on AuthException catch (e) {
      _fail(e.message);
    } on RemoteException catch (e) {
      widget.s.auth.signOut();
      _fail(e.isDenied ? 'هذا الحساب ليس حساب مشرف' : 'تعذر الاتصال بالخادم');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _fail(String m) {
    if (mounted) setState(() => _err = m);
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
        title: const Text('دخول المشرف'),
        content: Column(mainAxisSize: MainAxisSize.min, children: [
          TextField(controller: _mail, keyboardType: TextInputType.emailAddress, textDirection: TextDirection.ltr, decoration: const InputDecoration(labelText: 'البريد الإلكتروني')),
          const SizedBox(height: 8),
          TextField(controller: _pw, obscureText: true, textDirection: TextDirection.ltr, decoration: const InputDecoration(labelText: 'كلمة المرور'), onSubmitted: (_) => _go()),
          if (_err != null) Padding(padding: const EdgeInsets.only(top: 10), child: Text(_err!, style: TextStyle(color: Theme.of(context).colorScheme.error))),
        ]),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('إلغاء')),
          FilledButton(onPressed: _busy ? null : _go, child: _busy ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2)) : const Text('دخول')),
        ],
      );
}

class AdminPage extends StatelessWidget {
  const AdminPage(this.s, {super.key});
  final Services s;
  @override
  Widget build(BuildContext context) => DefaultTabController(
        length: 4,
        child: Scaffold(
          appBar: AppBar(
            title: const Text('لوحة المشرف'),
            actions: [
              TextButton(
                onPressed: () {
                  s.auth.signOut();
                  Navigator.pop(context);
                },
                child: const Text('خروج'),
              ),
            ],
            bottom: const TabBar(isScrollable: true, tabs: [
              Tab(icon: Icon(Icons.vpn_key_outlined), text: 'الرموز'),
              Tab(icon: Icon(Icons.quiz_outlined), text: 'الأسئلة'),
              Tab(icon: Icon(Icons.tune), text: 'الإعدادات'),
              Tab(icon: Icon(Icons.bar_chart), text: 'الإحصائيات'),
            ]),
          ),
          body: TabBarView(children: [CodesTab(s.admin), QuestionsTab(s), SettingsTab(s), StatsTab(s)]),
        ),
      );
}

class SettingsTab extends StatefulWidget {
  const SettingsTab(this.s, {super.key});
  final Services s;
  @override
  State<SettingsTab> createState() => _SettingsTabState();
}

class _SettingsTabState extends State<SettingsTab> with AutomaticKeepAliveClientMixin {
  final _ann = TextEditingController(), _msg = TextEditingController(), _url = TextEditingController(), _min = TextEditingController();
  bool _maint = false, _loading = true, _saving = false;
  Set<String> _hidden = {};
  String? _err;

  @override
  bool get wantKeepAlive => true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final c = await widget.s.admin.getConfig();
      _ann.text = c.announcement;
      _msg.text = c.maintenanceMessage;
      _url.text = c.contactUrl;
      _min.text = '${c.defaultExamMinutes}';
      _maint = c.maintenanceMode;
      _hidden = c.hiddenColleges.toSet();
      _err = null;
    } on RemoteException {
      _err = 'تعذر تحميل الإعدادات';
    }
    if (mounted) setState(() => _loading = false);
  }

  Future<void> _save() async {
    setState(() => _saving = true);
    try {
      final minutes = (int.tryParse(_min.text.trim()) ?? 20).clamp(5, 180);
      final v = await widget.s.admin.saveConfig(RemoteConfig(
        maintenanceMode: _maint, maintenanceMessage: _msg.text.trim(), announcement: _ann.text.trim(),
        contactUrl: _url.text.trim(), defaultExamMinutes: minutes, hiddenColleges: _hidden.toList(),
      ));
      await widget.s.sync.refresh();
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('تم الحفظ، إصدار المحتوى $v')));
    } on RemoteException {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('تعذر الحفظ')));
    }
    if (mounted) setState(() => _saving = false);
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    if (_loading) return const Center(child: CircularProgressIndicator());
    final bank = widget.s.store.bank;
    return ListView(padding: const EdgeInsets.all(16), children: [
      if (_err != null) Text(_err!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
      TextField(controller: _ann, maxLines: 2, decoration: const InputDecoration(labelText: 'شريط الإعلان (يظهر في الرئيسية، اتركه فارغًا لإخفائه)', border: OutlineInputBorder())),
      const SizedBox(height: 12),
      TextField(controller: _url, textDirection: TextDirection.ltr, keyboardType: TextInputType.url, decoration: const InputDecoration(labelText: 'رابط التواصل (واتساب أو غيره)', hintText: 'https://wa.me/967...', border: OutlineInputBorder())),
      const SizedBox(height: 12),
      TextField(controller: _min, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: 'مدة الاختبار الافتراضية بالدقائق', border: OutlineInputBorder())),
      const SizedBox(height: 8),
      SwitchListTile(title: const Text('وضع الصيانة'), subtitle: const Text('يمنع الطلاب من استخدام التطبيق مؤقتًا'), value: _maint, onChanged: (v) => setState(() => _maint = v)),
      if (_maint) TextField(controller: _msg, maxLines: 2, decoration: const InputDecoration(labelText: 'رسالة الصيانة', border: OutlineInputBorder())),
      const SizedBox(height: 12),
      Text('الكليات الظاهرة للطلاب', style: Theme.of(context).textTheme.titleMedium),
      for (final f in bank.allFacultyIds)
        SwitchListTile(
          title: Text(bank.facultyTitles[f]!),
          value: !_hidden.contains(f),
          onChanged: (v) => setState(() => v ? _hidden.remove(f) : _hidden.add(f)),
        ),
      const SizedBox(height: 12),
      FilledButton(onPressed: _saving ? null : _save, child: _saving ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2)) : const Text('حفظ ونشر للطلاب')),
    ]);
  }
}

class StatsTab extends StatefulWidget {
  const StatsTab(this.s, {super.key});
  final Services s;
  @override
  State<StatsTab> createState() => _StatsTabState();
}

class _StatsTabState extends State<StatsTab> {
  late Future<List<Object>> _f = _load();

  Future<List<Object>> _load() async => [await widget.s.admin.codes(), await widget.s.admin.getConfig(), await widget.s.admin.patchCount()];

  @override
  Widget build(BuildContext context) => FutureBuilder<List<Object>>(
        future: _f,
        builder: (c, snap) {
          if (snap.hasError) return Center(child: TextButton(onPressed: () => setState(() => _f = _load()), child: const Text('تعذر التحميل. إعادة المحاولة')));
          if (!snap.hasData) return const Center(child: CircularProgressIndicator());
          final codes = snap.data![0] as List<dynamic>;
          final cfg = snap.data![1] as RemoteConfig;
          final patches = snap.data![2] as int;
          int n(bool Function(dynamic) t) => codes.where(t).length;
          final tiles = <(String, String, IconData)>[
            ('إجمالي الرموز', '${codes.length}', Icons.vpn_key_outlined),
            ('متاحة', '${n((x) => x.status.name == 'available')}', Icons.check_circle_outline),
            ('مفعّلة على أجهزة', '${n((x) => x.status.name == 'used')}', Icons.phone_android),
            ('موقوفة', '${n((x) => x.status.name == 'suspended')}', Icons.block),
            ('إصدار المحتوى', '${cfg.contentVersion}', Icons.sync),
            ('تعديلات الأسئلة', '$patches', Icons.edit_note),
          ];
          return RefreshIndicator(
            onRefresh: () async {
              setState(() => _f = _load());
              await _f;
            },
            child: GridView.count(
              padding: const EdgeInsets.all(16), crossAxisCount: 2, childAspectRatio: 1.5, mainAxisSpacing: 10, crossAxisSpacing: 10,
              children: [
                for (final t in tiles)
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(14),
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisAlignment: MainAxisAlignment.center, children: [
                        Icon(t.$3, color: Theme.of(context).colorScheme.primary),
                        const SizedBox(height: 6),
                        Text(t.$2, style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800)),
                        Text(t.$1),
                      ]),
                    ),
                  ),
              ],
            ),
          );
        },
      );
}
