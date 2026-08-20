# مشخصات نسخه کافه‌بازار

- نام: اذکار نور
- شناسه بسته: `ir.adhkar.app`
- نسخه هدف انتشار: `1.3.0`
- کد نسخه هدف: `13`
- حداقل Android: API 24
- Android هدف: API 36
- نوع انتشار: رایگان، بدون تبلیغات و بدون پرداخت درون‌برنامه‌ای

## امضای انتشار

- الگوریتم کلید: RSA 4096-bit
- SHA-256 گواهی:
  `A3:06:1D:44:B5:F2:A0:CD:D2:4E:D5:0C:44:DB:A6:E4:DC:7A:EC:FE:F7:8F:C2:71:12:0F:21:2E:59:35:8E:95`
- اعتبار گواهی: ۲۴ ژوئیه ۲۰۲۶ تا ۹ دسامبر ۲۰۵۳

فایل `nour-adhkar-release.jks` و اطلاعات `release-signing.properties` محرمانه هستند، در Git ثبت نمی‌شوند و باید جداگانه پشتیبان‌گیری شوند.

## نتایج بررسی

- تست‌های واحد debug: موفق
- کامپایل، اعتبارسنجی امضای Gradle و بسته‌بندی release: موفق
- بررسی Lint Vital: به‌دلیل درخواست وابستگی‌های ناموجود Compose Desktop در AGP اجرا نشد
- بررسی امضای AAB: موفق
- شناسه بسته، نام نسخه و کد نسخه در Manifest خروجی: تأیید شد

## خروجی‌های نسخه ۱.۳.۰ با کد ۱۳

- `nour-adhkar-1.3.0-vc13.aab` — SHA-256: `D58B755E0D3D4B6CA4ABCFB3F5502D6870B4F738449E9F05A68BA80FF2206477`
- `nour-adhkar-1.3.0-vc13.bin` — SHA-256: `FB30DB47AE2D77B028B965CC85331F97B5394B1A5B12D32B99BCD4F78E03F76F`
- تغییرات فارسی: `docs/cafebazaar/CHANGELOG_1.3.0_FA.md`
- تغییرات انگلیسی: `docs/cafebazaar/CHANGELOG_1.3.0_EN.md`
- مجوزهای نهایی: اعلان، لرزش، اینترنت، سرویس پخش رسانه در پس‌زمینه، جلوگیری از خواب پردازنده و مجوز داخلی AndroidX برای receiver غیر-exported

## خروجی‌های پیشین نسخه ۱.۲.۱ با کد ۱۱

- `nour-adhkar-1.2.1-vc11.aab` — SHA-256: `A5D5EAA49EB40304EEFDB9CF17092427D6A8DAA2CCA632D9A72FD55E6866728B`
- `nour-adhkar-1.2.1-vc11.bin` — SHA-256: `4DD93855536F10E3192BA5EB1C92608F8F33EDEC174D9B074E6E2F7FB836B0A2`
- `nour-adhkar-1.2.1-vc11-debug.apk` — SHA-256: `8DABC40D1CCB40B3F599997F7464AECA9BD37A26E120C83B4A473C9DC7A9B21B`
- مجوزهای نهایی: اعلان، لرزش، اینترنت، سرویس پخش رسانه در پس‌زمینه، جلوگیری از خواب پردازنده و مجوز داخلی AndroidX برای receiver غیر-exported
- بررسی دستی صفحات اصلی روی دستگاه: موفق

Lint release به‌دلیل در دسترس نبودن artifactهای desktop مربوط به Compose در محیط ساخت کامل نشد. این مانع تولید، امضا، نصب یا اجرای خروجی Android نبود.

## خروجی‌های پیشین نسخه ۱.۲.۰ با کد ۱۰

- `nour-adhkar-1.2.0-vc10.aab` — SHA-256: `D167BF7FDD85C769083CF15F98412CEEFBE5E321C11D736B92E1ED65719BE756`
- `nour-adhkar-1.2.0-vc10.bin` — SHA-256: `14B8F8042935F67AEC52E819B3C417960E62EEC3EF17D9922260EF5437DE36BB`

## خروجی‌های پیشین نسخه ۱.۱.۱

- AAB: `release/cafebazaar-1.1.1-vc7/nour-adhkar-1.1.1-vc7.aab`
  - SHA-256: `8247DA99A035586C754CB37E9D416E118E18785476722014B28EA2837F995C77`
- BIN: `release/cafebazaar-1.1.1-vc7/nour-adhkar-1.1.1-vc7.bin`
  - SHA-256: `40FAC7679D84D9FAE0D7EF4F94F248F7CAF13F07430879C32A6C7BD077D2E013`

فایل BIN با Bundle Signer رسمی کافه‌بازار نسخه `0.1.13`، امضای v2 فعال و v3 غیرفعال تولید شده است.
