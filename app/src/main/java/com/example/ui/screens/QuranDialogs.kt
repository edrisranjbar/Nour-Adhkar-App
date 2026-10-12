package com.example.ui.screens

import com.example.ui.language.text

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.quran.QuranPage
import com.example.quran.QuranRepository
import androidx.compose.runtime.key
import com.example.quran.QuranTranslation

@Composable
internal fun QuranGoToPageDialog(
    pageInput: String,
    onPageInputChange: (String) -> Unit,
    labels: QuranLabels,
    onDismiss: () -> Unit,
    onGo: (Int) -> Unit
) {
    val requestedPage = pageInput.toQuranPageNumber()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(labels.goToPage) },
        text = {
            OutlinedTextField(
                value = pageInput,
                onValueChange = { onPageInputChange(it.filter(Char::isDigit).take(3)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(labels.pageRange) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(
                enabled = requestedPage in 1..QuranRepository.PAGE_COUNT,
                onClick = { onGo(requestedPage) }
            ) { Text(labels.go) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(labels.cancel) }
        }
    )
}

@Composable
internal fun QuranReaderColorDialog(
    selected: QuranReaderColor,
    labels: QuranLabels,
    onSelect: (QuranReaderColor) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(labels.pageColor) },
        text = {
            Column {
                QuranReaderColor.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == option, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(labels.colorName(option))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(labels.cancel) }
        }
    )
}

@Composable
internal fun QuranTranslationDialog(
    options: List<QuranTranslation>,
    selected: QuranTranslation,
    labels: QuranLabels,
    onSelect: (QuranTranslation) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(labels.translationPicker) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == option, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(option.title)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(labels.cancel) }
        }
    )
}
