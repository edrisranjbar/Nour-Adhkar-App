package com.example.store

/** Only compiled into the Myket flavor; no competing store intents or fallback links. */
object StoreConfig {
    const val CHANNEL = "myket"
    const val WEB_URL = "https://myket.ir/app/ir.adhkar.app"
    const val DETAILS_URI = "myket://details?id=ir.adhkar.app"
    const val RATING_URI = "myket://comment?id=ir.adhkar.app"
    const val RATING_ACTION = "android.intent.action.VIEW"
    const val PACKAGE = "ir.mservices.market"
    const val UPDATE_URL = "https://raw.githubusercontent.com/edrisranjbar/Nour-Adhkar-App/main/version-myket.json"
    const val FALLBACK_UPDATE_URL = ""
    const val ACCEPT_LEGACY_METADATA = false
}
