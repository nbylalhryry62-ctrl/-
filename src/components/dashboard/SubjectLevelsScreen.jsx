import { SUBJECTS } from '../../data/constants';
import { QUESTION_BANK, getSubjectLevels } from '../../data/questionBank';

export default function SubjectLevelsScreen({ subjectId, results = [], onBack, onSelectLevel, onOpenSyllabus }) {
  const subject = SUBJECTS[subjectId] || SUBJECTS.biology;
  const levels = getSubjectLevels(subject.id);
  const attempts = results.filter((item) => item.subject === subject.id);
  const best = {};
  for (const item of attempts) {
    const level = String(item.level || '1');
    if (!best[level] || item.percentage > best[level].percentage) best[level] = item;
  }
  return <main className="mx-auto w-full max-w-6xl space-y-5 px-4 py-6 sm:px-6"><div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border bg-white p-5"><div><button onClick={onBack} className="mb-2 text-sm font-semibold text-indigo-700">← العودة للمواد</button><h1 className="text-2xl font-extrabold">{subject.icon} مستويات {subject.name}</h1><p className="mt-1 text-sm text-slate-500">{(QUESTION_BANK[subject.id] || []).length} سؤالًا · {levels.length} مستوى</p></div><div className="flex gap-2"><button onClick={() => onSelectLevel(subject.id, 'random')} className="rounded-xl bg-indigo-600 px-4 py-3 text-sm font-bold text-white">اختبار عشوائي</button><button onClick={() => onSelectLevel(subject.id, 'all')} className="rounded-xl border px-4 py-3 text-sm font-bold">البنك الكامل</button><button onClick={() => onOpenSyllabus(subject.id)} className="rounded-xl border px-4 py-3 text-sm font-bold">المنهج</button></div></div>
    <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">{levels.map((level) => { const result = best[String(level.levelNumber)]; return <article key={level.id} className="flex items-center justify-between gap-3 rounded-2xl border bg-white p-4"><div><h2 className="font-bold">{level.title}</h2><p className="text-xs text-slate-500">{level.rangeLabel} · {level.questionCount} سؤال · {level.difficultyLabel}</p>{result && <p className={`mt-1 text-xs font-bold ${result.percentage >= 50 ? 'text-emerald-700' : 'text-rose-700'}`}>أفضل نتيجة: {result.percentage}%</p>}</div><button onClick={() => onSelectLevel(subject.id, level.id)} className="shrink-0 rounded-lg bg-indigo-50 px-4 py-2 text-sm font-bold text-indigo-700 hover:bg-indigo-100">ابدأ</button></article>; })}</section>
  </main>;
}
