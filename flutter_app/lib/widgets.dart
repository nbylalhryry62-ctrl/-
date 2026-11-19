import 'package:flutter/material.dart';
import 'data.dart';

const brand = Color(0xFF0A7A62);

class StatTile extends StatelessWidget {
  const StatTile(this.icon, this.value, this.label, {super.key, this.color});
  final IconData icon;
  final String value, label;
  final Color? color;
  @override
  Widget build(BuildContext context) {
    final c = color ?? Theme.of(context).colorScheme.primary;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Icon(icon, color: c),
          const SizedBox(height: 8),
          Text(value, style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800)),
          Text(label, style: Theme.of(context).textTheme.bodySmall),
        ]),
      ),
    );
  }
}

/// One question with options. With [instant] the answer is revealed on pick (study mode);
/// otherwise the pick is just recorded through [onPick] (exam mode).
class QuestionCard extends StatefulWidget {
  const QuestionCard({super.key, required this.q, this.instant = true, this.selected, this.onPick, this.onResult, this.index});
  final Question q;
  final bool instant;
  final int? selected;
  final int? index;
  final void Function(int)? onPick;
  final void Function(bool ok)? onResult;
  @override
  State<QuestionCard> createState() => _QuestionCardState();
}

class _QuestionCardState extends State<QuestionCard> {
  int? _picked;

  @override
  void didUpdateWidget(QuestionCard old) {
    super.didUpdateWidget(old);
    if (old.q.id != widget.q.id) _picked = null;
  }

  @override
  Widget build(BuildContext context) {
    final q = widget.q;
    final cs = Theme.of(context).colorScheme;
    final picked = widget.instant ? _picked : widget.selected;
    final revealed = widget.instant && _picked != null;
    final dir = q.en ? TextDirection.ltr : TextDirection.rtl;
    final meta = metaOf(q.fid);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Wrap(spacing: 6, runSpacing: 6, children: [
            Chip(label: Text(q.subject), avatar: CircleAvatar(backgroundColor: meta.color, radius: 5), visualDensity: VisualDensity.compact),
            Chip(label: Text(q.diff), visualDensity: VisualDensity.compact),
          ]),
          const SizedBox(height: 10),
          Directionality(
            textDirection: dir,
            child: Text('${widget.index != null ? '${widget.index}. ' : ''}${q.text}',
                style: Theme.of(context).textTheme.titleMedium?.copyWith(height: 1.7)),
          ),
          const SizedBox(height: 12),
          for (var i = 0; i < q.opts.length; i++)
            Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: _option(context, i, q, picked, revealed, dir, cs),
            ),
          if (revealed && q.expl.isNotEmpty)
            Container(
              margin: const EdgeInsets.only(top: 4),
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: (picked == q.ci ? Colors.green : Colors.red).withOpacity(.12),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Directionality(textDirection: dir, child: Text(q.expl)),
            ),
        ]),
      ),
    );
  }

  Widget _option(BuildContext context, int i, Question q, int? picked, bool revealed, TextDirection dir, ColorScheme cs) {
    Color border = Theme.of(context).dividerColor;
    Color? fill;
    if (revealed) {
      if (i == q.ci) {
        border = Colors.green;
        fill = Colors.green.withOpacity(.14);
      } else if (i == picked) {
        border = Colors.red;
        fill = Colors.red.withOpacity(.14);
      }
    } else if (i == picked) {
      border = cs.primary;
      fill = cs.primary.withOpacity(.12);
    }
    final letters = q.en ? 'ABCDE' : 'أبجده';
    return InkWell(
      borderRadius: BorderRadius.circular(12),
      onTap: revealed
          ? null
          : () {
              if (widget.instant) {
                setState(() => _picked = i);
                widget.onResult?.call(i == q.ci);
              }
              widget.onPick?.call(i);
            },
      child: Container(
        constraints: const BoxConstraints(minHeight: 50),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        decoration: BoxDecoration(color: fill, border: Border.all(color: border, width: 1.5), borderRadius: BorderRadius.circular(12)),
        child: Row(textDirection: dir, children: [
          CircleAvatar(radius: 14, backgroundColor: cs.surfaceContainerHighest, child: Text(letters[i], style: const TextStyle(fontSize: 13))),
          const SizedBox(width: 10),
          Expanded(child: Directionality(textDirection: dir, child: Text(q.opts[i]))),
        ]),
      ),
    );
  }
}

/// Study session: walks through a list of questions with instant feedback and records the answers.
class StudyPage extends StatefulWidget {
  const StudyPage({super.key, required this.title, required this.questions, required this.onDone});
  final String title;
  final List<Question> questions;
  final void Function(List<MapEntry<String, bool>>) onDone;
  @override
  State<StudyPage> createState() => _StudyPageState();
}

class _StudyPageState extends State<StudyPage> {
  int i = 0;
  final answers = <String, bool>{};

  @override
  Widget build(BuildContext context) {
    final q = widget.questions[i];
    final last = i == widget.questions.length - 1;
    final done = answers.containsKey(q.id);
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
        bottom: PreferredSize(preferredSize: const Size.fromHeight(4), child: LinearProgressIndicator(value: (i + 1) / widget.questions.length)),
      ),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        QuestionCard(q: q, index: i + 1, onResult: (ok) => setState(() => answers[q.id] = ok)),
      ]),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: FilledButton(
            onPressed: !done
                ? null
                : () {
                    if (last) {
                      widget.onDone(answers.entries.toList());
                      final ok = answers.values.where((e) => e).length;
                      showDialog<void>(
                        context: context,
                        builder: (c) => AlertDialog(
                          title: const Text('انتهت الجلسة'),
                          content: Text('أجبت صح عن $ok من ${answers.length}'),
                          actions: [TextButton(onPressed: () => Navigator.pop(c), child: const Text('حسنًا'))],
                        ),
                      ).then((_) {
                        if (context.mounted) Navigator.pop(context);
                      });
                    } else {
                      setState(() => i++);
                    }
                  },
            child: Text(last ? 'إنهاء' : 'التالي'),
          ),
        ),
      ),
    );
  }
}
