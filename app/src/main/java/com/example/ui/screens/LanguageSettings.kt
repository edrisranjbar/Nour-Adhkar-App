package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.example.ui.language.LocalizedIcon as Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.language.AppLanguage
import com.example.ui.language.LocalizedText as Text
import com.example.ui.viewmodel.AdhkarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettings(viewModel: AdhkarViewModel) {
    val language by viewModel.appLanguage.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(value = language.label, onValueChange = {}, readOnly = true,
            label = { Text("زبان برنامه") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true)
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppLanguage.entries.forEach { option ->
                DropdownMenuItem(text = { Text(option.label) }, onClick = {
                    viewModel.setAppLanguage(option)
                    expanded = false
                })
            }
        }
    }
}
