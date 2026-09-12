package com.example.ui.language

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.Painter

@Composable
fun LocalizedIcon(imageVector: ImageVector, contentDescription: String?, modifier: Modifier = Modifier,
    tint: Color = androidx.compose.material3.LocalContentColor.current) {
    androidx.compose.material3.Icon(imageVector, contentDescription?.let { LocalAppLanguage.current.text(it) }, modifier, tint)
}

@Composable
fun LocalizedIcon(painter: Painter, contentDescription: String?, modifier: Modifier = Modifier,
    tint: Color = androidx.compose.material3.LocalContentColor.current) {
    androidx.compose.material3.Icon(painter, contentDescription?.let { LocalAppLanguage.current.text(it) }, modifier, tint)
}
