/**
 * بوابة القبول الذكية - جامعة عدن (2026 / 2027م)
 * وحدة إدارة بنك الأسئلة الشامل (7,654 سؤالاً) والتشخيص الفوري لتطبيق React Native / Expo
 * تطوير وإعداد: نبيل الحريري (https://wa.me/967778763924)
 */

export interface AdmissionQuestion {
  id: string;
  facultyId: 'medical' | 'sports_nutrition' | 'law' | 'cs_it' | 'economics_politics' | 'languages' | 'engineering';
  facultyTitle: string;
  trackId: string;
  subjectId: string;
  subjectName: string;
  unit: string;
  lesson?: string;
  gradeRef?: string;
  unitRef: string;
  difficulty: 'سهل' | 'متوسط' | 'متقدم' | 'نماذج القبول';
  lang: 'ar' | 'en';
  question: string;
  options: string[];
  correctIndex: number;
  correctAnswerText: string;
  explanation: string;
  sourceBank: 'unified_6000' | 'reviewed_1687';
}

export interface DiagnosticCorrection {
  isCorrect: boolean;
  selectedOptionIndex: number;
  selectedOptionLetter: string;
  selectedOptionText: string;
  correctOptionIndex: number;
  correctOptionLetter: string;
  correctOptionText: string;
  whyWrongExplanation: string | null;
  whyCorrectExplanation: string;
  excludedDistractorsSummary: string;
  syllabusReference: string;
}

/**
 * دالة التشخيص الفوري التلقائي عند اختيار الطالب للإجابة
 * توضح سبب خطأ إجابة الطالب (إن كانت خاطئة) وسبب صحة الإجابة المعتمدة علمياً ومنهجياً
 */
export function buildDetailedDiagnosticCorrection(
  q: AdmissionQuestion,
  selectedIdx: number
): DiagnosticCorrection {
  const isLTR = q.lang === 'en';
  const letters = isLTR ? ['A', 'B', 'C', 'D', 'E'] : ['أ', 'ب', 'ج', 'د', 'هـ'];
  const isCorrect = selectedIdx === q.correctIndex;
  const userOpt = q.options[selectedIdx] ?? '';
  const correctOpt = q.options[q.correctIndex] ?? '';
  const userLetter = letters[selectedIdx] || String(selectedIdx + 1);
  const correctLetter = letters[q.correctIndex] || String(q.correctIndex + 1);
  const rawExpl = q.explanation || 'وفق القاعدة المعتمدة في المنهج الثانوي اليمني ودليل جامعة عدن.';

  let whyWrongExplanation: string | null = null;
  if (!isCorrect) {
    if (q.lang === 'en') {
      whyWrongExplanation = `اختيارك للبديل (${userLetter}: ${userOpt}) غير صحيح في هذا السياق؛ لأن هذا الخيار لا يحقق القاعدة اللغوية أو المعنى الاصطلاحي الدقيق المطلوب في محور (${q.unit})، بل يمثل مشتتاً يختلف في الزمن القواعدي أو الوظيفة اللغوية أو الدلالة المعجمية عن الجواب الصحيح (${correctLetter}: ${correctOpt}).`;
    } else if (q.subjectId.includes('math')) {
      whyWrongExplanation = `اختيارك للبديل (${userLetter}: ${userOpt}) غير صحيح رياضياً؛ لأن تطبيق القانون أو القاعدة الرياضية المقررة في محور (${q.unit}) على معطيات السؤال يؤدي حسابياً ومنطقياً إلى النتيجة (${correctOpt})، بينما ينتج الخيار (${userOpt}) عادةً عن خطأ في الإشارة أو التعويض أو الخلط بين قوانين الوحدة.`;
    } else if (q.subjectId.includes('chem') || q.subjectId.includes('phys') || q.subjectId.includes('bio')) {
      whyWrongExplanation = `اختيارك للبديل (${userLetter}: ${userOpt}) غير صحيح علمياً؛ لأن هذا البديل لا ينطبق على المفهوم أو التفاعل أو القانون العلمي المسؤول عنه في السؤال ضمن وحدة (${q.unit})، وإنما يرتبط بخصائص أو حالات أخرى مختلفة عن (${correctOpt}).`;
    } else {
      whyWrongExplanation = `اختيارك للبديل (${userLetter}: ${userOpt}) غير صحيح؛ لأنه يخالف الحكم أو القاعدة المقررة في محور (${q.unit} — ${q.subjectName}) ولا يطابق المفهوم الدقيق الذي يحدده السؤال وهو (${correctOpt}).`;
    }
  }

  const distractors = q.options.filter((_, idx) => idx !== q.correctIndex).join(' ، ');
  const excludedDistractorsSummary = `البدائل الأخرى (${distractors}) لا تنطبق على المعطى المحدد في محور ${q.unit} (${q.gradeRef || 'المنهج المعتمد'}).`;

  return {
    isCorrect,
    selectedOptionIndex: selectedIdx,
    selectedOptionLetter: userLetter,
    selectedOptionText: userOpt,
    correctOptionIndex: q.correctIndex,
    correctOptionLetter: correctLetter,
    correctOptionText: correctOpt,
    whyWrongExplanation,
    whyCorrectExplanation: rawExpl,
    excludedDistractorsSummary,
    syllabusReference: `${q.subjectName} • ${q.unitRef}${q.lesson ? ` (${q.lesson})` : ''}`,
  };
}
