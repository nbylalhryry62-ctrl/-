# بوابة القبول - نسخة Flutter

نسخة أصلية (Dart) من التطبيق: الرئيسية، تصفح البنك مع بحث وتصفية، اختبارات بمؤقت، مراجعة متباعدة (لايتنر)، دفتر أخطاء، هدف يومي وسلسلة أيام، ووضع ليلي. تعمل دون اتصال.

```bash
python3 ../scripts/build_flutter_data.py   # يولّد assets/qbank.json.gz من data/
flutter create . --platforms=android,ios --org ye.edu.aden --project-name aden_portal
flutter pub get && flutter test && flutter run
flutter build apk --release
```
مجلدات المنصات (android/ios) تُولَّد بالأمر `flutter create` ولا تُحفظ في المستودع.
