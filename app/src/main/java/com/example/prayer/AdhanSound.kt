package com.example.prayer

import androidx.annotation.RawRes
import com.example.R

data class AdhanRecording(
    val id: String,
    val title: String,
    @param:RawRes val resource: Int
)

val adhanRecordings = listOf(
    AdhanRecording("adhan_one", "علی بن احمد ملا (مکه)", R.raw.adhan_makkah),
    AdhanRecording("adhan_two", "اذان مسجدالنبی (مدینه)", R.raw.adhan_madinah),
    AdhanRecording("adhan_three", "مشاری راشد العفاسی", R.raw.adhan_alafasy)
)

data class AdhanSound(val id: String = "") {
    val isSelected: Boolean get() = adhanRecordings.any { it.id == id }
}
