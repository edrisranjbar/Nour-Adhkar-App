package com.example.ui.util

private val latinDigits = '0'..'9'
private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

fun String.toPersianDigits(): String = map { character ->
    if (character in latinDigits) persianDigits[character - '0'] else character
}.joinToString(separator = "")

fun Number.toPersianDigits(): String = toString().toPersianDigits()
