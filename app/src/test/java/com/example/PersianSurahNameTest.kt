package com.example

import com.example.ui.screens.persianSurahName
import org.junit.Assert.assertEquals
import org.junit.Test

class PersianSurahNameTest {
    @Test fun dropsTheArticleAndUsesPersianLetters() {
        assertEquals("بقره", persianSurahName("البقرة"))
        assertEquals("فاتحه", persianSurahName("الفاتحة"))
        assertEquals("اعلی", persianSurahName("الأعلى"))
        assertEquals("اسراء", persianSurahName("الإسراء"))
        assertEquals("کهف", persianSurahName("الكهف"))
    }

    @Test fun leavesNamesWithoutTheArticleAlone() {
        assertEquals("آل عمران", persianSurahName("آل عمران"))
        assertEquals("یس", persianSurahName("يس"))
        assertEquals("طه", persianSurahName("طه"))
    }
}
