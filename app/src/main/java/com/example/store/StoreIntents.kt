package com.example.store

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** One routing boundary for the compiled-in store, including a same-store web fallback. */
object StoreIntents {
    fun ratingIntent(): Intent = Intent(StoreConfig.RATING_ACTION, Uri.parse(StoreConfig.RATING_URI))
        .setPackage(StoreConfig.PACKAGE)

    fun updateIntent(): Intent = Intent(Intent.ACTION_VIEW, Uri.parse(StoreConfig.DETAILS_URI))
        .setPackage(StoreConfig.PACKAGE)

    fun webIntent(): Intent = Intent(Intent.ACTION_VIEW, Uri.parse(StoreConfig.WEB_URL))

    fun openRating(context: Context): Boolean = open(context, ratingIntent())
    fun openUpdate(context: Context): Boolean = open(context, updateIntent())

    private fun open(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            openWeb(context)
        } catch (_: SecurityException) {
            openWeb(context)
        }
    }

    private fun openWeb(context: Context): Boolean = try {
        context.startActivity(webIntent())
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
