package com.example

import com.example.prayer.awaitLocationResult
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.*
import org.junit.Test

class LocationResultTest {
    @Test fun providerFailureStillAllowsAnotherProviderToLocate() = runBlocking {
        val result = awaitLocationResult<String>(listOf("gps", "network")) { provider, deliver ->
            if (provider == "gps") throw SecurityException("Precise permission unavailable")
            deliver("network fix")
            val cancel: () -> Unit = {}
            cancel
        }
        assertEquals("network fix", result)
    }

    @Test fun nullFromOneProviderDoesNotFinishTheRequest() = runBlocking {
        val callbacks = mutableMapOf<String, (String?) -> Unit>()
        var stopped = 0
        val pending = async(start = CoroutineStart.UNDISPATCHED) {
            awaitLocationResult<String>(listOf("gps", "network")) { provider, deliver ->
                callbacks[provider] = deliver
                val cancel: () -> Unit = { stopped++ }
                cancel
            }
        }
        callbacks.getValue("network")(null)
        assertFalse(pending.isCompleted)
        callbacks.getValue("gps")("gps fix")
        assertEquals("gps fix", pending.await())
        assertEquals(2, stopped)
        // Late or repeated callbacks cannot complete the continuation twice.
        callbacks.getValue("gps")("late fix")
    }

    @Test fun cancellationStopsEveryRegisteredProvider() = runBlocking {
        var stopped = 0
        val pending = async(start = CoroutineStart.UNDISPATCHED) {
            awaitLocationResult<String>(listOf("gps", "network")) { _, _ -> { stopped++ } }
        }
        pending.cancelAndJoin()
        assertTrue(stopped >= 2)
    }

    @Test fun allProvidersUnavailableReturnsNull() = runBlocking {
        val result = awaitLocationResult<String>(listOf("gps", "network")) { _, _ ->
            throw IllegalArgumentException("Provider unavailable")
        }
        assertNull(result)
    }

    @Test fun timeoutStopsPendingLocationRequests() = runBlocking {
        var stopped = false
        val result = withTimeoutOrNull(50) {
            awaitLocationResult<String>(listOf("gps")) { _, _ -> { stopped = true } }
        }
        assertNull(result)
        assertTrue(stopped)
    }

    @Test fun noProvidersReturnsNull() = runBlocking {
        assertNull(awaitLocationResult<String>(emptyList()) { _, _ -> error("Must not request") })
    }
}
