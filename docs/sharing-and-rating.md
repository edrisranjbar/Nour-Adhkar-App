# Sharing and rating

Everything the app shares outward carries the Cafe Bazaar link so recipients can install the app.
Links and the feedback address live in `com.example.share.AppLinks`.

## Text shares

`appShareFooter()` appends «اذکار نور», a short install line and `https://cafebazaar.ir/app/ir.adhkar.app`.
It is used by dhikr text shares (`DhikrItem.shareText`), article shares, and every image-share caption.
The About screen keeps its own app-share text, which already includes the link.

## Image cards

`ShareCardRenderer` draws a fixed 1080×1350 (4:5) card on an Android `Canvas`: deep green gradient,
gold frame, Vazirmatn for UI text and Amiri Quran for Arabic adhkar, and a footer with the logo,
app name, install line and store URL. Long adhkar shrink to fit; if they still do not fit, the
translation is dropped before the Arabic text is ever truncated.

`shareAppCardImage()` renders off the main thread into `cacheDir/shared/`, exposes the file through
the `${applicationId}.fileprovider` `FileProvider` (`res/xml/share_paths.xml`), and opens the
Android chooser with the image plus a text caption. Apps that drop the caption still show the link
printed on the card.

- Dhikr cards: the share icon opens a menu with «اشتراک به‌صورت تصویر» and «اشتراک به‌صورت متن».
- Achievements: the detail and celebration screens share an image card with the badge artwork.
- Streak: the streak celebration dialog has «اشتراک‌گذاری مداومت», which shares the streak count.

Opening the chooser never sends anything by itself; the user picks the recipient.

## Cafe Bazaar rating prompt

`RatingPromptStore` (preferences file `rating_prompt`) gates `RatingPromptDialog`:

- shown only after the streak celebration that follows completing an adhkar collection,
  and only when the current streak is at least 3 days;
- at most 3 times in total, at least 7 days apart;
- never again after the user chooses to rate or to send feedback.

«بله، امتیاز می‌دهم» opens Bazaar's rating page (`ACTION_EDIT` on `bazaar://details?id=ir.adhkar.app`,
package `com.farsitel.bazaar`), falling back to the web listing when Bazaar is not installed.
«نه چندان؛ پیشنهاد می‌دهم» opens an email to the support address so unhappy users reach the
developer instead of leaving a low rating. «بعداً» or dismissing snoozes the prompt for a week.
