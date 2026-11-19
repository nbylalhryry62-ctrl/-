# إعداد Firebase (مرة واحدة)

1. أنشئ مشروعًا في https://console.firebase.google.com وفعّل **Firestore** (وضع الإنتاج).
2. **Authentication** ← Sign-in method ← فعّل «البريد وكلمة المرور» **وعطّل التسجيل الذاتي** (أنشئ حساب المشرف من تبويب Users يدويًا).
3. في Firestore أنشئ مجموعة `admins` وفيها مستند **معرّفه = UID حساب المشرف** (أي حقل بداخله، مثل `ok: true`). هذا هو الذي يمنح الصلاحية.
4. انشر القواعد: `firebase deploy --only firestore:rules` (أو الصق محتوى `firestore.rules` في تبويب Rules).
5. أنشئ مستند `app_config/main` (اختياري، تنشئه لوحة المشرف تلقائيًا عند أول حفظ).
6. من Project settings انسخ **Web API Key** و**Project ID**، ثم في GitHub: Settings ← Secrets and variables ← Actions ← **Variables**:
   `FIREBASE_API_KEY` و`FIREBASE_PROJECT_ID`. بعدها يبني سير العمل «Flutter app» نسخة مقفلة بالرموز.
   محليًا: `flutter run --dart-define=FIREBASE_API_KEY=... --dart-define=FIREBASE_PROJECT_ID=...`

بدون هذين المتغيرين يعمل التطبيق مفتوحًا دون بوابة ولا مزامنة.

## ملاحظات أمنية
- مفتاح الـ API في Firebase معرّف عام وليس سريًا؛ الحماية الفعلية في `firestore.rules`.
- معرّف الجهاز رقم عشوائي يُخزَّن في التخزين الآمن. إذا مسح المستخدم بيانات التطبيق أو أعاد تثبيته فسيحتاج المشرف إلى «إعادة ضبط الجهاز».
- يقرأ أي شخص يعرف الرمز مستندَه (لا يمكنه سرد الرموز). الرمز 12 خانة عشوائية (60 بت) فتخمينه غير عملي، وتضيف الواجهة تأخيرًا بعد المحاولات الفاشلة. للحماية الأقوى فعّل **App Check**.
- بعد 30 يومًا دون أي اتصال ناجح بالخادم يُطلب من المستخدم الاتصال لإعادة التحقق.
