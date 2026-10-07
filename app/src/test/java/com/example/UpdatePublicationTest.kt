package com.example

import com.example.store.StoreConfig
import com.example.updates.UpdateChecker
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class UpdatePublicationTest {
    private fun release() = JSONObject().put("store", StoreConfig.CHANNEL)
        .put("published", true).put("versionName", "3.0.0").put("versionCode", 30)
        .put("minRequiredVersionCode", 0)

    @Test fun `published newer release offers optional update`() {
        val update = UpdateChecker.parse(release(), 23)!!
        assertEquals(30, update.versionCode)
        assertFalse(update.isRequired)
        assertNull(UpdateChecker.parse(release(), 30))
    }

    @Test fun `required threshold applies only to the published release in this store`() {
        val json = release().put("minRequiredVersionCode", 25)
        assertTrue(UpdateChecker.parse(json, 23)!!.isRequired)
        assertFalse(UpdateChecker.parse(json, 25)!!.isRequired)
        assertNull(UpdateChecker.parse(json.put("published", false), 23))
    }

    @Test fun `competing store publication cannot prompt or force an update`() {
        val other = if (StoreConfig.CHANNEL == "myket") "bazaar" else "myket"
        assertNull(UpdateChecker.parse(release().put("store", other).put("minRequiredVersionCode", 30), 23))
    }

    @Test fun `legacy metadata is accepted only by the existing channel`() {
        val json = release().apply { remove("store"); remove("published") }
        assertEquals(StoreConfig.ACCEPT_LEGACY_METADATA, UpdateChecker.parse(json, 23) != null)
    }

    @Test fun `unpublished and invalid version metadata cannot force an update`() {
        assertNull(UpdateChecker.parse(release().put("published", false), 23))
        assertNull(UpdateChecker.parse(release().put("versionCode", 0), 23))
        assertNull(UpdateChecker.parse(release().put("versionName", ""), 23))
        assertNull(UpdateChecker.parse(release().put("minRequiredVersionCode", 31), 23))
        assertNull(UpdateChecker.parse(release().put("minRequiredVersionCode", -1), 23))
        assertNull(UpdateChecker.parse(JSONObject(), 23))
    }

    @Test fun `Myket metadata must explicitly confirm publication`() {
        if (StoreConfig.CHANNEL != "myket") return
        assertNull(UpdateChecker.parse(release().apply { remove("published") }, 23))
    }
}
