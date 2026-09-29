package com.example.data.model

/** A lecture (one audio file) by a [Scholar]. [durationSec] is optional; the player learns the real length. */
data class Lecture(
    val id: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val durationSec: Int? = null
)

/**
 * A scholar shown under «علما و مشاهیر». [photoUrl] is set from the admin panel; without it the
 * app draws cover art tinted by [hue] (0..360).
 */
data class Scholar(
    val id: String,
    val name: String,
    val tagline: String,
    val bio: String,
    val hue: Float,
    val photoUrl: String? = null,
    val lectures: List<Lecture>
)
