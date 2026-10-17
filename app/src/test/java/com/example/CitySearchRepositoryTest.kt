package com.example

import com.example.data.repository.CitySearchRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CitySearchRepositoryTest {
    @Test fun preservesCityCoordinatesAndDisambiguatesWithRegionAndCountry() {
        val result = CitySearchRepository.parse("""{"results":[
            {"name":"شیراز","admin1":"فارس","country":"ایران","latitude":29.6103,"longitude":52.5311}
        ]}""").single()
        assertEquals("شیراز، فارس، ایران", result.label)
        assertEquals(29.6103, result.latitude, 0.00001)
        assertEquals(52.5311, result.longitude, 0.00001)
    }

    @Test fun missingOrInvalidCoordinatesAreNeverSubstitutedWithZero() {
        assertTrue(CitySearchRepository.parse("""{"results":[
            {"name":"Missing"},
            {"name":"Invalid","latitude":91,"longitude":52},
            {"name":"Invalid","latitude":29,"longitude":181}
        ]}""").isEmpty())
    }

    @Test fun duplicateResultsAreRemoved() {
        val city = """{"name":"تهران","country":"ایران","latitude":35.69,"longitude":51.39}"""
        assertEquals(1, CitySearchRepository.parse("""{"results":[$city,$city]}""").size)
    }

    @Test fun missingResultsIsAnEmptySearch() {
        assertTrue(CitySearchRepository.parse("{}").isEmpty())
    }

    @Test(expected = IllegalStateException::class)
    fun serverErrorIsNotShownAsAnEmptySearch() {
        CitySearchRepository.parse("""{"error":true,"reason":"Unavailable"}""")
    }

    @Test fun encodesPersianNamesAndNormalizesTheCountrySeparator() {
        val url = CitySearchRepository.searchUrl(" شیراز، ایران ", "fa-IR")
        assertTrue(url.contains("name=%D8%B4%DB%8C%D8%B1%D8%A7%D8%B2%2C+%D8%A7%DB%8C%D8%B1%D8%A7%D9%86"))
        assertTrue(url.contains("language=fa"))
    }
}
