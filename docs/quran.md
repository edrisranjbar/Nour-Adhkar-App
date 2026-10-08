# قرآن کریم

تب «قرآن» متن عربی قرآن را کاملاً آفلاین و بر اساس صفحات مصحف مدینه، از صفحهٔ ۱ تا ۶۰۴، نمایش می‌دهد. کاربر با کشیدن صفحه به راست یا چپ جابه‌جا می‌شود و آخرین صفحهٔ خوانده‌شده ذخیره می‌گردد.

نوار بالای خواننده جست‌وجوی متن یا نام سوره دارد. کاربر می‌تواند هم از منوی سه‌نقطه و هم با لمس نام سوره یا شماره صفحه در سربرگ مصحف، دو مسیر مستقل ناوبری را باز کند: فهرست جست‌وجوشوندهٔ ۱۱۴ سوره برای رفتن به آغاز یک سوره، و ورودی مستقیم صفحه برای رفتن به یکی از صفحه‌های ۱ تا ۶۰۴. انتخاب سوره، صفحهٔ آغاز آن را باز می‌کند و اگر سوره در میانهٔ صفحه شروع شود به عنوان همان سوره می‌رود. منوی سه‌نقطه همچنین انتخاب رنگ کاغذی، سپیا، سبز ملایم یا شب را ارائه می‌کند. لمس هر آیه، پنلی باز می‌کند که سربرگ آن شمارهٔ آیه (در یک دایره) و نام سوره را نشان می‌دهد؛ زیر آن کلید «ترجمه / تفسیر»، سپس ترجمهٔ همان آیه (به‌جای تکرار متن عربی) و در پایین هایلایت چهاررنگ (دایره‌های کوچک) و دکمهٔ افزودن یا ویرایش یادداشت قرار دارد. ترجمه بر اساس زبان برنامه انتخاب می‌شود (بخش «ترجمه» را ببینید). همهٔ هایلایت‌ها و یادداشت‌ها تنها روی دستگاه ذخیره می‌شوند.

انتخاب یک آیه از نتایج جست‌وجو، صفحهٔ آن را باز می‌کند و تمام متن و شمارهٔ همان آیه را با رنگ تأکیدیِ متناسب با رنگ صفحه مشخص می‌کند. این نشانه تا خروج از صفحه باقی می‌ماند؛ انتخاب سوره یا رفتن مستقیم به صفحه نیز آن را پاک می‌کند. نتیجهٔ تازه جایگزین نتیجهٔ قبلی می‌شود، حتی اگر هر دو در یک صفحه باشند. این نشانه موقت است و هایلایت‌ها و یادداشت‌های ذخیره‌شده را تغییر نمی‌دهد؛ هنگام تلاوت همان آیه نیز قابل‌مشاهده می‌ماند و اندازه و چیدمان متن را تغییر نمی‌دهد.

آغاز هر سوره با نام سوره روی تصویر PNG تزئینی متقارن `quran_surah_ornament.png` نمایش داده می‌شود؛ متن نام سوره به‌صورت زنده و وسط‌چین روی تصویر قرار می‌گیرد و بسم‌الله در خطی مستقل زیر آن است. متن آیات در صفحه‌های ۱ و ۲ برای هماهنگی با صفحه‌آرایی آغاز مصحف وسط‌چین است. رنگ هایلایت هر آیه، متن آیه و شمارهٔ همان آیه را با هم در بر می‌گیرد.
متن آیات با اندازهٔ جمع‌وجور و فاصلهٔ خطی باز نمایش داده می‌شود تا حرکات و اعراب روی صفحهٔ تلفن خواناتر باشند. هر صفحهٔ مصحف یک صفحهٔ ثابت است و اسکرول نمی‌شود: اگر متن صفحه (همراه با سربرگ سوره و بسم‌الله) در ارتفاع موجود جا نشود، اندازهٔ همهٔ آن‌ها با هم به‌اندازهٔ لازم کوچک می‌شود و هرگز از اندازهٔ پیش‌فرض بزرگ‌تر نمی‌شود.

