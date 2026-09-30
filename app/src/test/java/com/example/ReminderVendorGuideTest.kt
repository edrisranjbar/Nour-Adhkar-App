package com.example

import com.example.notifications.PhoneBrand
import com.example.notifications.ReminderVendorGuide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderVendorGuideTest {
    @Test
    fun manufacturersMapToBrandsIgnoringCaseAndSpaces() {
        assertEquals(PhoneBrand.XIAOMI, ReminderVendorGuide.brandFor("Xiaomi"))
        assertEquals(PhoneBrand.XIAOMI, ReminderVendorGuide.brandFor(" POCO "))
        assertEquals(PhoneBrand.HUAWEI, ReminderVendorGuide.brandFor("HONOR"))
        assertEquals(PhoneBrand.OPPO, ReminderVendorGuide.brandFor("realme"))
        assertEquals(PhoneBrand.VIVO, ReminderVendorGuide.brandFor("vivo"))
        assertEquals(PhoneBrand.SAMSUNG, ReminderVendorGuide.brandFor("samsung"))
    }

    @Test
    fun unknownOrMissingManufacturerUsesTheGenericGuide() {
        assertEquals(PhoneBrand.OTHER, ReminderVendorGuide.brandFor("Google"))
        assertEquals(PhoneBrand.OTHER, ReminderVendorGuide.brandFor(null))
    }

    @Test
    fun everyBrandHasANonBlankGuide() {
        PhoneBrand.entries.forEach { assertTrue(ReminderVendorGuide.stepsFor(it).isNotBlank()) }
    }
}
