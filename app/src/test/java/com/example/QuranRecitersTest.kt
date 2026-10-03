package com.example

import com.example.media.QuranReciters
import org.junit.Assert.*
import org.junit.Test
import java.net.URI

class QuranRecitersTest {
    @Test fun catalogContainsFiftyDistinctCompleteRecitationSources() {
        assertEquals(50, QuranReciters.size)
        assertEquals(50, QuranReciters.map { it.id }.toSet().size)
        assertEquals(50, QuranReciters.map { it.baseUrl }.toSet().size)
        QuranReciters.forEach {
            assertTrue(it.faName.isNotBlank() && it.arName.isNotBlank())
            val uri = URI(it.baseUrl)
            assertEquals("https", uri.scheme)
            assertTrue(uri.host.endsWith(".mp3quran.net"))
            assertEquals("${it.baseUrl}/001.mp3", it.surahUrl(1))
            assertEquals("${it.baseUrl}/114.mp3", it.surahUrl(114))
            assertTrue(runCatching { it.surahUrl(0) }.isFailure)
            assertTrue(runCatching { it.surahUrl(115) }.isFailure)
        }
    }

    @Test fun existingDownloadIdsRemainCompatible() {
        assertEquals(listOf("afs", "basit", "sds", "shur", "husr", "minsh", "maher", "s_gmd", "ajm", "yasser"),
            QuranReciters.take(10).map { it.id })
    }
}
