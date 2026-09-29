package com.example.data.model

/** A lecture (one audio file) by a [Scholar]. [durationSec] is optional; the player learns the real length. */
data class Lecture(
    val id: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val durationSec: Int? = null
)

/** A scholar shown under «علما و مشاهیر». [hue] (0..360) tints his generated cover art. */
data class Scholar(
    val id: String,
    val name: String,
    val tagline: String,
    val bio: String,
    val hue: Float,
    val lectures: List<Lecture>
)
