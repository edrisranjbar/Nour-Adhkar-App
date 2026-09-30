package com.example.notifications

/** Phone brands whose battery managers commonly block background alarms beyond Android's own setting. */
enum class PhoneBrand { XIAOMI, HUAWEI, OPPO, VIVO, SAMSUNG, OTHER }

/**
 * Short, user-facing steps for vendor battery managers. Menu names differ between models and
 * system versions, so the text describes what to enable rather than promising exact labels.
 */
object ReminderVendorGuide {
    fun brandFor(manufacturer: String?): PhoneBrand = when (manufacturer?.trim()?.lowercase()) {
        "xiaomi", "redmi", "poco", "blackshark" -> PhoneBrand.XIAOMI
        "huawei", "honor" -> PhoneBrand.HUAWEI
        "oppo", "realme", "oneplus" -> PhoneBrand.OPPO
        "vivo", "iqoo" -> PhoneBrand.VIVO
        "samsung" -> PhoneBrand.SAMSUNG
        else -> PhoneBrand.OTHER
    }

    fun stepsFor(brand: PhoneBrand): String = when (brand) {
        PhoneBrand.XIAOMI ->
            "در تنظیمات گوشی، بخش برنامه‌ها ← مدیریت برنامه‌ها ← اذکار نور را باز کنید. «شروع خودکار» را روشن و صرفه‌جویی باتری را روی «بدون محدودیت» بگذارید. اذکار نور را در فهرست برنامه‌های اخیر قفل کنید."
        PhoneBrand.HUAWEI ->
            "در تنظیمات گوشی، بخش باتری ← راه‌اندازی برنامه‌ها را باز کنید. اذکار نور را روی «مدیریت دستی» بگذارید و راه‌اندازی خودکار، راه‌اندازی ثانویه و اجرا در پس‌زمینه را روشن کنید."
        PhoneBrand.OPPO ->
            "در تنظیمات گوشی، بخش باتری یا مدیریت برنامه‌ها ← اذکار نور را باز کنید. اجرا در پس‌زمینه و شروع خودکار را مجاز کنید و مصرف باتری را روی «بدون محدودیت» بگذارید."
        PhoneBrand.VIVO ->
            "در تنظیمات گوشی، بخش باتری ← مصرف انرژی در پس‌زمینه را باز کنید و اجرای اذکار نور در پس‌زمینه را مجاز کنید. اگر گزینه «شروع خودکار» وجود دارد، آن را روشن کنید."
        PhoneBrand.SAMSUNG ->
            "در تنظیمات گوشی، بخش باتری ← محدودیت‌های مصرف باتری در پس‌زمینه را باز کنید. اذکار نور را از فهرست «برنامه‌های در حالت خواب» و «برنامه‌های در حالت خواب عمیق» خارج کنید."
        PhoneBrand.OTHER ->
            "در تنظیمات گوشی، مصرف باتری اذکار نور را روی «بدون محدودیت» بگذارید و اجرا در پس‌زمینه را مجاز کنید."
    }

    const val MENU_NAMES_DISCLAIMER = "نام گزینه‌ها در مدل‌ها و نسخه‌های مختلف سیستم‌عامل ممکن است کمی متفاوت باشد."
}
