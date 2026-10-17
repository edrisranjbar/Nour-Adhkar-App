package com.example.ui.screens

import com.example.data.repository.CitySearchRepository
import com.example.data.repository.CitySearchResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.LocalizedIcon as Icon
import com.example.ui.language.LocalizedText as Text
import kotlinx.coroutines.CancellationException

@Composable
fun CityLocationSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    onSelected: (String, Double, Double) -> Unit
) {
    val context = LocalContext.current
    val language = LocalAppLanguage.current
    var request by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<CitySearchResult>>(emptyList()) }
    var source by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(request, language) {
        val name = request ?: return@LaunchedEffect
        searching = true
        error = null
        results = emptyList()
        try {
            val found = CitySearchRepository.search(context.applicationContext, name, language.localeTag)
            results = found.cities
            source = found.source
            if (results.isEmpty()) error = "شهری پیدا نشد؛ نام شهر و کشور را وارد کنید."
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = "جستجوی شهر ممکن نیست؛ اتصال اینترنت را بررسی و دوباره تلاش کنید."
        } finally {
            searching = false
            request = null
        }
    }
    fun search() { if (query.isNotBlank() && !searching) request = query.trim() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                request = null
                results = emptyList()
                error = null
                onQueryChange(it)
            },
            label = { Text("نام شهر") },
            placeholder = { Text("مثلاً شیراز، ایران") },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { search() }),
            trailingIcon = {
                IconButton(onClick = { search() }, enabled = query.isNotBlank() && !searching) {
                    if (searching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Search, contentDescription = "جستجوی شهر")
                }
            }
        )
        if (results.isNotEmpty()) {
            Text("شهر موردنظر را انتخاب کنید", style = MaterialTheme.typography.bodySmall)
            results.forEach { address ->
                val label = address.label
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                    onSelected(label, address.latitude, address.longitude)
                    results = emptyList()
                    error = null
                }) { Text(label) }
            }
            source?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall) }
    }
}
