import 'dart:async';
import 'package:flutter/material.dart';
import '../data.dart';
import '../store.dart';
import '../widgets.dart';

class ExamScreen extends StatefulWidget {
  const ExamScreen({super.key, required this.store});
  final AppStore store;
  @override
  State<ExamScreen> createState() => _ExamScreenState();
}

class _ExamScreenState extends State<ExamScreen> {
  String? fid;
  int count = 20;
  int minutes = 20;

  @override
  Widget build(BuildContext context) {
    final bank = widget.store.bank;
    final pool = bank.query(fid: fid);
    final n = count > pool.length ? pool.length : count;
    return ListView(padding: const EdgeInsets.all(16), children: [
      Text('اختبار تجريبي', style: Theme.of(context).textTheme.headlineSmall),
      const SizedBox(height: 14),
      Wrap(spacing: 8, runSpacing: 8, children: [
        ChoiceChip(label: const Text('كل الكليات'), selected: fid == null, onSelected: (_) => setState(() => fid = null)),
        for (final f in bank.facultyIds)
          ChoiceChip(label: Text(bank.facultyTitles[f]!), selected: fid == f, selectedColor: metaOf(f).color.withOpacity(.2), onSelected: (_) => setState(() => fid = f)),
      ]),
      const SizedBox(height: 18),
      Text('عدد الأسئلة', style: Theme.of(context).textTheme.titleMedium),
      const SizedBox(height: 6),
      SegmentedButton<int>(
        showSelectedIcon: false,
        segments: [for (final c in [10, 20, 30, 50]) ButtonSegment(value: c, label: Text('$c'))],
        selected: {count},
        onSelectionChanged: (s) => setState(() => count = s.first),
      ),
      const SizedBox(height: 18),
      Text('المدة: $minutes دقيقة', style: Theme.of(context).textTheme.titleMedium),
      Slider(value: minutes.toDouble(), min: 5, max: 90, divisions: 17, label: '$minutes', onChanged: (v) => setState(() => minutes = v.round())),
      const SizedBox(height: 10),
      FilledButton(
        onPressed: n == 0
            ? null
            : () => Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (_) => ExamRun(
                      store: widget.store,
                      title: fid == null ? 'اختبار شامل' : bank.facultyTitles[fid]!,
                      questions: bank.sample(pool, n),
                      minutes: minutes,
                    ),
                  ),
                ),
        child: Text('ابدأ ($n سؤالًا)'),
      ),
    ]);
  }
}

class ExamRun extends StatefulWidget {
  const ExamRun({super.key, required this.store, required this.title, required this.questions, required this.minutes});
  final AppStore store;
  final String title;
  final List<Question> questions;
  final int minutes;
  @override
  State<ExamRun> createState() => _ExamRunState();
}

