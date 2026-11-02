# Aden Medical Test — تطبيق اختبار القبول الطبي (جامعة عدن)

تطبيق أندرويد متكامل مبني بلغة **Kotlin** وواجهات **Jetpack Compose** مع دعم كامل للغة العربية واتجاه اليمين لليسار (RTL) وفق معايير **Material Design 3**.

التطبيق مصمم لطلاب كلية الطب والعلوم الصحية بجامعة عدن لاجتياز اختبارات القبول والمفاضلة في مواد: الأحياء، الكيمياء، واللغة الإنجليزية.

## المميزات الأساسية (Core Features)

1. **بنك الأسئلة الشامل (1,142 سؤالاً أصلياً)**:
   - **الأحياء**: 179 سؤالاً (الإحساس والتنظيم العصبي، التغذية والهضم، ونماذج السنوات السابقة لكليات الطب البشري، الأسنان، الصيدلة، المختبرات، والتمريض).
   - **الكيمياء**: 525 سؤالاً (اختيار من متعدد، مسائل حسابية، صح وخطأ، وتفاعلات عضوية وغير عضوية).
   - **اللغة الإنجليزية**: 438 سؤالاً (المفردات، القواعد والأزمنة، والمصطلحات الطبية المعيارية).
2. **نظام الاختبارات والمستويات الذكي**:
   - تقسيم المادة إلى مستويات تدريجية (25 سؤالاً لكل مستوى) مصنفة حسب الصعوبة (مستوى تأسيسي، مستوى متوسط، مستوى متقدم).
   - وضع الاختبار العشوائي (Random Exam) لمحاكاة ورقة الامتحان الفعلي في 10 دقائق.
   - مؤقت عد تنازلي تفاعلي مع تنبيهات عند اقتراب انتهاء الوقت.
   - تنبيهات واضحة قبل التسليم في حال وجود أسئلة غير مجابة.
3. **تقرير النتائج ومراجعة الإجابات**:
   - احتساب الدرجة ونسبة النجاح المعيارية (النجاح من 50%).
   - استعراض فوري ومفصل لكل سؤال، مع توضيح إجابة الطالب مقابل الإجابة الصحيحة المعتمدة.
4. **المصادقة السحابية وسجل النتائج (Firebase Auth & Cloud Firestore)**:
   - تسجيل الدخول الآمن عبر حساب جوجل باستخدام **Jetpack Credential Manager** و **Firebase Authentication**.
   - حفظ ومزامنة ملف الطالب وسجل كافة المحاولات والنتائج لحظياً عبر **Cloud Firestore** (`/users/{userId}` و `/users/{userId}/results/{resultId}`) مع قواعد أمان صارمة (`firestore.rules`).
   - إمكانية إعادة أي اختبار سابق بلمسة واحدة.
5. **مفردات المنهج المعتمدة (Syllabus)**:
   - دليل مفصل للموضوعات الوزارية المقررة للصفوف الثانوية الثلاثة.
6. **أيقونة التطبيق التكيفية (Adaptive Launcher Icon)**:
   - أيقونة طبية مخصصة تجمع بين شعار الطب، قبعة التخرج، والكتاب بلون نيلي كحلي وأخضر زمردي مع دعم كامل لجميع مقاسات mipmap (mdpi إلى xxxhdpi).

## البنية الهندسية (Android Project Structure)

```text
app/
├── src/main/
│   ├── assets/
│   │   ├── question_bank.json           # 1,142 سؤالاً مع الخيارات والمفاتيح الصحيحة
│   │   └── syllabus_data.json           # المفردات والموضوعات المعتمدة
│   ├── java/com/example/adenmedicaltest/
│   │   ├── MainActivity.kt              # نقطة البداية مع تفعيل Edge-to-Edge و RTL وبوابة المصادقة
│   │   ├── data/
│   │   │   ├── model/Models.kt          # نماذج البيانات (Subject, Question, ExamResult, UserProfile, Syllabus)
│   │   │   ├── remote/                  # FirestoreErrorHandler ومعالجة أخطاء الشبكة والصلاحيات
│   │   │   └── repository/              # ExamRepository, SyllabusRepository, AuthRepository
│   │   ├── ui/
│   │   │   ├── auth/GoogleAuthHelper.kt # تسجيل الدخول بحساب Google عبر Credential Manager
│   │   │   ├── theme/                   # Material 3 Theme, Typography, Colors
│   │   │   ├── navigation/              # Destinations & Routes
│   │   │   ├── viewmodel/MainViewModel  # إدارة حالة الاختبار، المؤقت، والنتائج السحابية
│   │   │   ├── components/AppTopBar     # الشريط العلوي والتنقل
│   │   │   └── screens/                 # شاشات Compose (Auth, Dashboard, Exam, Result, History, Syllabus, Profile, Admin)
│   └── res/
│       ├── drawable/                    # أيقونات المتجهات وخلفيات Launcher
│       ├── mipmap-*/                    # أيقونات التطبيق التكيفية بمختلف الكثافات
│       └── values/                      # strings.xml و firebase_applet_config.xml
```

## المتطلبات والبناء (Build & Run)

- **Language**: Kotlin 2.2+
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 15 (API 35/36)
- **Backend & Cloud**: Firebase Authentication, Cloud Firestore (`firebase-bom`), Jetpack Credential Manager
- **Gradle**: Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`, `libs.versions.toml`)
