package com.example.prayer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Race foreground providers; a failed provider must not cancel another provider's fix. */
internal suspend fun <T : Any> awaitLocationResult(
    providers: List<String>,
    request: (String, (T?) -> Unit) -> (() -> Unit)
): T? {
    val cancellations = mutableListOf<() -> Unit>()
    fun cancelAll() { cancellations.forEach { runCatching { it() } } }
    try {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { cancelAll() }
            val pending = providers.toMutableSet()
            if (pending.isEmpty()) continuation.resume(null)
            providers.distinct().forEach { provider ->
                if (!continuation.isActive) return@forEach
                val deliver: (T?) -> Unit = { result ->
                    if (pending.remove(provider) && continuation.isActive && (result != null || pending.isEmpty())) {
                        continuation.resume(result)
                    }
                }
                try {
                    cancellations += request(provider, deliver)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    deliver(null)
                }
            }
        }
    } finally {
        cancelAll()
    }
}
