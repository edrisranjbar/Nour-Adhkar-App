# بازخورد و پیام‌های مدیر

برای پیام‌های خوانده‌نشده، دکمهٔ «علامت‌گذاری به‌عنوان خوانده‌شده» با کادر دور و آیکن دو تیک نمایش داده می‌شود. دکمه پس از ثبت موفق وضعیت خواندن پنهان می‌شود.

در صفحه «درباره برنامه»، کاربر نوع بازخورد را از پیشنهاد، انتقاد و سایر انتخاب می‌کند و متن پیام را با اتصال اینترنت به API وب می‌فرستد. پیام فقط پس از پاسخ موفق سرور از فرم پاک می‌شود.

«پیام‌های مدیر» در فهرست کناری قرار دارد. این صفحه هنگام باز شدن اطلاعیه‌های منتشرشده را از `https://api.adhkar.ir/api` دریافت می‌کند. لمس هر پیام آن را برای شناسه تصادفی ذخیره‌شده روی همین نصب خوانده‌شده علامت می‌زند. با پاک‌کردن داده‌های برنامه یا نصب مجدد، این شناسه و وضعیت خواندن از دست می‌رود. این بخش صندوق پیام داخل برنامه است و اعلان فشاری دستگاه ارسال نمی‌کند. در صورت نبود اینترنت، خطا و گزینه تلاش دوباره نشان داده می‌شود.

پیش از انتشار نسخه برنامه، مهاجرت‌های پایگاه داده و کد API پنل وب باید روی سرور نصب شوند.

## Entry point

تاریخ پیام‌ها از بخش تاریخ میلادی `created_at` به جلالی تبدیل می‌شود و با نام فارسی ماه و اعداد فارسی، مانند `۲۶ شهریور ۱۴۰۴`، بدون ساعت یا روز هفته نمایش داده می‌شود. روز ثبت‌شدهٔ سرور حفظ می‌شود و با منطقهٔ زمانی دستگاه جابه‌جا نمی‌شود. تاریخ خالی یا نامعتبر با «تاریخ نامشخص» نمایش داده می‌شود.

Messages are opened from the notifications icon at the left end of the home app bar (not the drawer). A badge shows the unread count (Persian digits, 99+ cap); it refreshes when Home is shown and updates as notices are loaded or marked read. The screen title is «پیام‌ها» in the app bar only.

## Feedback requires sign-in

«ارسال نظر و پیشنهاد» asks signed-out users to sign in first: a short explanation and «ورود به حساب», which opens the account screen. Signed-in users see «ارسال با نام …», and the app sends their sign-in token with `POST /api/app-feedback`. The admin panel's feedback list and the dashboard show the sender's name and email; older app versions without sign-in appear as «ناشناس». If the token has expired, the server answers 401 and the app asks the user to sign in again.

## Replies and likes

- **Admin panel:** under each in-app message from a signed-in sender, the feedback list has «پاسخ دادن» (a reply box you can later edit or clear) and a «♡ پسندیدن» toggle. Anonymous messages from older app versions say that a reply cannot reach the sender.
- **App:** the feedback sheet has two tabs, «ارسال پیام» and «پیشنهادهای من». «پیشنهادهای من» loads the user's messages from `GET /api/app-feedback/mine` (sign-in required), newest first. Each one shows its type, date, «پسندیده شد» when liked, and the reply as «پاسخ تیم اذکار نور».
- **RTL tabs:** «ارسال پیام» stays on the right and «پیشنهادهای من» on the left. Only the outer edges are rounded; the shared inner boundary is straight, using the same explicit physical-corner shapes as the verse-sharing selector. Tab selection, reply counts and message submission behavior are unchanged.
- **New replies:** replies the user hasn't seen are marked «پاسخ تازه», and the About screen's feedback button shows «N پاسخ تازه». The sheet opens on «پیشنهادهای من» while one is waiting. Seen replies are remembered on the device, and editing a reply makes it new again.
