package com.example

import com.example.media.QuranAyahQueue
import com.example.media.QuranAyahReciter
import com.example.media.QuranAyahReciters
import com.example.media.QuranAyahTrack
import com.example.media.QuranReciters
import com.example.media.quranAyahReciter
import org.junit.Assert.*
import org.junit.Test
import java.net.URI

class QuranAyahPlayerTest {
    // Hafs verse counts per surah (6236 in total).
    private val verseCounts = listOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53,
        89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12,
        12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26,
        30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6
    )
    private val queue = QuranAyahQueue(verseCounts)

    @Test fun countsCoverTheWholeQuran() = assertEquals(6236, verseCounts.sum())

    @Test fun bismillahPrecedesSurahStartsExceptAlFatihaAndAtTawbah() {
        assertEquals(QuranAyahTrack(2, 1, bismillah = true), queue.first(2, 1))
        assertEquals(QuranAyahTrack(1, 1), queue.first(1, 1))
        assertEquals(QuranAyahTrack(9, 1), queue.first(9, 1))
        assertEquals(QuranAyahTrack(2, 5), queue.first(2, 5))
        assertEquals(QuranAyahTrack(2, 1), queue.next(QuranAyahTrack(2, 1, bismillah = true)))
    }

    @Test fun playbackContinuesAcrossSurahsAndEndsAfterAnNas() {
        assertEquals(QuranAyahTrack(1, 7), queue.next(QuranAyahTrack(1, 6)))
        assertEquals(QuranAyahTrack(2, 1, bismillah = true), queue.next(QuranAyahTrack(1, 7)))
        assertEquals(QuranAyahTrack(9, 1), queue.next(QuranAyahTrack(8, 75)))
        assertNull(queue.next(QuranAyahTrack(114, 6)))
    }

    @Test fun previousStepsBackOneVerseAcrossSurahs() {
        assertEquals(QuranAyahTrack(2, 4), queue.previous(QuranAyahTrack(2, 5)))
        assertEquals(QuranAyahTrack(1, 7), queue.previous(QuranAyahTrack(2, 1)))
        assertEquals(QuranAyahTrack(1, 7), queue.previous(QuranAyahTrack(2, 1, bismillah = true)))
        assertNull(queue.previous(QuranAyahTrack(1, 1)))
    }

    @Test fun rejectsVersesOutsideTheSurah() {
        assertTrue(runCatching { queue.first(1, 8) }.isFailure)
        assertTrue(runCatching { queue.first(115, 1) }.isFailure)
    }

    @Test fun catalogUsesEveryAyahFileNamesWithMirrorFallback() {
        assertEquals(QuranAyahReciters.size, QuranAyahReciters.map { it.id }.toSet().size)
        assertEquals(QuranAyahReciters.size, QuranAyahReciters.map { it.folder }.toSet().size)
        QuranAyahReciters.forEach { reciter ->
            assertTrue(reciter.faName.isNotBlank() && reciter.arName.isNotBlank())
            val urls = reciter.ayahUrls(2, 255)
            assertEquals("${QuranAyahReciter.EVERY_AYAH}/${reciter.folder}/002255.mp3", urls.first())
            assertEquals(if (reciter.mirrored) 2 else 1, urls.size)
            urls.forEach { assertEquals("https", URI(it).scheme) }
        }
    }

    @Test fun defaultVoiceFollowsTheSavedChoices() {
        assertEquals("maher", quranAyahReciter("maher", "afs").id)
        assertEquals("husr", quranAyahReciter(null, "husr").id)
        assertEquals("afs", quranAyahReciter("missing", "not_in_ayah_catalog").id)
        // The reader's previous default surah voice keeps an ayah recording.
        assertEquals(QuranReciters.first().id, quranAyahReciter(null, QuranReciters.first().id).id)
    }
}
