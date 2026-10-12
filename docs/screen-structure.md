# Home and Quran screen structure

`HomeScreen.kt` owns the home screen state collection, search switching, and section order. Its extracted components live alongside it in `ui/screens/`: `HomeStreak.kt`, `HomeCollections.kt`, `HomeCategories.kt`, `HomeActivityCards.kt`, and `HomeAyahCards.kt`. Other screens can still reuse the category grid and streak helpers.

`QuranScreen.kt` owns reader state, preferences, page navigation, and the callbacks that update them. Reader rendering, styling, labels, search helpers, verse actions, the ayah player bar, the surah picker, and dialogs are split into `QuranReaderParts.kt`, `QuranReaderStyle.kt`, `QuranLabels.kt`, `QuranSearch.kt`, `QuranVerseActions.kt`, `QuranAyahPlayerBar.kt`, `QuranSurahPicker.kt`, and `QuranDialogs.kt`.

The extracted Quran components use package-internal visibility. Dialogs receive values and callbacks; preference writes, page changes, and dialog dismissal remain coordinated by `QuranScreen`. This extraction does not change the Room schema, preference keys, storage paths, or section order.
