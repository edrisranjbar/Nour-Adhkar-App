package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.repository.ProgressSyncRepository
import com.example.data.repository.ProgressSyncError
import com.example.ui.language.AppLanguage
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.text
import com.example.ui.util.toPersianDigits
import com.example.ui.util.formatPersianDateTime
import java.text.DateFormat
import java.util.TimeZone

@Composable
fun ProgressSyncCard() {
    val context = LocalContext.current
    val arabic = LocalAppLanguage.current == AppLanguage.ARABIC
    val state by ProgressSyncRepository.state.collectAsState()
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (arabic) "نسخ التقدم ومزامنته" else "پشتیبان‌گیری و همگام‌سازی پیشرفت", style = MaterialTheme.typography.titleMedium)
            Text(if (arabic) "احفظ سلسلة الأيام والسجل والأعمال اليومية وتقدم القراءة في حسابك لاستعادتها على هاتف جديد. تُرسل إلى خادم أذكار نور؛ الاستخدام دون حساب يبقى متاحًا." else "زنجیره، تاریخچه، چک‌لیست و پیشرفت مطالعه را در حساب خود نگه دارید و روی گوشی جدید بازیابی کنید. این اطلاعات روی سرور اذکار نور ذخیره می‌شود؛ استفاده بدون حساب همچنان ممکن است.", style = MaterialTheme.typography.bodySmall)
            Text(if (arabic) "تتم مزامنة تقدمك تلقائيًا عند تسجيل الدخول." else "پیشرفت شما پس از ورود به حساب، به‌صورت خودکار همگام می‌شود.", style = MaterialTheme.typography.bodySmall)
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.error) {
                val message = when (state.errorReason) {
                    ProgressSyncError.SIGN_IN_REQUIRED -> "نشست حساب شما پایان یافته است. از حساب خارج شوید و دوباره وارد شوید تا همگام‌سازی ادامه یابد."
                    ProgressSyncError.CONNECTION -> "اتصال به سرور برقرار نشد. اینترنت را بررسی کنید؛ پیشرفت روی گوشی محفوظ است."
                    ProgressSyncError.SERVER -> "سرور همگام‌سازی موقتاً در دسترس نیست. پیشرفت روی گوشی محفوظ است؛ دوباره تلاش می‌کنیم."
                    ProgressSyncError.RATE_LIMITED -> "کمی صبر کنید؛ همگام‌سازی به‌صورت خودکار دوباره انجام می‌شود."
                    ProgressSyncError.INVALID_DATA -> "سرور اطلاعات پیشرفت را نپذیرفت. پیشرفت روی گوشی محفوظ است؛ با پشتیبانی تماس بگیرید."
                    ProgressSyncError.TOO_LARGE -> "حجم پشتیبان از حد مجاز سرور بیشتر است. پیشرفت روی گوشی محفوظ است؛ با پشتیبانی تماس بگیرید."
                    else -> "همگام‌سازی انجام نشد. پیشرفت روی گوشی محفوظ است؛ دوباره تلاش می‌کنیم."
                }
                Text(LocalAppLanguage.current.text(message), color = MaterialTheme.colorScheme.error)
            }
            if (state.lastSynced > 0) {
                val date = if (arabic) DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(state.lastSynced).toPersianDigits()
                else formatPersianDateTime(state.lastSynced, TimeZone.getDefault())
                Text((if (arabic) "آخر مزامنة: " else "آخرین همگام‌سازی: ") + date, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(
                modifier = Modifier.align(AbsoluteAlignment.Left),
                enabled = !state.busy,
                onClick = { ProgressSyncRepository.sync(context) }
            ) {
                Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(if (arabic) "مزامنة الآن" else "همام سازی")
            }
        }
    }
}
