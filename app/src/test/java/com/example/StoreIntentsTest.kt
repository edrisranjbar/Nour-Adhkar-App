package com.example

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import com.example.store.StoreConfig
import com.example.store.StoreIntents
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class StoreIntentsTest {
    private class RecordingContext(private val reject: (Intent) -> Boolean) : ContextWrapper(null) {
        val attempts = mutableListOf<Intent>()
        override fun startActivity(intent: Intent) {
            attempts += intent
            if (reject(intent)) throw ActivityNotFoundException()
        }
    }

    @Test fun `rating opens the official comment action for the selected store`() {
        val context = RecordingContext { false }
        assertTrue(StoreIntents.openRating(context))
        val intent = context.attempts.single()
        if (StoreConfig.CHANNEL == "myket") {
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals("myket://comment?id=ir.adhkar.app", intent.dataString)
            assertEquals("ir.mservices.market", intent.`package`)
        } else {
            assertEquals(Intent.ACTION_EDIT, intent.action)
            assertEquals("bazaar://details?id=ir.adhkar.app", intent.dataString)
            assertEquals("com.farsitel.bazaar", intent.`package`)
        }
    }

    @Test fun `missing store falls back to the same store website without an explicit package`() {
        val context = RecordingContext { it.`package` != null }
        assertTrue(StoreIntents.openUpdate(context))
        assertEquals(2, context.attempts.size)
        assertEquals("${StoreConfig.CHANNEL}://details?id=ir.adhkar.app", context.attempts.first().dataString)
        val web = context.attempts.last()
        assertEquals(Intent.ACTION_VIEW, web.action)
        assertNull(web.`package`)
        assertEquals(if (StoreConfig.CHANNEL == "myket") "https://myket.ir/app/ir.adhkar.app"
            else "https://cafebazaar.ir/app/ir.adhkar.app", web.dataString)
    }

    @Test fun `no store and no browser does not report a successful rating launch`() {
        assertFalse(StoreIntents.openRating(RecordingContext { true }))
    }

    @Test fun `blocked store activity still opens a browser`() {
        val context = object : ContextWrapper(null) {
            val attempts = mutableListOf<Intent>()
            override fun startActivity(intent: Intent) {
                attempts += intent
                if (intent.`package` != null) throw SecurityException()
            }
        }
        assertTrue(StoreIntents.openRating(context))
        assertEquals(StoreConfig.WEB_URL, context.attempts.last().dataString)
    }
}
