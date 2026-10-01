package com.example

import com.example.data.repository.ProgressRecords
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProgressRecordsTest {
    @Test fun deletionSurvivesAnOlderDeviceAndRepeatedRestore() {
        val data = JSONObject().put("h:session", "history")
        val first = ProgressRecords.capture(JSONObject(), JSONObject(), data, "a", 100)
        val deleted = ProgressRecords.capture(data, first, JSONObject(), "a", 200)
        val merged = ProgressRecords.merge(deleted, ProgressRecords.array(first))
        assertTrue(merged.getJSONObject("h:session").getBoolean("deleted"))
        assertEquals(0, ProgressRecords.snapshot(merged).length())
        assertEquals(merged.toString(), ProgressRecords.merge(merged, ProgressRecords.array(merged)).toString())
    }

    @Test fun newPhoneRestoresUnionAndDeterministicConflicts() {
        val first = ProgressRecords.capture(JSONObject(), JSONObject(), JSONObject().put("day:a", true), "a", 100)
        val second = ProgressRecords.capture(JSONObject(), JSONObject(), JSONObject().put("day:b", true), "b", 100)
        val union = ProgressRecords.merge(first, ProgressRecords.array(second))
        assertEquals(2, ProgressRecords.snapshot(union).length())
        val changed = ProgressRecords.capture(JSONObject().put("day:a", true), first, JSONObject(), "b", 100)
        assertTrue(ProgressRecords.merge(first, ProgressRecords.array(changed)).getJSONObject("day:a").getBoolean("deleted"))
        assertEquals(2, ProgressRecords.snapshot(ProgressRecords.merge(JSONObject(), ProgressRecords.array(union))).length())
    }

    @Test fun unchangedSnapshotDoesNotAcquireNewTimestamp() {
        val snapshot = JSONObject().put("counter", 12)
        val records = ProgressRecords.capture(JSONObject(), JSONObject(), snapshot, "a", 100)
        assertEquals(100L, ProgressRecords.capture(snapshot, records, snapshot, "a", 200).getJSONObject("counter").getLong("modified"))
    }
}
