package com.example.ui.screens

import com.example.ui.util.toPersianDigits
import java.util.Locale

internal fun audioSize(bytes: Long): String =
    if (bytes < 0) "—" else String.format(Locale.US, "%.1f MB", bytes / 1048576.0).toPersianDigits()
