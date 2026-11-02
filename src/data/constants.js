export const PASSING_PERCENTAGE = 50; // نسبة النجاح القابلة للتعديل من ثابت واحد
export const EXAM_DURATION_SECONDS = 600; // 10 دقائق = 600 ثانية

export const SUBJECTS = {
  biology: {
    id: 'biology',
    name: 'الأحياء',
    icon: '🧬',
    description: 'الإحساس والتنظيم العصبي (1-85) + التغذية والهضم (87-126) + نماذج طب بشري وأسنان وصيدلة ومختبرات وتمريض جامعة عدن — إجمالي 179 سؤالاً',
    durationMinutes: 10,
    badgeBg: 'bg-emerald-100 text-emerald-800 border-emerald-200',
    cardAccent: 'from-emerald-500 to-teal-600',
    iconBg: 'bg-emerald-50 text-emerald-600 border-emerald-200'
  },
  chemistry: {
    id: 'chemistry',
    name: 'الكيمياء',
    icon: '⚗️',
    description: 'اختيار من متعدد (1-43) + مسائل حسابية (44-75) + صح وخطأ وعلاقات كيميائية ونماذج طب أسنان — إجمالي 525 سؤالاً شاملاً',
    durationMinutes: 10,
    badgeBg: 'bg-amber-100 text-amber-800 border-amber-200',
    cardAccent: 'from-amber-500 to-orange-600',
    iconBg: 'bg-amber-50 text-amber-600 border-amber-200'
  },
  english: {
    id: 'english',
    name: 'اللغة الإنجليزية',
    icon: '📖',
    description: 'المفردات والقواعد والأزمنة (1-88) + القواعد والمفردات الطبية (150 قاعدة + 200 مفردة طبية: 89-438) — إجمالي 438 سؤالاً شاملاً',
    durationMinutes: 10,
    badgeBg: 'bg-indigo-100 text-indigo-800 border-indigo-200',
    cardAccent: 'from-indigo-500 to-purple-600',
    iconBg: 'bg-indigo-50 text-indigo-600 border-indigo-200'
  }
};


export const ARABIC_NAME_REGEX = /^[\u0600-\u06FF\s]{3,100}$/;
