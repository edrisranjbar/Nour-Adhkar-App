package com.example

import com.example.prayer.MAX_PRAYER_PLACES
import com.example.prayer.PrayerPlace
import com.example.prayer.PrayerPlaces
import com.example.prayer.PrayerSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Saved prayer places; Robolectric provides org.json. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrayerPlacesTest {
    private val tehran = PrayerSettings("تهران", 35.69, 51.39, "Asia/Tehran", "MUSLIM_WORLD_LEAGUE", false, false, listOf(1, 0, 0, 0, 0, -2))
    private val mashhad = PrayerSettings("مشهد", 36.30, 59.60, "Asia/Tehran", "KARACHI", true, false)
    private val mecca = PrayerSettings("مکه", 21.42, 39.83, "Asia/Riyadh", "UMM_AL_QURA", false, false)

    @Test
    fun `migration keeps the single saved location exactly`() {
        val places = PrayerPlaces.migrate(tehran) { "a" }
        assertEquals(1, places.places.size)
        assertEquals("a", places.activeId)
        assertEquals("تهران", places.active?.name)
        assertEquals(tehran, places.active?.settings)
        assertTrue(PrayerPlaces.migrate(PrayerSettings()) { "x" }.places.isEmpty())
        assertEquals("مکان من", PrayerPlaces.migrate(tehran.copy(location = "موقعیت فعلی", automaticLocation = true)) { "b" }.active?.name)
    }

    @Test
    fun `json round trip preserves every per-place setting`() {
        val places = PrayerPlaces(listOf(PrayerPlace("a", "خانه", tehran), PrayerPlace("b", "حرم", mecca)), "b")
        val parsed = PrayerPlaces.fromJson(places.toJson(), "b")!!
        assertEquals(places, parsed)
        assertNull(PrayerPlaces.fromJson(null, null))
        // An unknown active id falls back to the first place.
        assertEquals("a", PrayerPlaces.fromJson(places.toJson(), "missing")!!.activeId)
    }

    @Test
    fun `adding activates the new place and respects the limit`() {
        var places = PrayerPlaces.migrate(tehran) { "a" }.add(PrayerPlace("b", "مشهد", mashhad))
        assertEquals("b", places.activeId)
        assertEquals("KARACHI", places.active?.settings?.method)
        repeat(20) { places = places.add(PrayerPlace("x$it", "x", mecca)) }
        assertEquals(MAX_PRAYER_PLACES, places.places.size)
    }

    @Test
    fun `removing the active place activates the first remaining and the last place stays`() {
        val places = PrayerPlaces(listOf(PrayerPlace("a", "خانه", tehran), PrayerPlace("b", "مشهد", mashhad)), "a")
        val removed = places.remove("a")
        assertEquals(listOf("b"), removed.places.map { it.id })
        assertEquals("b", removed.activeId)
        assertSame(removed, removed.remove("b"))
        assertEquals("a", places.remove("b").activeId)
    }

    @Test
    fun `switching, renaming and moving`() {
        val places = PrayerPlaces(listOf(PrayerPlace("a", "خانه", tehran), PrayerPlace("b", "مشهد", mashhad)), "a")
        assertEquals("b", places.activate("b").activeId)
        assertEquals("a", places.activate("nope").activeId)
        assertEquals("منزل", places.update(places.places[0].copy(name = "منزل")).places[0].name)
        assertEquals(listOf("b", "a"), places.move("b", -1).places.map { it.id })
        assertSame(places, places.move("a", -1))
    }
}
