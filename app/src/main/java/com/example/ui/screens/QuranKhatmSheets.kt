package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.quran.QuranKhatmDailyLog
import com.example.quran.QuranKhatmGoal
import com.example.quran.QuranKhatmPlan
import com.example.quran.QuranKhatmStatus
import com.example.quran.QuranRepository
import com.example.ui.language.AppLanguage
import com.example.ui.util.toPersianDigits
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun QuranKhatmProgressStrip(
    goal: QuranKhatmGoal,
    plan: QuranKhatmPlan,
    labels: QuranKhatmLabels,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = labels.compactTitle(plan),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (goal.paused) labels.paused else labels.percent(plan.progress),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(5.dp))
            LinearProgressIndicator(
                progress = { plan.progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun QuranKhatmSetupSheet(
    currentPage: Int,
    existingGoal: QuranKhatmGoal?,
    labels: QuranKhatmLabels,
    onDismiss: () -> Unit,
    onSave: (days: Int, startPage: Int, reminderEnabled: Boolean, reminderTime: String) -> Unit
) {
    val context = LocalContext.current
    var daysText by remember(existingGoal) { mutableStateOf((existingGoal?.targetDays ?: 30).toString()) }
    var startFromCurrent by remember(existingGoal) { mutableStateOf(existingGoal?.startPage?.let { it > 1 } ?: false) }
    var reminderEnabled by remember(existingGoal) { mutableStateOf(existingGoal?.reminderEnabled ?: true) }
    var reminderTime by remember(existingGoal) { mutableStateOf(existingGoal?.reminderTime ?: "20:00") }
    val days = daysText.toIntOrNull()?.takeIf { it in 1..3650 }
    val startPage = if (existingGoal != null) existingGoal.startPage else if (startFromCurrent) currentPage else 1
    val totalPages = QuranRepository.PAGE_COUNT - startPage + 1
    val dailyPages = days?.let { kotlin.math.ceil(totalPages / it.toDouble()).toInt() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (existingGoal == null) labels.createTitle else labels.editTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = labels.setupDescription,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(labels.duration, style = MaterialTheme.typography.labelLarge)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf(7, 30, 60, 90)) { preset ->
                    FilterChip(
                        selected = days == preset,
                        onClick = { daysText = preset.toString() },
                        label = { Text(labels.days(preset)) }
                    )
                }
            }
            OutlinedTextField(
                value = daysText,
                onValueChange = { daysText = it.filter(Char::isDigit).take(4) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(labels.customDays) },
                supportingText = dailyPages?.let { { Text(labels.preview(it, startPage)) } },
                isError = days == null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            if (existingGoal == null) {
                Text(
                    text = labels.startingPoint,
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { startFromCurrent = false }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = !startFromCurrent, onClick = null)
                    Text(labels.fromBeginning)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { startFromCurrent = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = startFromCurrent, onClick = null)
                    Text(labels.fromCurrentPage(currentPage))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(labels.dailyReminder, style = MaterialTheme.typography.titleSmall)
                    Text(
                        labels.dailyReminderDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
            }
            if (reminderEnabled) {
                OutlinedButton(
                    onClick = {
                        val parts = reminderTime.split(":")
                        TimePickerDialog(
                            context,
                            { _, hour, minute -> reminderTime = "%02d:%02d".format(hour, minute) },
                            parts.getOrNull(0)?.toIntOrNull() ?: 20,
                            parts.getOrNull(1)?.toIntOrNull() ?: 0,
                            true
                        ).show()
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) { Text(labels.reminderAt(reminderTime)) }
            }

            Button(
                enabled = days != null,
                onClick = { days?.let { onSave(it, startPage, reminderEnabled, reminderTime) } },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
            ) {
                Text(if (existingGoal == null) labels.startGoal else labels.saveChanges)
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun QuranKhatmDetailsSheet(
    goal: QuranKhatmGoal,
    plan: QuranKhatmPlan,
    logs: List<QuranKhatmDailyLog>,
    currentPage: Int,
    labels: QuranKhatmLabels,
    onDismiss: () -> Unit,
    onContinue: (Int) -> Unit,
    onRecord: () -> Unit,
    onEdit: () -> Unit,
    onPauseChanged: (Boolean) -> Unit,
    onCancel: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
            Text(labels.goalTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                text = labels.status(plan.status),
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
            LinearProgressIndicator(
                progress = { plan.progress },
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(8.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(labels.completedPages(goal), style = MaterialTheme.typography.bodyMedium)
                Text(labels.percent(plan.progress), style = MaterialTheme.typography.bodyMedium)
            }

            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(labels.today, style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = if (goal.isComplete) labels.completed else labels.todayRange(plan),
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = labels.deadline(plan.deadlineDayKey),
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (!goal.isComplete) {
                Button(
                    enabled = !goal.paused,
                    onClick = { onContinue(plan.targetStartPage) },
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                ) { Text(labels.continueReading) }
                OutlinedButton(
                    enabled = !goal.paused && currentPage >= goal.startPage && currentPage > goal.lastCompletedPage,
                    onClick = onRecord,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text(labels.recordThrough(currentPage)) }
            }

            Text(
                text = labels.dailyLog,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (logs.isEmpty()) {
                Text(labels.noDailyLog, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                logs.take(7).forEach { log ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(labels.date(log.dayKey), style = MaterialTheme.typography.bodyMedium)
                        Text(labels.throughPage(log.completedThroughPage), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEdit) { Text(labels.editGoal) }
                if (!goal.isComplete) {
                    TextButton(onClick = { onPauseChanged(!goal.paused) }) {
                        Text(if (goal.paused) labels.resume else labels.pause)
                    }
                }
                TextButton(onClick = onCancel) { Text(labels.cancelGoal, color = MaterialTheme.colorScheme.error) }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

internal class QuranKhatmLabels(private val language: AppLanguage) {
    private val arabic = language == AppLanguage.ARABIC
    val menuTitle get() = if (arabic) "خطة ختم القرآن" else "برنامه ختم قرآن"
    val createTitle get() = if (arabic) "إنشاء خطة ختم" else "ساخت برنامه ختم"
    val editTitle get() = if (arabic) "تعديل خطة الختم" else "ویرایش برنامه ختم"
    val setupDescription get() = if (arabic) "حدد المدة، وسنقسم الصفحات المتبقية إلى ورد يومي مرن." else "مدت را مشخص کنید؛ صفحات باقی‌مانده به ورد روزانهٔ منعطف تقسیم می‌شوند."
    val duration get() = if (arabic) "مدة الختم" else "مدت ختم"
    val customDays get() = if (arabic) "عدد الأيام" else "تعداد روزها"
    val startingPoint get() = if (arabic) "نقطة البداية" else "نقطه شروع"
    val fromBeginning get() = if (arabic) "من الصفحة الأولى" else "از صفحه اول"
    val dailyReminder get() = if (arabic) "تذكير يومي" else "یادآوری روزانه"
    val dailyReminderDescription get() = if (arabic) "يفتح الورد التالي مباشرة" else "مستقیماً ورد بعدی را باز می‌کند"
    val startGoal get() = if (arabic) "ابدأ الختم" else "شروع ختم"
    val saveChanges get() = if (arabic) "حفظ التغييرات" else "ذخیره تغییرات"
    val goalTitle get() = if (arabic) "تقدم ختم القرآن" else "پیشرفت ختم قرآن"
    val paused get() = if (arabic) "متوقفة" else "متوقف"
    val today get() = if (arabic) "ورد اليوم" else "ورد امروز"
    val completed get() = if (arabic) "تم ختم القرآن، تقبل الله" else "ختم قرآن کامل شد؛ قبول باشد"
    val continueReading get() = if (arabic) "متابعة التلاوة" else "ادامه تلاوت"
    val dailyLog get() = if (arabic) "السجل اليومي" else "گزارش روزانه"
    val noDailyLog get() = if (arabic) "لم تسجل تلاوة بعد." else "هنوز تلاوتی ثبت نشده است."
    val editGoal get() = if (arabic) "تعديل" else "ویرایش"
    val pause get() = if (arabic) "إيقاف مؤقت" else "توقف موقت"
    val resume get() = if (arabic) "استئناف" else "ادامه برنامه"
    val cancelGoal get() = if (arabic) "إلغاء الخطة" else "لغو برنامه"

    fun days(value: Int) = if (arabic) "$value أيام" else "${value.toPersianDigits()} روز"
    fun fromCurrentPage(page: Int) = if (arabic) "من الصفحة $page" else "از صفحه ${page.toPersianDigits()}"
    fun preview(pages: Int, startPage: Int) = if (arabic) {
        "نحو $pages صفحات يومياً، بدءاً من الصفحة $startPage"
    } else {
        "حدود ${pages.toPersianDigits()} صفحه در روز، از صفحه ${startPage.toPersianDigits()}"
    }
    fun reminderAt(time: String) = if (arabic) "وقت التذكير: $time" else "زمان یادآوری: ${time.toPersianDigits()}"
    fun compactTitle(plan: QuranKhatmPlan) = if (arabic) {
        "ختم القرآن · اليوم ${plan.dayNumber} من ${plan.targetDays}"
    } else {
        "ختم قرآن · روز ${plan.dayNumber.toPersianDigits()} از ${plan.targetDays.toPersianDigits()}"
    }
    fun percent(progress: Float) = "${(progress * 100).toInt().coerceIn(0, 100).toPersianDigits()}٪"
    fun status(status: QuranKhatmStatus) = when (status) {
        QuranKhatmStatus.AHEAD -> if (arabic) "متقدم على الخطة" else "جلوتر از برنامه"
        QuranKhatmStatus.ON_TRACK -> if (arabic) "حسب الخطة" else "مطابق برنامه"
        QuranKhatmStatus.BEHIND -> if (arabic) "بحاجة إلى تعويض" else "نیاز به جبران"
        QuranKhatmStatus.COMPLETE -> if (arabic) "اكتمل الختم" else "ختم کامل شده"
    }
    fun completedPages(goal: QuranKhatmGoal): String {
        val completed = (goal.lastCompletedPage - goal.startPage + 1).coerceAtLeast(0)
        val total = QuranRepository.PAGE_COUNT - goal.startPage + 1
        return if (arabic) "$completed من $total صفحة" else "${completed.toPersianDigits()} از ${total.toPersianDigits()} صفحه"
    }
    fun todayRange(plan: QuranKhatmPlan) = if (arabic) {
        "الصفحات ${plan.targetStartPage}–${plan.targetEndPage}"
    } else {
        "صفحات ${plan.targetStartPage.toPersianDigits()} تا ${plan.targetEndPage.toPersianDigits()}"
    }
    fun deadline(dayKey: Long) = if (arabic) "موعد الإتمام: ${date(dayKey)}" else "تاریخ پایان: ${date(dayKey)}"
    fun recordThrough(page: Int) = if (arabic) "تسجيل حتى الصفحة $page" else "ثبت تلاوت تا صفحه ${page.toPersianDigits()}"
    fun throughPage(page: Int) = if (arabic) "حتى الصفحة $page" else "تا صفحه ${page.toPersianDigits()}"
    fun date(dayKey: Long): String = SimpleDateFormat("yyyy/MM/dd", Locale.US)
        .format(Date(dayKey))
        .let { if (arabic) it else it.toPersianDigits() }
}
