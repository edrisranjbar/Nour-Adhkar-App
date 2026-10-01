package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.repository.ProgressSnapshot
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProgressSnapshotTest {
    @Test fun restoresWithoutDuplicatingHistoryOrChangingLocationAndPreservesInFlightEdits() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("nour_adhkar_prefs", 0)
        prefs.edit().clear().putString("prayer_location", "تهران").putStringSet("activity_day_keys", setOf("123"))
            .putInt("quran_last_read_page", 20).commit()
        val db = AdhkarDatabase.getDatabase(context)
        db.tasbihSessionDao().clearHistory()
        db.dhikrProgressDao().deleteAllProgress()
        db.tasbihSessionDao().insertSession(TasbihSessionEntity(dhikrName = "test", count = 3, timestamp = 100))
        val snapshot = ProgressSnapshot(context)
        val backup = snapshot.read()
        assertFalse(backup.has("p:prayer_location"))
        prefs.edit().remove("activity_day_keys").remove("quran_last_read_page").commit()
        db.tasbihSessionDao().clearHistory()
        snapshot.apply(snapshot.read(), backup)
        snapshot.apply(snapshot.read(), backup)
        assertEquals(1, db.tasbihSessionDao().getAllSessions().size)
        assertEquals(setOf("123"), prefs.getStringSet("activity_day_keys", emptySet()))
        assertEquals("تهران", prefs.getString("prayer_location", null))
        val before = snapshot.read()
        prefs.edit().putInt("quran_last_read_page", 50).commit()
        val remote = JSONObject(before.toString()).put("p:quran_last_read_page", JSONObject().put("type", "int").put("value", 30))
        snapshot.apply(before, remote)
        assertEquals(50, prefs.getInt("quran_last_read_page", 0))
    }
}