متن صفحه با قلم «KFGQPC HAFS Uthmanic Script» نسخهٔ ۱۸ مجمع ملک فهد نمایش داده می‌شود. این قلم رمزگذاری علائم مخصوص مجمع را انتظار دارد (مثلاً سکون U+06E1 و صفر مستدیر U+0652) و متن تنزیل در آن به‌جای برخی علائم دایره‌های جایگزین نشان می‌دهد؛ بنابراین متن نمایشی آیات از فایل بدون تغییر `quran/kfgqpc-hafs.txt` (متن حفص مجمع، نسخهٔ ۱۸) خوانده می‌شود و متن تنزیل برای جست‌وجو، شناسهٔ آیات و نگاشت صفحه‌ها باقی می‌ماند. شمارهٔ آیه با ارقام عربی-هندی نوشته می‌شود تا قلم خودش آن را درون نشان پایان آیه قرار دهد.

## برنامه ختم قرآن

گزینهٔ «برنامه ختم قرآن» در منوی سه‌نقطهٔ خواننده قرار دارد و مقصد جدیدی به منوی اصلی اضافه نمی‌کند. کاربر مدت ختم را از یک انتخاب‌گر ۷، ۳۰، ۶۰، ۹۰، ۱۸۰ یا ۳۶۵ روزه انتخاب می‌کند، شمارهٔ صفحهٔ شروع را در یک فیلد عددی وارد می‌کند و در صورت تمایل زمان یادآوری روزانه را تعیین می‌کند.

پس از فعال‌شدن برنامه، خلاصهٔ پیشرفت (روز فعلی از کل روزها و درصد) زیر عنوان نوار بالای خواننده و یک خط باریک پیشرفت در لبهٔ پایین همان نوار نمایش داده می‌شود تا ارتفاع صفحهٔ قرآن کم نشود؛ لمس عنوان، پنل جزئیات را باز می‌کند. پنل جزئیات، قرائت روزانه، درصد پیشرفت، تاریخ پایان و گزارش قرائت روزانه را نشان می‌دهد؛ تاریخ‌ها در زبان فارسی شمسی هستند. پیشرفت تنها با دکمهٔ «ثبت تلاوت تا این صفحه» تغییر می‌کند؛ ورق‌زدن به‌تنهایی هیچ صفحه‌ای را تکمیل‌شده ثبت نمی‌کند. اگر کاربر عقب یا جلو بیفتد، سهم روزهای باقی‌مانده از روی صفحات باقی‌مانده دوباره محاسبه می‌شود.

یادآوری ختم از تنظیم کلی اعلان‌ها پیروی می‌کند، با تأخیر یک‌ساعته سازگار است و با لمس اعلان، صفحهٔ بعدیِ خوانده‌نشده را در خوانندهٔ داخلی باز می‌کند. توقف موقت، تکمیل یا لغو برنامه، زمان‌بندی اعلان آن را متوقف می‌کند. برنامه و گزارش روزانه به‌صورت آفلاین در تنظیمات محلی دستگاه ذخیره می‌شوند.

در توقف موقت، شمارش روزهای برنامه ثابت می‌ماند و پس از ادامه، تاریخ پایان به اندازهٔ روزهای توقف جابه‌جا می‌شود. ثبت پیشرفت فقط از صفحهٔ شروع برنامه به بعد امکان‌پذیر است. پنل ساخت و جزئیات در نمایشگرهای کوتاه قابل پیمایش‌اند و مقدار دلخواه مدت باید بین ۱ تا ۳۶۵۰ روز باشد.

## ترجمه

ترجمه‌ها کاملاً آفلاین‌اند و از منابع اهل سنت انتخاب شده‌اند:

- فارسی: «ترجمهٔ فارسی معانی قرآن کریم» گروه ترجمهٔ اسلام‌هاوس، منتشرشده در QuranEnc (مرکز ترجمهٔ رواد با همکاری IslamHouse.com)، نسخهٔ v1.1.0 — `assets/quran/translation-fa-islamhouse.txt`.
- فارسی (انتخابی): «تفسیر نور» دکتر مصطفی خرمدل (از Tanzil، `fa.khorramdel`؛ همان منبع ترجمهٔ آیات احساسات) — `assets/quran/translation-fa-khorramdel.txt`.