class _ExamRunState extends State<ExamRun> with WidgetsBindingObserver {
  late final DateTime endsAt = DateTime.now().add(Duration(minutes: widget.minutes));
  final answers = <String, int>{};
  int i = 0;
  Timer? _timer;
  int left = 0;
  bool finished = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    left = _remaining();
    _timer = Timer.periodic(const Duration(seconds: 1), (_) => _tick());
  }

  int _remaining() => endsAt.difference(DateTime.now()).inSeconds.clamp(0, 1 << 30);

  // The timer follows the wall clock, so it stays correct if the app is backgrounded.
  void _tick() {
    if (!mounted || finished) return;
    setState(() => left = _remaining());
    if (left == 0) _submit();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState s) {
    if (s == AppLifecycleState.resumed) _tick();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _timer?.cancel();
    super.dispose();
  }

  void _submit() {
    if (finished) return;
    finished = true;
    _timer?.cancel();
    var c = 0, w = 0;
    final rec = <MapEntry<String, bool>>[];
    for (final q in widget.questions) {
      final a = answers[q.id];
      if (a == null) continue;
      final ok = a == q.ci;
      ok ? c++ : w++;
      rec.add(MapEntry(q.id, ok));
    }
    widget.store.record(rec);
    final r = ExamResult(widget.title, widget.questions.length, c, w, widget.questions.length - c - w, DateTime.now().millisecondsSinceEpoch);
    widget.store.addResult(r);
    Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => ResultPage(result: r, questions: widget.questions, answers: answers)));
  }

  Future<bool> _confirmExit() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: const Text('إنهاء الاختبار؟'),
        content: Text('أجبت عن ${answers.length} من ${widget.questions.length}.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('متابعة')),
          FilledButton(onPressed: () => Navigator.pop(c, true), child: const Text('إنهاء وتسليم')),
        ],
      ),
    );
    return ok ?? false;
  }

  @override
  Widget build(BuildContext context) {
    final q = widget.questions[i];
    final mm = (left ~/ 60).toString().padLeft(2, '0');
    final ss = (left % 60).toString().padLeft(2, '0');
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) async {
        if (!didPop && await _confirmExit()) _submit();
      },
      child: Scaffold(
        appBar: AppBar(
          title: Text('$mm:$ss', style: TextStyle(fontWeight: FontWeight.w800, color: left < 60 ? Colors.red : null)),
          actions: [TextButton(onPressed: () async { if (await _confirmExit()) _submit(); }, child: const Text('تسليم'))],
          bottom: PreferredSize(preferredSize: const Size.fromHeight(4), child: LinearProgressIndicator(value: answers.length / widget.questions.length)),
        ),
        body: ListView(padding: const EdgeInsets.all(16), children: [
          QuestionCard(q: q, index: i + 1, instant: false, selected: answers[q.id], onPick: (a) => setState(() => answers[q.id] = a)),
          const SizedBox(height: 12),
          Wrap(spacing: 6, runSpacing: 6, children: [
            for (var k = 0; k < widget.questions.length; k++)
              ActionChip(
                label: Text('${k + 1}'),
                backgroundColor: k == i ? Theme.of(context).colorScheme.primary : (answers.containsKey(widget.questions[k].id) ? Theme.of(context).colorScheme.primaryContainer : null),
                labelStyle: TextStyle(color: k == i ? Colors.white : null),
                onPressed: () => setState(() => i = k),
              ),
          ]),
        ]),
        bottomNavigationBar: SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Row(children: [
              Expanded(child: OutlinedButton(onPressed: i > 0 ? () => setState(() => i--) : null, child: const Text('السابق'))),
              const SizedBox(width: 10),
              Expanded(child: FilledButton(onPressed: i < widget.questions.length - 1 ? () => setState(() => i++) : null, child: const Text('التالي'))),
            ]),
          ),
        ),
      ),
    );
  }
}

class ResultPage extends StatelessWidget {
  const ResultPage({super.key, required this.result, required this.questions, required this.answers});
  final ExamResult result;
  final List<Question> questions;
  final Map<String, int> answers;

  @override
  Widget build(BuildContext context) {
    final r = result;
    final wrong = questions.where((q) => answers[q.id] != null && answers[q.id] != q.ci).toList();
    final msg = r.pct >= 80 ? 'ممتاز، مستواك قوي' : r.pct >= 60 ? 'جيد، واصل التدريب' : 'تحتاج مراجعة المنهج ثم إعادة المحاولة';
    return Scaffold(
      appBar: AppBar(title: const Text('النتيجة')),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        Center(
          child: SizedBox(
            width: 140, height: 140,
            child: Stack(alignment: Alignment.center, children: [
              SizedBox.expand(child: CircularProgressIndicator(value: r.pct / 100, strokeWidth: 12, color: r.pct >= 60 ? Colors.green : Colors.red)),
              Text('${r.pct}%', style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w800)),
            ]),
          ),
        ),
        const SizedBox(height: 14),
        Center(child: Text(msg, style: Theme.of(context).textTheme.titleMedium)),
        const SizedBox(height: 6),
        Center(child: Text('${r.correct} صحيحة · ${r.wrong} خاطئة · ${r.blank} متروكة')),
        const SizedBox(height: 18),
        if (wrong.isNotEmpty) Text('الأسئلة الخاطئة', style: Theme.of(context).textTheme.titleLarge),
        for (final q in wrong) QuestionCard(q: q, instant: false, selected: answers[q.id], onPick: null),
        FilledButton(onPressed: () => Navigator.pop(context), child: const Text('عودة')),
      ]),
    );
  }
}
