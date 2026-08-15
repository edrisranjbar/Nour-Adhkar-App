# راهنمای انتشار در کافه‌بازار

## ۱. پشتیبان‌گیری از کلید

فایل `nour-adhkar-release.jks` و فایل محلی `release-signing.properties` را در دو محل امن و جداگانه پشتیبان بگیرید. این فایل‌ها در Git ثبت نمی‌شوند. گم‌شدن کلید می‌تواند انتشار به‌روزرسانی با همان شناسه بسته را غیرممکن کند.

برای مشاهده مشخصات گواهی:

```powershell
& "$env:JAVA_HOME\bin\keytool.exe" -list -v -keystore .\nour-adhkar-release.jks -alias nour-adhkar
```

## ۲. ساخت خروجی‌ها

Java همراه Android Studio را تنظیم و خروجی‌ها را بسازید:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat clean testDebugUnitTest lintRelease bundleRelease assembleRelease
```

اگر Maven مربوط به AAPT2 در دسترس نبود ولی Android SDK 36 نصب است:

```powershell
.\gradlew.bat bundleRelease assembleRelease `
  "-Pandroid.aapt2FromMavenOverride=$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\aapt2.exe"
```

خروجی‌های قابل بارگذاری:

- App Bundle: `app/build/outputs/bundle/release/app-release.aab`
- APK عمومی: `app/build/outputs/apk/release/app-release.apk`

## ۳. آماده‌سازی پیشخان

1. سیاست حریم خصوصی فارسی را روی یک URL عمومی HTTPS میزبانی کنید.
2. وارد پیشخان توسعه‌دهندگان بازار شوید و قراردادهای لازم حساب را تأیید کنید.
3. «برنامه جدید» را انتخاب و فایل AAB را بارگذاری کنید. اگر پیشخان برای حساب یا مسیر انتخابی فقط APK پذیرفت، APK امضاشده را بارگذاری کنید.
4. شناسه بسته استخراج‌شده باید `ir.adhkar.app` باشد.
5. اطلاعات `STORE_LISTING_FA.md`، آیکن و تصاویر پوشه تحویل را وارد کنید.
6. برنامه را رایگان و بدون پرداخت درون‌برنامه‌ای تنظیم کنید.
7. پرسش‌های مجوزها، داده‌ها و رده‌بندی سنی را مطابق اطلاعات بسته انتشار پاسخ دهید.
8. پیش‌نمایش صفحه برنامه را بازبینی کنید و سپس آن را برای داوری بفرستید.

## ۴. انتشار نسخه‌های بعدی

- همیشه از همین keystore استفاده کنید.
- `versionCode` را برای هر نسخه افزایش دهید.
- `versionName` و متن تغییرات را به‌روزرسانی کنید.
- پیش از بارگذاری، امضا و checksum خروجی را بررسی کنید.
