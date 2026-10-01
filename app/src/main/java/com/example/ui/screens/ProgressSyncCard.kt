package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.repository.ProgressSyncRepository
import com.example.ui.language.AppLanguage
import com.example.ui.language.LocalAppLanguage
import com.example.ui.util.toPersianDigits
import java.text.DateFormat

@Composable
fun ProgressSyncCard() {
    val context = LocalContext.current
    val arabic = LocalAppLanguage.current == AppLanguage.ARABIC
    val state by ProgressSyncRepository.state.collectAsState()
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (arabic) "نسخ التقدم ومزامنته" else "پشتیبان‌گیری و همگام‌سازی پیشرفت", style = MaterialTheme.typography.titleMedium)
            Text(if (arabic) "احفظ سلسلة الأيام والسجل والأعمال اليومية وتقدم القراءة في حسابك لاستعادتها على هاتف جديد. تُرسل إلى خادم أذكار نور؛ الاستخدام دون حساب يبقى متاحًا." else "زنجیره، تاریخچه، چک‌لیست و پیشرفت مطالعه را در حساب خود نگه دارید و روی گوشی جدید بازیابی کنید. این اطلاعات روی سرور اذکار نور ذخیره می‌شود؛ استفاده بدون حساب همچنان ممکن است.", style = MaterialTheme.typography.bodySmall)
            Text(if (arabic) "تتم مزامنة تقدمك تلقائيًا عند تسجيل الدخول." else "پیشرفت شما پس از ورود به حساب، به‌صورت خودکار همگام می‌شود.", style = MaterialTheme.typography.bodySmall)
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.error) Text(if (arabic) "تعذرت المزامنة. تقدمك محفوظ على الهاتف؛ سنحاول مجددًا." else "همگام‌سازی انجام نشد. پیشرفت روی گوشی محفوظ است؛ دوباره تلاش می‌کنیم.", color = MaterialTheme.colorScheme.error)
            if (state.lastSynced > 0) Text((if (arabic) "آخر مزامنة: " else "آخرین همگام‌سازی: ") + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(state.lastSynced).toPersianDigits(), style = MaterialTheme.typography.bodySmall)
            TextButton(enabled = !state.busy, onClick = { ProgressSyncRepository.sync(context) }) {
                Text(if (arabic) "مزامنة الآن" else "همگام‌سازی اکنون")
            }
        }
    }
}