در زبان فارسی، گزینهٔ «ترجمهٔ آیات» در منوی سه‌نقطهٔ خواننده بین این دو ترجمه جابه‌جا می‌کند؛ پیش‌فرض اسلام‌هاوس است و انتخاب در کلید `quran_translation_fa` ذخیره می‌شود. زبان عربی فقط التفسیر المیسر را دارد، پس این گزینه در آن نمایش داده نمی‌شود.
- عربی: «التفسیر المیسر» مجمع ملک فهد برای چاپ مصحف شریف (از Tanzil، `ar.muyassar`) — `assets/quran/translation-ar-muyassar.txt`.
- اردو: ترجمهٔ مولانا محمد ابراهیم جوناگڑهی، منتشرشده در QuranEnc (`urdu_junagarhi`، نسخهٔ v1.1.3) — `assets/quran/translation-ur-junagarhi.txt`. این متن مستقیماً از API رسمی QuranEnc گرفته شده است. نشانه‌های ارجاع پانویس مانند `[1]` حذف شده‌اند (خود پانویس‌ها همراه برنامه نیستند)، لام‌الف‌های ترکیبی U+FEFB/U+FEFC به «لا» تبدیل شده‌اند و نویسه‌های کنترلی پاک شده‌اند؛ بقیهٔ متن تغییر نکرده است. در زبان دری همان ترجمه‌های فارسی نمایش داده می‌شوند. زبان اردو تفسیر اردو ندارد و تفاسیر عربی را نشان می‌دهد.

