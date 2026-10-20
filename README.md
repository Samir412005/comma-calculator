# آلة حاسبة — مشروع Android 1.1.0

مشروع Java/Gradle كامل لإنتاج APK عبر GitHub Actions، وليس APK مبنياً. لم يُنفّذ بناء Gradle أو اختبارات Java أو تشغيل الواجهة لهذه النسخة في البيئة الحالية. أول تشغيل ناجح لخط CI هو التحقق الفعلي من البناء والاختبارات.

## التطوير الموجود في المصدر
- واجهة عربية، ثيم فاتح/داكن/حسب النظام.
- سجل محلي لآخر 60 عملية، استرجاع نتيجة ومسح السجل بتأكيد.
- الجذر والتربيع والمقلوب، بالإضافة للحساب العشري والنسبة وتغيير الإشارة.
- حفظ المسودة واستعادة حالة الواجهة. لا أذونات ولا اتصال شبكي داخل التطبيق.

## البناء في GitHub
1. أنشئ مستودع GitHub وارفع محتويات هذا المجلد إلى جذره، بما فيها `.github`.
2. من Settings → Secrets and variables → Actions أضف الأسرار الأربعة:
   - `KEYSTORE_BASE64`: ملف توقيع JKS مشفّر Base64.
   - `KEYSTORE_PASSWORD`: كلمة مرور مخزن التوقيع.
   - `KEY_ALIAS`: الاسم المستعار للمفتاح (عادة `calculator`).
   - `KEY_PASSWORD`: كلمة مرور المفتاح.
3. من Actions شغّل **Build signed calculator APK**.
4. افتراضياً يجب أن يطابق المفتاح شهادة APK الأصلي. اترك `allow_new_signature` مغلقاً لتحديث التطبيق المثبّت.
5. بعد النجاح حمّل artifact باسم `calculator-1.1.0-signed`، وفكّه لتحصل على `app-release.apk` وبصمة SHA256 وتقرير التوقيع. تقارير JUnit في artifact منفصل.

المتطلبات المثبّتة في CI: JDK 17، Gradle 8.9، Android Gradle Plugin 8.7.3، SDK/build-tools 35. لا نحتاج gradle-wrapper.jar؛ setup-gradle يثبّت Gradle.

## المفتاح الأصلي أو إنشاء توقيع جديد
ملف ZIP لا يحتوي أي مفتاح أو كلمة مرور. لتحديث APK الأصلي تحتاج مالك مفتاحه الأصلي لإضافته إلى أسرار المستودع بطريقة خاصة وآمنة. لا تنشر ملف التوقيع ولا قيم الأسرار في الرسائل أو Git.

بصمة الشهادة الأصلية SHA256:
`52a51e0154dfe461798f73e207f14f67a4f8342599092a7efd7d0ffe44a4f81d`

إذا لم يتوفر المفتاح الأصلي، أنشئ مفتاحاً جديداً على جهاز موثوق (سيطلب كلمات المرور تفاعلياً):

```sh
keytool -genkeypair -keystore calculator-release.jks -storetype JKS -alias calculator -keyalg RSA -keysize 3072 -validity 10000
```

لإنشاء ملف Base64 محلي من دون طباعته على الشاشة:

```sh
python3 -c "import base64,pathlib; pathlib.Path('keystore-base64.txt').write_text(base64.b64encode(pathlib.Path('calculator-release.jks').read_bytes()).decode())"
```

انسخ محتوى هذا الملف إلى `KEYSTORE_BASE64` ثم احذف نسخة Base64 المؤقتة واحفظ JKS وكلمات مروره في مكان آمن واحتياطي. لا ترفعهما إلى المستودع. عند استخدام المفتاح الجديد فعّل `allow_new_signature` عمداً.

**المفتاح الجديد لا يستطيع تحديث النسخة القديمة.** يلزم إلغاء تثبيتها أولاً، ما يمسح سجلها وإعداداتها، أو تغيير applicationId لبناء تطبيق مستقل. احفظ نفس المفتاح الجديد لكل تحديث لاحق؛ لا تُنشئ مفتاحاً عشوائياً في كل تشغيل CI.

## البناء المحلي
ثبّت JDK17 وAndroid SDK35 وGradle8.9، وحدد ANDROID_HOME. للاختبارات فقط:

```sh
python3 tools/check_project.py
gradle --no-daemon :app:testReleaseUnitTest
```

لـ APK موقّع، صدّر الأسرار الأربعة في بيئتك ثم:

```sh
gradle --no-daemon :app:testReleaseUnitTest :app:assembleRelease
```

يمكن استخدام `KEYSTORE_PATH` لمسار JKS بدلاً من `KEYSTORE_BASE64` محلياً. المخرج: `app/build/outputs/apk/release/app-release.apk`. لا تُرفع مفاتيح التوقيع مع artifacts.

اختبار EngineRegressionTest يستدعي EngineTest ويشمل الحساب الأساسي والأخطاء والدوال الجديدة. وجود الاختبارات لا يعني اجتيازها؛ CI يشغّلها قبل قبول APK. يجب أيضاً تجربة التثبيت وفتح الواجهة وتغيير الثيم والسجل على هاتف فعلي قبل اعتبار النسخة جاهزة.
