package com.example.store

/** Only compiled into the Bazaar flavor. Keep competing store links out of this source set. */
object StoreConfig {
    const val CHANNEL = "bazaar"
    const val WEB_URL = "https://cafebazaar.ir/app/ir.adhkar.app"
    const val DETAILS_URI = "bazaar://details?id=ir.adhkar.app"
    const val RATING_URI = DETAILS_URI
    const val RATING_ACTION = "android.intent.action.EDIT"
    const val PACKAGE = "com.farsitel.bazaar"
    const val UPDATE_URL = "https://api.adhkar.ir/api/app-version"
    const val FALLBACK_UPDATE_URL = "https://raw.githubusercontent.com/edrisranjbar/Nour-Adhkar-App/main/version.json"
    const val ACCEPT_LEGACY_METADATA = true
}
