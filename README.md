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
| `pwa-deploy/` | التطبيق: `index.html` (هيكل فقط)، `css/` (5 ملفات: tokens وbase وcomponents وshell وviews)، `js/`، `sw.js`، `manifest.json`، `icons/`، `netlify.toml` |
| `scripts/` | `validate_bank.py` للفحص، `build_data.py` لتجميع البنك في ملف التطبيق، `build_single.py` لإنتاج صفحة واحدة مستقلة |

## بنية الواجهة (`pwa-deploy/js`)

كل طبقة في مجلد مستقل، ولا يوجد كود مكرر بين الشاشات: بطاقة السؤال وشريط التقدم والحلقة والنوافذ تُبنى من مكان واحد.

| المجلد | المسؤولية |
|---|---|
| `data/` | `syllabus.js` (المنهج)، `qbank-data.js` (البنك المضغوط، يُولَّد آليًا)، `bank.js` (فك الضغط والاستعلام) |
| `core/` | `store` (تقدم المتعلم)، `storage` (تخزين آمن)، `router` (مسارات `#home` وغيرها)، `actions` (أحداث `data-act` بلا onclick) |
| `ui/` | الأيقونات، المظهر، تنسيق الرياضيات، المكونات المشتركة، بطاقة السؤال، الهيكل العام |
| `features/` | `exam.js` (محرك الاختبار والمؤقت)، `progress.js` (الجاهزية والإحصاءات) |
| `views/` | شاشة لكل ملف: الرئيسية، المنهج، البنك، الاختبار، دفتر الأخطاء |

لا يعتمد التطبيق على Tailwind أو مكتبة أيقونات خارجية، وتحتاج الخطوط فقط اتصالًا في أول زيارة (وإلا تُستخدم خطوط النظام).

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
2. شغّل `python3 scripts/build_data.py` لإعادة توليد `pwa-deploy/js/data/qbank-data.js`.
3. حدّث ملفات الكليات وCSV وSQLite بما يطابق التعديل.
4. شغّل `python3 scripts/validate_bank.py`. يشغّله GitHub Actions تلقائيًا عند كل push.

يتحقق الفحص من: تفرّد المعرفات، وعدم تكرار الخيارات، وتطابق مفتاح الإجابة مع نصها، ووجود تعليل لكل سؤال، وتطابق الأعداد في JSON وCSV وSQLite وخريطة المفردات والبنك المضمّن.

## ملاحظات

- صفحة واحدة مستقلة للمشاركة: `python3 scripts/build_single.py` ينتج `dist/aden-portal.html`.
- أُزيلت من هذا الإصدار شاشة «استوديو الذكاء الاصطناعي» لأنها كانت تتطلب مفتاح Gemini فارغًا في الكود، وكذلك أدوات تصدير TypeScript وSQL وملفات PWA التي تغطيها الآن سكربتات المستودع.
- سجل التغييرات في [CHANGELOG.md](CHANGELOG.md).
