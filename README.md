# بوابة القبول الذكية - جامعة عدن (2026 / 2027م)

[![Validate question bank](../../actions/workflows/validate.yml/badge.svg)](../../actions/workflows/validate.yml)

بنك أسئلة من **7,654 سؤالًا** موزعة على 7 كليات و154 محورًا دراسيًا، مع تطبيق ويب تقدمي (PWA) يعمل دون اتصال، ووحدة TypeScript لتطبيقات React Native / Expo.

*English: question bank (7,654 items, 7 faculties, 154 syllabus axes), an offline-capable PWA, and an Expo/TypeScript service for the University of Aden admission exam.*

تطوير وإعداد: نبيل الحريري (<https://wa.me/967778763924>)

## البنية

| المسار | المحتوى |
|---|---|
| `data/qbank_full_7654.json` | البنك الكامل بصيغة JSON |
| `data/faculties/` | ملف لكل كلية للتحميل الكسول |
| `data/syllabus_map_154.json` | خريطة المفردات (مجموع أعدادها = حجم البنك) |
| `data/needs_review_30.json` | 30 سؤالًا معزولًا بانتظار مراجعة الأصل الورقي |
| `csv/` | ملفات CSV بترميز UTF-8-SIG للفتح في Excel |
| `database/aden_qbank_7654.sqlite` | قاعدة SQLite مفهرسة لـ `expo-sqlite` |
| `expo-integration/qbankService.ts` | خدمة TypeScript مع دالة `buildDetailedDiagnosticCorrection` للتشخيص الفوري |
| `pwa-deploy/` | التطبيق (`index.html` يضمّن البنك مضغوطًا)، `sw.js`، `manifest.json`، الأيقونات، `netlify.toml` |
| `scripts/` | `validate_bank.py` للفحص، و`embed_bank.py` لإعادة تضمين البنك في الصفحة |

## الكليات

| الكلية | الأسئلة |
|---|---|
| الكليات الطبية وكلية التمريض | 1,140 |
| كلية الحاسوب وتكنولوجيا المعلومات | 1,378 |
| كلية الهندسة | 1,136 |
| كلية الحقوق | 1,000 |
| كلية الاقتصاد والعلوم السياسية | 1,000 |
| كلية اللغات والترجمة | 1,000 |
| كلية التربية الرياضية والتغذية العلاجية | 1,000 |

## التشغيل محليًا

```bash
cd pwa-deploy
python3 -m http.server 8080   # ثم افتح http://localhost:8080
```

يحتاج التطبيق اتصالًا في أول زيارة لتحميل Tailwind والأيقونات والخطوط، ثم يعمل دون اتصال.

## النشر

- **GitHub Pages:** فعّل Pages من Settings ثم Pages واختر Source: GitHub Actions. يرفع `.github/workflows/pages.yml` مجلد `pwa-deploy` عند كل push إلى `main`.
- **Netlify / Cloudflare Pages:** انشر مجلد `pwa-deploy` (الإعدادات في `netlify.toml`).

## تعديل البنك

1. عدّل الأسئلة في `data/qbank_full_7654.json`.
2. شغّل `python3 scripts/embed_bank.py` لتحديث النسخة المضمّنة في الصفحة.
3. حدّث ملفات الكليات وCSV وSQLite بما يطابق التعديل.
4. شغّل `python3 scripts/validate_bank.py`. يشغّله GitHub Actions تلقائيًا عند كل push.

يتحقق الفحص من: تفرّد المعرفات، وعدم تكرار الخيارات، وتطابق مفتاح الإجابة مع نصها، ووجود تعليل لكل سؤال، وتطابق الأعداد في JSON وCSV وSQLite وخريطة المفردات والبنك المضمّن.

## ملاحظات

- ميزات الذكاء الاصطناعي في الواجهة تحتاج مفتاح Gemini في `GeminiService.apiKey` داخل `pwa-deploy/index.html`. لا تضع مفتاحًا حقيقيًا في مستودع عام.
- سجل التغييرات في [CHANGELOG.md](CHANGELOG.md).
