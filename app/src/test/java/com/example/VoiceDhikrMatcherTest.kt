package com.example

import com.example.voice.countCompleteDhikr
import com.example.voice.normalizeDhikr
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceDhikrMatcherTest {
    @Test fun `normalizes Arabic spelling and marks`() {
        assertEquals("استغفر الله", normalizeDhikr("أَسْتَغْفِرُ اللّٰهَ"))
    }

    @Test fun `counts complete repetitions only`() {
        assertEquals(2, countCompleteDhikr("سبحان الله، سبحان الله سبحان", "سُبْحَانَ الله"))
    }

    @Test fun `ignores unrelated words`() {
        assertEquals(0, countCompleteDhikr("الحمد لله", "الله أكبر"))
    }
}
