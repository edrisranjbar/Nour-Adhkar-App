package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.repository.AppInboxApi
import com.example.data.repository.AppNotice
import com.example.ui.util.formatInboxDate
import kotlinx.coroutines.launch

@Composable
fun AppInboxScreen(innerPadding: PaddingValues) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var notices by remember { mutableStateOf<List<AppNotice>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    fun refresh() {
        scope.launch {
            loading = true
            try { notices = AppInboxApi.notices(context); error = null }
            catch (e: Exception) { error = AppInboxApi.describe(e) }
            finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { refresh() }
    if (loading && notices.isEmpty()) {
        // First load: center the spinner in the whole content area.
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding()).padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }
        error?.let { message -> item { Column { Text(message); TextButton(onClick = ::refresh) { Text("تلاش دوباره") } } } }
        if (!loading && error == null && notices.isEmpty()) item { Text("هنوز پیامی دریافت نکرده‌اید.") }
        items(notices, key = { it.id }) { notice ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text((if (notice.read) "" else "●  ") + notice.title, style = MaterialTheme.typography.titleMedium)
                    Text(notice.message, style = MaterialTheme.typography.bodyMedium)
                    Text(remember(notice.date) { formatInboxDate(notice.date) }, style = MaterialTheme.typography.labelSmall)
                    if (!notice.read) TextButton(onClick = {
                        scope.launch {
                            try {
                                AppInboxApi.markRead(context, notice.id)
                                notices = notices.map { if (it.id == notice.id) it.copy(read = true) else it }
                            } catch (e: Exception) { error = AppInboxApi.describe(e) }
                        }
                    }) { Text("علامت‌گذاری به‌عنوان خوانده‌شده") }
                }
            }
        }
    }
}
