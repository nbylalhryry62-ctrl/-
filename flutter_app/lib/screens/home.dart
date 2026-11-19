import 'package:flutter/material.dart';
import '../data.dart';
import 'package:url_launcher/url_launcher.dart';
import '../chart.dart';
import '../sync.dart';
import '../store.dart';
import '../widgets.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key, required this.store, required this.sync, required this.go});
  final AppStore store;
  final ContentSync sync;
  final void Function(int tab) go;

  @override
  Widget build(BuildContext context) {
    final bank = store.bank;
    final cs = Theme.of(context).colorScheme;
    final goalP = (store.todayCount / store.goal).clamp(0.0, 1.0);
    final cfg = sync.config;
    return ListView(padding: const EdgeInsets.all(16), children: [
      if (cfg.announcement.isNotEmpty)
        Container(
          margin: const EdgeInsets.only(bottom: 14),
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(color: Colors.amber.withValues(alpha: .18), borderRadius: BorderRadius.circular(14), border: Border.all(color: Colors.amber.shade700)),
          child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Icon(Icons.campaign_outlined, color: Colors.amber.shade800),
            const SizedBox(width: 10),
            Expanded(child: Text(cfg.announcement)),
          ]),
        ),
      Container(
        padding: const EdgeInsets.all(22),
        decoration: BoxDecoration(
          gradient: const LinearGradient(colors: [Color(0xFF0B1E31), Color(0xFF0C3A46)]),
          borderRadius: BorderRadius.circular(20),
        ),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          const Text('جامعة عدن · قبول 2026/2027', style: TextStyle(color: Color(0xFF7FE0C2), fontWeight: FontWeight.w700)),
          const SizedBox(height: 8),
          const Text('استعد لاختبار القبول بثقة', style: TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.w800)),
          const SizedBox(height: 6),
          Text('${bank.all.length} سؤالًا في ${bank.facultyIds.length} كليات', style: const TextStyle(color: Color(0xFFA9BFD2))),
          const SizedBox(height: 14),
          Wrap(spacing: 10, children: [
            FilledButton(onPressed: () => go(2), child: const Text('ابدأ اختبارًا')),
            OutlinedButton(
              onPressed: () => go(1),
              style: OutlinedButton.styleFrom(foregroundColor: Colors.white, side: const BorderSide(color: Colors.white38)),
              child: const Text('تصفح البنك'),
            ),
          ]),
        ]),
      ),
      if (cfg.contactUrl.isNotEmpty)
        Padding(
          padding: const EdgeInsets.only(top: 10),
          child: OutlinedButton.icon(
            onPressed: () async {
              final uri = Uri.tryParse(cfg.contactUrl);
              if (uri != null) await launchUrl(uri, mode: LaunchMode.externalApplication);
            },
            icon: const Icon(Icons.chat_outlined),
            label: const Text('تواصل معنا'),
          ),
        ),
      const SizedBox(height: 16),
      GridView.count(
        crossAxisCount: 2, shrinkWrap: true, physics: const NeverScrollableScrollPhysics(), childAspectRatio: 1.6, mainAxisSpacing: 10, crossAxisSpacing: 10,
        children: [
          StatTile(Icons.local_fire_department_outlined, '${store.streak}', 'أيام متتالية', color: Colors.orange),
          StatTile(Icons.flag_outlined, '${store.todayCount}/${store.goal}', 'هدف اليوم'),
          StatTile(Icons.event_repeat, '${store.dueCount}', 'مستحقة للمراجعة', color: cs.tertiary),
          StatTile(Icons.insights, store.history.isEmpty ? '—' : '${store.avgScore.round()}%', 'متوسط الاختبارات'),
        ],
      ),
      const SizedBox(height: 6),
      LinearProgressIndicator(value: goalP, minHeight: 8, borderRadius: BorderRadius.circular(8)),
      Align(
        alignment: AlignmentDirectional.centerStart,
        child: TextButton.icon(
          onPressed: () => showModalBottomSheet<void>(
            context: context,
            builder: (c) => SafeArea(
              child: Column(mainAxisSize: MainAxisSize.min, children: [
                const ListTile(title: Text('الهدف اليومي (عدد الأسئلة)')),
                for (final g in [10, 20, 30, 50])
                  ListTile(
                    title: Text('$g سؤالًا'),
                    trailing: store.goal == g ? const Icon(Icons.check) : null,
                    onTap: () {
                      store.setGoal(g);
                      Navigator.pop(c);
                    },
                  ),
              ]),
            ),
          ),
          icon: const Icon(Icons.tune, size: 18),
          label: const Text('تغيير الهدف'),
        ),
      ),
      if (store.history.isNotEmpty) ...[
        const SizedBox(height: 10),
        Text('تطوّر نتائجك', style: Theme.of(context).textTheme.titleLarge),
        const SizedBox(height: 8),
        TrendChart(store.history.take(12).map((e) => e.pct).toList().reversed.toList()),
      ],
      const SizedBox(height: 22),
      Text('الأوسمة (${store.badges.length}/${allBadges.length})', style: Theme.of(context).textTheme.titleLarge),
      const SizedBox(height: 10),
      Wrap(spacing: 8, runSpacing: 8, children: [
        for (final b in allBadges)
          Tooltip(
            message: b.hint,
            child: Chip(
              avatar: Icon(b.icon, size: 18, color: store.badges.contains(b.id) ? Colors.amber.shade800 : null),
              label: Text(b.title),
              backgroundColor: store.badges.contains(b.id) ? Colors.amber.withValues(alpha: .2) : null,
            ),
          ),
      ]),
      const SizedBox(height: 22),
      Text('إتقان الكليات', style: Theme.of(context).textTheme.titleLarge),
      const SizedBox(height: 10),
      for (final fid in bank.facultyIds)
        Card(
          child: ListTile(
            leading: CircleAvatar(backgroundColor: metaOf(fid).color.withOpacity(.15), child: Icon(metaOf(fid).icon, color: metaOf(fid).color)),
            title: Text(bank.facultyTitles[fid]!),
            subtitle: Padding(
              padding: const EdgeInsets.only(top: 6),
              child: LinearProgressIndicator(value: store.mastery(fid), color: metaOf(fid).color, minHeight: 6, borderRadius: BorderRadius.circular(6)),
            ),
            trailing: Text('${bank.count(fid)}'),
          ),
        ),
      if (store.history.isNotEmpty) ...[
        const SizedBox(height: 18),
        Text('آخر الاختبارات', style: Theme.of(context).textTheme.titleLarge),
        for (final r in store.history.take(5))
          ListTile(
            title: Text(r.title),
            subtitle: Text('${r.correct} صحيحة · ${r.wrong} خاطئة · ${r.blank} متروكة'),
            trailing: Text('${r.pct}%', style: TextStyle(fontWeight: FontWeight.w800, color: r.pct >= 60 ? Colors.green : Colors.red)),
          ),
      ],
    ]);
  }
}
