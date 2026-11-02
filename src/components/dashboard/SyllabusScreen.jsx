import { useEffect, useState } from 'react';
import { SUBJECTS } from '../../data/constants';
import { SYLLABUS_DATA } from '../../data/syllabusData';
import { QUESTION_BANK, getSubjectLevels } from '../../data/questionBank';

export default function SyllabusScreen({ initialSubject = 'english', onBack, onOpenLevels }) {
  const [active, setActive] = useState(SUBJECTS[initialSubject] ? initialSubject : 'english');
  useEffect(() => { if (SUBJECTS[initialSubject]) setActive(initialSubject); }, [initialSubject]);
  const syllabus = SYLLABUS_DATA[active] || SYLLABUS_DATA.english;
  return <main className="mx-auto w-full max-w-5xl space-y-5 px-4 py-6 sm:px-6"><div className="flex flex-wrap items-center justify-between gap-3"><div><h1 className="text-2xl font-extrabold">مفردات المنهج المقررة</h1><p className="text-sm text-slate-500">الموضوعات المعتمدة للمواد الثلاث.</p></div><button onClick={onBack} className="rounded-xl border bg-white px-4 py-2 font-semibold">العودة</button></div>
    <div className="grid grid-cols-3 gap-2">{Object.values(SUBJECTS).map((item) => <button key={item.id} onClick={() => setActive(item.id)} className={`rounded-xl border p-3 text-sm font-bold ${active === item.id ? 'border-indigo-600 bg-indigo-50 text-indigo-700' : 'bg-white'}`}>{item.icon} {item.name}<span className="block text-xs font-normal text-slate-500">{(QUESTION_BANK[item.id] || []).length} سؤال</span></button>)}</div>
    <section className="space-y-4 rounded-2xl border bg-white p-5 sm:p-7"><header className="flex flex-wrap items-center justify-between gap-3 border-b pb-4"><h2 className="text-xl font-extrabold">{syllabus.title}</h2>{(QUESTION_BANK[active] || []).length > 0 && <button onClick={() => onOpenLevels(active)} className="rounded-xl bg-indigo-600 px-4 py-2 font-bold text-white">عرض {getSubjectLevels(active).length} مستوى</button>}</header>
      {syllabus.sections.map((section, index) => <article key={`${active}-${index}`} className="rounded-xl bg-slate-50 p-4"><h3 className="mb-3 border-b pb-2 font-extrabold text-indigo-900">{section.heading}</h3><ul className="grid gap-2 sm:grid-cols-2">{section.items.map((item, itemIndex) => <li key={`${index}-${itemIndex}`} className="rounded-lg bg-white p-3 text-sm text-slate-700">• {item}</li>)}</ul></article>)}
    </section>
  </main>;
}
