package com.example

import com.example.data.repository.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProgressSyncApiTest {
    private class Server(vararg statuses: Int) {
        private val replies = statuses.toList()
        val requests = mutableListOf<HttpURLConnection>()
        val bodies = mutableListOf<ByteArrayOutputStream>()
        val api = ProgressSyncApi { url ->
            val status = replies[requests.size]
            val body = ByteArrayOutputStream().also { bodies.add(it) }
            object : HttpURLConnection(url) {
                override fun connect() {}
                override fun disconnect() {}
                override fun usingProxy() = false
                override fun getResponseCode() = status
                override fun getOutputStream() = body
                override fun getInputStream() = ByteArrayInputStream("{\"records\":[]}".toByteArray())
            }.also { requests.add(it) }
        }
    }

    @Test fun successfulSessionDoesNotRefresh() = runBlocking {
        val server = Server(200)
        val result = server.api.sync("current", JSONArray()) { throw AssertionError("No refresh expected") }
        assertEquals("current", result.token)
        assertEquals(1, server.requests.size)
    }

    @Test fun expiredSessionRefreshesAndRetriesIdenticalPendingRecordsOnce() = runBlocking {
        val server = Server(401, 200)
        val records = JSONArray().put(JSONObject().put("key", "p:quran_last_read_page").put("value", 42))
        var refreshes = 0
        val result = server.api.sync("expired", records) {
            assertEquals("expired", it)
            refreshes++
            "renewed"
        }
        assertEquals(1, refreshes)
        assertEquals("renewed", result.token)
        assertEquals("Bearer expired", server.requests[0].getRequestProperty("Authorization"))
        assertEquals("Bearer renewed", server.requests[1].getRequestProperty("Authorization"))
        assertArrayEquals(server.bodies[0].toByteArray(), server.bodies[1].toByteArray())
    }

    @Test fun repeatedUnauthorizedResponseDoesNotLoopAndRequiresSignIn() = runBlocking {
        val server = Server(401, 401)
        var refreshes = 0
        val failure = runCatching { server.api.sync("expired", JSONArray()) { refreshes++; "renewed" } }.exceptionOrNull()!!
        assertEquals(1, refreshes)
        assertEquals(2, server.requests.size)
        assertEquals(ProgressSyncError.SIGN_IN_REQUIRED, syncError(failure as Exception))
    }

    @Test fun logoutDuringRefreshDoesNotSendAnotherUpload() = runBlocking {
        val server = Server(401)
        assertTrue(runCatching { server.api.sync("old", JSONArray()) { null } }.isFailure)
        assertEquals(1, server.requests.size)
    }

    @Test fun serverAndValidationFailuresDoNotRefreshSessions() = runBlocking {
        for ((status, reason) in listOf(503 to ProgressSyncError.SERVER, 422 to ProgressSyncError.INVALID_DATA,
            413 to ProgressSyncError.TOO_LARGE, 429 to ProgressSyncError.RATE_LIMITED)) {
            val server = Server(status)
            val failure = runCatching {
                server.api.sync("current", JSONArray()) { throw AssertionError("Must not refresh on $status") }
            }.exceptionOrNull()!!
            assertEquals(reason, syncError(failure as Exception))
            assertEquals(1, server.requests.size)
        }
        assertEquals(ProgressSyncError.SIGN_IN_REQUIRED, syncError(AuthException("Expired", 401)))
    }
}
