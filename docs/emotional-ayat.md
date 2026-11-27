# Mood-based Quran verses

The 2026-11-24 research selection was approved for implementation. It replaces the
previous selection with 60 distinct, complete verses: ten for each existing mood.
Forty remain in their existing mood, 19 are newly selected, and 39:10 moves from
sadness to feeling overwhelmed. No mood identifiers or reminder schedules change.

| Mood | Approved verses (research review order) |
|---|---|
| `anxious` — نگرانم | 13:28, 2:186, 3:173, 20:46, 27:62, 40:44, 64:11, 48:4, 39:36, 11:123 |
| `sad` — غمگینم | 12:86, 94:5, 21:83, 12:84, 93:3, 2:155, 2:156, 9:40, 28:13, 15:97 |
| `regretful` — پشیمانم | 39:53, 4:110, 42:25, 3:135, 25:70, 20:82, 7:23, 11:90, 39:54, 66:8 |
| `angry` — عصبانی‌ام | 3:134, 42:37, 17:53, 41:34, 41:36, 7:199, 25:63, 42:40, 42:43, 24:22 |
| `overwhelmed` — خسته و تحت فشارم | 20:25, 20:26, 78:9, 2:286, 2:153, 65:7, 18:10, 11:115, 39:10, 16:97 |
| `grateful` — آرام و شکرگزارم | 1:2, 2:152, 27:19, 14:7, 31:12, 16:18, 16:53, 39:66, 10:58, 93:11 |

## Text integrity and presentation

- Arabic is copied verbatim from the bundled Tanzil Uthmani 1.1 XML. Its notice is
  preserved alongside the copied text. See [Tanzil](https://tanzil.net) and its
  [text license](https://tanzil.net/docs/text_license).
- Persian is the **complete** bundled `fa.khorramdel` translation, credited as
  «تفسیر نور، دکتر مصطفی خرمدل». Research-report summaries are not used as translations.
- Every entry contains exactly one verse. The old `sad-94-5` ID is retained but
  now contains 94:5 only; 94:6 is mentioned as adjacent context. 20:25 and 20:26
  remain two verses of the same continuous prayer, not separate prescribed rituals.
- Editorial context/reflection is shown separately below the translator credit,
  headed «تأمل در آیه», in the existing Persian/Dari translation view. It is not
  part of the credited translation. Other language visibility remains unchanged.
- Text wraps naturally; no line limit or fixed height truncates a complete verse.
- Mood matching is editorial, not a claim of a prescribed religious treatment,
  a special repetition count, or a guaranteed medical or material outcome.

Context notes distinguish particular addressees and events (Moses and Aaron,
the Prophet, Hudaybiyyah, the cave during migration, and the mother of Moses).
They preserve hope alongside repentance, forgiveness alongside justice and
boundaries, and patience alongside rest, help, and the possibility of changing
harmful circumstances. Ordinary distress is not labeled weak faith.

## Sources reviewed

Every proposed verse was reviewed with the bundled Persian *Al-Mukhtasar* tafsir
and the existing Khorramdel translation. Online checks of Ibn Kathir addressed
particularly sensitive contexts:

- [13:28 — remembrance and reassurance](https://quran.com/13:28/tafsirs/en-tafisr-ibn-kathir)
- [12:84 — Jacob's grief](https://quran.com/12:84/tafsirs/en-tafisr-ibn-kathir)
- [93:3 — the Prophet and delayed revelation](https://quran.com/93:3/tafsirs/en-tafisr-ibn-kathir)
- [15:97 — distress caused by hostile speech](https://quran.com/15:97/tafsirs/en-tafisr-ibn-kathir)
- [48:4 — serenity at Hudaybiyyah](https://quran.com/48:4/tafsirs/en-tafisr-ibn-kathir)
- [39:53 — mercy and repentance](https://quran.com/39:53/tafsirs/en-tafisr-ibn-kathir)
- [42:40 — forgiveness and justice](https://quran.com/42:40/tafsirs/en-tafisr-ibn-kathir)
- [24:22 — charity and the slander incident](https://quran.com/24:22/tafsirs/en-tafisr-ibn-kathir)
- [2:286 — accountability, capacity and supplication](https://quran.com/2:286/tafsirs/en-tafisr-ibn-kathir)
- [65:7 — financial provision according to means](https://quran.com/65:7/tafsirs/en-tafisr-ibn-kathir)
- [78:9 — sleep and rest](https://quran.com/78:9/tafsirs/en-tafisr-ibn-kathir)
- [31:12 — gratitude and wisdom](https://quran.com/31:12/tafsirs/en-tafisr-ibn-kathir)
- [14:7 — gratitude in the context of deliverance](https://quran.com/14:7/tafsirs/en-tafisr-ibn-kathir)

## Persistence and verification

Retained verses keep their existing IDs. The existing picker retains today's
selection if its ID is still valid and filters removed IDs out of the seen set.
It chooses an unseen candidate on subsequent days; research order does not
change the random daily rotation. User mood history and preferences are preserved.

`EmotionalAyahDataTest` covers ten sourced entries per mood and exact Arabic and
Persian parity with the bundled source files, including complete rather than
excerpted verses. Implementation-time static checks also compare the selection
with the approved research manifest. Gradle tests, compilation, installation and
visual verification require a separate requested run.
