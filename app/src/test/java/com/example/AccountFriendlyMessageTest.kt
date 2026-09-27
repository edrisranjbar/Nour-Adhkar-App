package com.example

import com.example.data.repository.AccountRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountFriendlyMessageTest {
    private val serverError = "مشکلی در سرور پیش آمده است. لطفاً چند دقیقه دیگر دوباره تلاش کنید."

    @Test
    fun acceptsGenuinePersianSentences() {
        assertTrue(AccountRepository.isPersianSentence("رمز عبور اشتباه است"))
        assertTrue(AccountRepository.isPersianSentence("کد تأیید باید ۵ رقم باشد."))
        assertTrue(AccountRepository.isPersianSentence("لطفاً قبل از درخواست مجدد ۲ دقیقه صبر کنید."))
    }

    @Test
    fun rejectsMixedStrayOrLatinText() {
        assertFalse(AccountRepository.isPersianSentence("Server Error: خطا"))
        assertFalse(AccountRepository.isPersianSentence("Too Many Attempts."))
        assertFalse(AccountRepository.isPersianSentence("،"))
        assertFalse(AccountRepository.isPersianSentence("خ"))
        assertFalse(AccountRepository.isPersianSentence(""))
    }

    @Test
    fun showsPersianClientErrorsVerbatim() {
        assertEquals("رمز عبور اشتباه است", AccountRepository.friendlyMessage(401, "رمز عبور اشتباه است"))
    }

    @Test
    fun replacesMixedOrServerTextWithFriendlyMessage() {
        assertEquals("ایمیل یا رمز عبور نادرست است.", AccountRepository.friendlyMessage(401, "Server Error: خطا"))
        assertEquals(serverError, AccountRepository.friendlyMessage(500, "Server Error"))
        assertEquals(serverError, AccountRepository.friendlyMessage(500, "مشکل داخلی"))
    }
}
