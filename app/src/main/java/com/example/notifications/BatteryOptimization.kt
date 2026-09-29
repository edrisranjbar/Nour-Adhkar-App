package com.example.notifications

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * Adhan and reminder alarms must fire while the phone is idle. Battery optimization (Doze,
 * app standby and vendor battery savers) can delay or drop them, so the app asks the user
 * to exempt it. The request is always user-initiated; nothing changes without their approval.
 */
object BatteryOptimization {
    fun isIgnoring(context: Context): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Opens the system "allow unrestricted battery" dialog, falling back to the settings list. */
    @SuppressLint("BatteryLife") // Adhan and reminder alarms are this app's core, user-enabled feature.
    fun request(context: Context) {
        if (isIgnoring(context)) return
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
        val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        for (intent in listOf(direct, fallback, details)) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
    }
}