قالب هر فایل `surah|ayah|text` در ۶۲۳۶ سطر به ترتیب مصحف است و سطرهای آغازشده با `#` اطلاعات منبع را نگه می‌دارند. متن‌ها تغییر نکرده‌اند (فقط چند نویسهٔ کنترلی نامرئی از متن فارسی حذف شد). فقط ترجمهٔ زبان فعلی و هنگام نخستین بازکردن خواننده بارگذاری می‌شود. منبع استخراج: آینهٔ [fawazahmed0/quran-api](https://github.com/fawazahmed0/quran-api) (نسخه‌های `fas-islamhousecompe`، `fas-mostafakhorramd` و `ara-kingfahadquranc`)؛ برای به‌روزرسانی، متن را با نسخهٔ اصلی QuranEnc/Tanzil مقایسه کنید. آزمون: `QuranTranslationsTest`.

## تفسیر

پنل آیه دو زبانه دارد: «ترجمه» و «تفسیر» (در زبان عربی «المعنى» و «التفسير»). آخرین زبانهٔ انتخاب‌شده تا پایان خواندن حفظ می‌شود. در زبانهٔ تفسیر، ردیف تراشه‌ها منبع را انتخاب می‌کند و انتخاب برای هر زبان جداگانه ذخیره می‌شود (`quran_tafsir_fa` / `quran_tafsir_ar`). متن قابل انتخاب و کپی است و ناحیهٔ متن تا ۴۵٪ ارتفاع صفحه بزرگ می‌شود و درونش پیمایش می‌شود؛ تا زمانی که متن ادامه دارد، لبهٔ پایین کارت کم‌کم محو می‌شود تا خط آخر ناگهان بریده دیده نشود.

تفاسیر اهل سنت، کاملاً آفلاین:

| شناسه | تفسیر | زبان |
|---|---|---|
| `fa_mokhtasar` | المختصر فی تفسیر القرآن الکریم (ترجمهٔ فارسی)، مرکز تفسیر للدراسات القرآنیة | فارسی |
| `fa_saadi` | تفسیر سعدی (تیسیر الکریم الرحمن)، ترجمهٔ فارسی | فارسی |
| `ar_saadi` | تيسير الكريم الرحمن، السعدي | عربی |
| `ar_mokhtasar` | المختصر في تفسير القرآن الكريم | عربی |
| `ar_ibn_kathir` | تفسير القرآن العظيم، ابن كثير | عربی |
| `ar_tabari` | جامع البيان عن تأويل آي القرآن، الطبري | عربی |
| `ar_qurtubi` | الجامع لأحكام القرآن، القرطبي | عربی |
| `ar_baghawi` | معالم التنزيل، البغوي | عربی |
| `ar_jalalayn` | تفسير الجلالين | عربی |

در زبان فارسی ابتدا تفاسیر فارسی و سپس تفاسیر عربی نمایش داده می‌شوند (پیش‌فرض: المختصر فارسی)؛ در زبان عربی فقط تفاسیر عربی (پیش‌فرض: السعدي). التفسیر المیسر در عربی همان متن زبانهٔ «المعنى» است و تکرار نشده است.

- هر تفسیر در `assets/quran/tafsir/<id>/<surah>.txt` ذخیره شده است؛ هر سطر `ayah|text` (شکست پاراگراف به‌صورت `\n`) یا `ayah|=owner` است، یعنی این آیه همان متن تفسیر آیهٔ `owner` در همان سوره را دارد. در این حالت پنل بازهٔ آیه‌ها را بالای متن نشان می‌دهد («تفسیر آیه‌های ۶ تا ۷»).
- اگر تفسیری برای آیه‌ای مدخل ندارد (مثلاً ۵۹ آیه در السعدي، ۲۲۶ آیه در الجلالين و یک آیه در القرطبي)، پنل صادقانه می‌گوید متن جداگانه‌ای نیست و تفسیر آیهٔ پیشین را با ذکر شمارهٔ آن نشان می‌دهد؛ اگر آیهٔ پیشینی نباشد (چهار آیهٔ آغاز سوره در الجلالين) پیام «متنی ندارد» نمایش داده می‌شود.
- فقط یک فایل سوره هنگام لمس آیه خوانده می‌شود و چهار سورهٔ اخیر در حافظه نگه داشته می‌شوند.
- منبع: مخزن [spa5k/tafsir_api](https://github.com/spa5k/tafsir_api) که داده‌ها را از Quran.com و QUL (Tarteel) گرفته است (اسلاگ‌ها: `persian-mokhtasar`، `fr-tafsir-as-saadi`، `ar-tafseer-al-saddi`، `ar-tafsir-al-mukhtasar`، `ar-tafsir-ibn-kathir`، `ar-tafsir-al-tabari`، `ar-tafseer-al-qurtubi`، `ar-tafsir-al-baghawi`، `ar-tafsir-al-jalalayn`). متن مؤلف تغییر نکرده است؛ فقط پانویس‌های محقق نسخه (`[[…]]`، اختلاف نسخه‌ها) و نشانه‌های برگ نسخهٔ خطی (مانند `٤\ب`) از ابن‌کثیر، طبری، قرطبی و بغوی حذف شده‌اند (پانویس‌های تودرتو کامل حذف می‌شوند و فاصلهٔ اضافهٔ پیش از ویرگول باقی‌مانده از حذف پانویس پاک می‌شود)، بک‌اسلش‌های جداکنندهٔ ارجاع‌ها در طبری به «،» تبدیل شده‌اند، بک‌اسلش‌های اضافی دیگر پاک شده‌اند و شکست خط‌های وسط جمله (از صفحه‌بندی چاپی) به فاصله تبدیل شده‌اند.
- در قرطبی آیهٔ ۱۷:۳۰ فقط پانویس محقق را داشت (مؤلف دربارهٔ آن سخن نگفته است)، پس پنل برای آن تفسیر آیهٔ پیشین را با توضیح نشان می‌دهد.
- حجم تفاسیر در APK حدود ۲۹ مگابایت (فشرده) است؛ طبری و قرطبی بزرگ‌ترین‌ها هستند (حدود ۶ و ۷ مگابایت).
- آزمون: `QuranTafsirsTest`.

## داده و انتساب

متن عربی Uthmani بدون تغییر از **Tanzil Project، نسخهٔ ۱.۱** در `app/src/main/assets/quran/tanzil-uthmani.xml` قرار دارد و اطلاعیهٔ کپی‌رایت اصلی در همان فایل حفظ شده است. اعتبار منبع (Tanzil Project، tanzil.net) در متن صفحهٔ «درباره برنامه» آمده است.

نقشهٔ صفحه‌های مصحف مدینه در `app/src/main/assets/quran/page-index.json` است و از پروژهٔ [quran-json](https://github.com/wpdynamo/quran-json) که شاخص‌های صفحه را از Quran.com تولید می‌کند، گرفته شده است.

## Audio recitation

«قرآن صوتی» (`quran_audio`) is a dedicated listening screen available from the drawer and the reader's reciter menu. Its design follows the rest of the app (Material theme roles, the app's green accent, Vazirmatn) and the Quran reader:

- **Now-playing card:** the surah name is set on the reader's `quran_surah_ornament` frame in the Uthmanic face, with «سورهٔ ۱۸ · ۱۱۰ آیه · صفحه ۲۹۳» beneath. A status pill shows ready, downloading, or playing. While playing it has small moving sound bars, which stay still when the system's "remove animations" setting is on. A «تلاوت ذخیره شده» chip appears when the surah is saved offline. Below a divider, the Qari row shows an initial-letter avatar (no unverified portraits), the name, «حفص از عاصم», and a «تغییر» pill. Tapping the surah half or the Qari row opens the matching picker.
- **Player:** the slider and the 72 dp play button use the app's green (#3A6931 light, #A3D899 dark). The ±10 s buttons are tonal. Previous/next surah sit at the ends. Transport controls keep left-to-right order in RTL. While a surah is being fetched, a ring around the play button shows the download percentage, and the button becomes a cancel button.
- **Status line:** one line under the controls. It shows an error card with «تلاش دوباره», the download percentage, or, for a surah not yet saved, a hint that it downloads once and then plays offline.
- **Pickers:** rows have a numbered surah badge or a Qari avatar, a highlighted selection with a check instead of radio buttons, saved-surah icons, and a rounded search field with a clear button. The surah list opens scrolled to the current surah. Each Qari row shows how many surahs are saved for that voice.

Times and numbers follow the app language's digits. Searchable bottom sheets select a Qari or any of the 114 Surahs, accepting Persian/Arabic digits and normalizing common Arabic/Persian spelling differences. The listening screen keeps its Qari in the `reciter` preference and also remembers its last Surah. Changing either selection stops the current session and waits for an explicit Play action rather than downloading automatically.

The listening player provides pause/resume, a seek slider with elapsed/duration labels, ten-second skips, and previous/next Surah selection. Quran recitations play at their original speed; there is no speed control or playback-rate adjustment in the shared Quran player. A download icon marks saved Surahs in the selector. Loading, cancellation, mobile-data confirmation, retryable errors, and an empty search state are presented in the existing Persian/Arabic Material theme. Leaving the listening screen stops playback/downloads, matching the reader lifecycle; background playback is not provided.

The shared catalog contains 50 curated complete Hafs recitations from the [MP3Quran public catalog](https://www.mp3quran.net/api/v3/reciters?language=ar), checked on 2026-10-03. This is a curated selection, not a popularity ranking supplied by the source. All 50 sources' Surah 001 and 114 URLs returned HTTP 200 in endpoint probes; this does not verify every individual recording. The original ten IDs and URLs remain unchanged so existing preferences and saved audio continue to work. Catalog compatibility is covered by `QuranRecitersTest`.

Full-surah playback downloads the surah from mp3quran.net into `filesDir/quran_audio/<reciter>/<NNN>.mp3`, shows progress, validates content length and Android-readable audio, then atomically publishes it and plays locally. Later plays use the saved file without a network request. A missing download needs internet once; failures remain retryable. Partial files are ignored and cleaned up; downloads are restartable, not range-resumed. No broad storage permission is needed. On a connection without Wi-Fi, the app asks before downloading and shows the size when supplied by the server. Wi-Fi needs no confirmation.

The listening screen uses the shared `QuranDownloadDialog`. It identifies the pending Surah and Qari in a themed summary card, shows the rounded download size in localized units (or explicitly says the server did not report a size), explains mobile-data usage and persistent offline storage, and offers «دانلود و پخش» / «فعلاً نه». Confirm resumes that pending request; dismiss/Back cancels it. Content scrolls on small screens without fixed text heights. Permission and download scheduling behavior remain unchanged.

The listening screen's selector marks saved Surahs. Downloads are managed automatically; there is no downloaded-audio management entry or dialog. Audio is excluded from Android cloud backup/device transfer. Downloads persist per Qari/Surah across app restarts. The store rechecks for a completed file under its mutation lock before requesting the network; `QuranAudioStoreTest` covers reuse across store instances.

## Verse-by-verse recitation (reader)

The reader recites ayah by ayah and follows along. The play button in the reader's top bar starts from the first verse of the visible page; «پخش از این آیه» in a verse's sheet starts from that verse. The reciting verse is tinted with the page accent color (over any saved highlight), and the reader turns to the next page when the recitation reaches it. A swipe away from the reciting page holds until the next verse begins. Playback continues across surahs until it is stopped or reaches the end of an-Nas. Before verse 1 of each surah except al-Fatiha and at-Tawbah, the Basmala (EveryAyah's 1:1 file) plays without a verse highlight.

While a recitation is active, a floating player above the bottom navigation shows the surah, the verse (or «بسم‌الله»), and the Qari. It has previous/next verse, pause/resume, and close buttons. Transport controls keep their left-to-right media order in RTL. The page refits above the measured player height instead of hiding its last lines. The screen stays on while a recitation is active. Leaving the reader stops playback; there is no background playback.

The reader's Qari menu still links to «قرآن صوتی», then lists the 31 verse-by-verse voices in `QuranAyahReciters`. The choice is saved as `ayah_reciter` in the `quran_audio` preferences. Before a choice is saved, the reader uses the listening screen's Qari if it has an ayah recording, else Alafasy. IDs match the surah catalog for the same Qari. Changing the voice while reciting restarts the current verse in the new voice.

Sources (researched 2026-10-06):

- **[EveryAyah](https://everyayah.com/data/recitations.js)** (primary): one MP3 per verse at `https://everyayah.com/data/<folder>/<SSSAAA>.mp3`. It has about 80 folders, including translations, Warsh, and several bitrates. The curated list keeps complete Hafs Arabic recitations at the best available bitrate, including the Mujawwad and Muallim (teaching) styles and the Iranian Qaris Shahriar Parhizgar and Karim Mansoori. Each kept folder's 1:1, 114:6, 2:282 and several mid-Quran verses returned HTTP 200. Mustafa Ismail was excluded because many verses are missing. These probes do not verify every one of the 6,236 files per Qari.
- **[QuranicAudio's EveryAyah mirror](https://mirrors.quranicaudio.com/everyayah/)** (fallback): the same folder names. Quran.com's verse API links to it. It serves 19 of the kept folders (`mirrored = true`) and is tried when EveryAyah fails.
- Other sources were reviewed but not used. The Quran.com/Quran Foundation API (`verses.quran.com`) has 12 verse recitations that overlap EveryAyah. The Islamic Network CDN (`cdn.islamic.network/quran/audio/<bitrate>/<edition>/<global ayah>.mp3`, from alquran.cloud) has about 20 Arabic voices. MP3Quran's `ayat_timing` API offers verse timings for about 115 of its surah files. Those timings target `cdn.mp3quran.net` recordings rather than the `serverN` files this app downloads, so applying them to saved surahs was left as a possible follow-up.

Each verse is downloaded once into `cacheDir/quran_ayah/<reciter>/<SSSAAA>.mp3`. A download is written to a `.part` file, length-checked, and atomically renamed. Then it plays locally while the next verse is prefetched. Replaying recently heard verses needs no network. Android may reclaim the cache. The app also trims the least recently played verses once the cache exceeds 200 MB. A cached file Android cannot play is deleted so that it downloads again. Verse files are small (roughly 40–800 KB), so verse playback streams on mobile data without the full-surah confirmation. Failures stop playback with a friendly Persian/Arabic message. The play order and catalog are covered by `QuranAyahPlayerTest`.

## Search

The search icon opens a spotlight-style overlay: a dimmed backdrop with a floating, auto-focused field. Results are grouped into matching surahs (by name or number; tapping jumps to the surah) and matching verses (at least two characters; up to 40, tapping opens the verse page). Tapping the backdrop or ✕ on an empty field closes it.

## Share a verse

Tapping a verse opens its sheet, which now has «اشتراک‌گذاری» next to the note button.

The note and share buttons use their natural content widths with single-line labels. On narrow screens or at larger font sizes, the action row wraps entire buttons onto separate rows instead of splitting the note label across lines.

The share sheet offers:

- **Format:** «متن» (text) on the right and «تصویر» (image) on the left in the RTL sheet. Image remains the initial selection; each option's icon, selection and preview match its label.
- **«همراه با ترجمه»:** adds the verse's translation in the reader's current translation. It's disabled when no translation is available.
- **Live preview:** the exact image card, or the exact text that will be sent.

**Image:** the existing 1080×1350 share card, with the reference («سوره …، آیه …») at the top, the verse in the Quranic face inside ﴿ ﴾, the translation beneath it, and the app name and store line at the bottom. Quran text is never cut off. If the translation doesn't fit, the image carries the verse only and the translation travels in the message text. If even the verse alone doesn't fit, the image option is disabled and the verse is shared as text. Image shares also include the full text as the caption.

**Text:** the verse in ﴿ ﴾ using the Unicode (Tanzil) text, so it reads correctly in any app, then the reference, the optional translation with its credit, and the app footer.

Sharing goes through the Android share chooser (WhatsApp, Telegram, Instagram, etc.).

The shared image-card footer places the Nour Adhkar logo and app name on the physical right, and the selected release store's link (Bazaar or Myket) on the physical left. It omits the download invitation to keep the image uncluttered. The link fits the remaining width without overlapping the branding. This footer is shared with the app's other image cards. See [store releases](store-releases.md) for routing and packaging.
