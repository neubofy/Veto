package com.neubofy.veto.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.neubofy.veto.R
import com.neubofy.veto.data.LogEntry
import com.neubofy.veto.data.LogRepository
import com.neubofy.veto.ui.VetoActivity
import com.neubofy.veto.ui.theme.VetoTheme
import com.neubofy.veto.ui.theme.glassmorphism
import com.neubofy.veto.utils.log
import com.neubofy.veto.utils.writeToUri
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

private const val EXPORT_REQ_CODE = 30

class LogViewActivity : VetoActivity() {

    companion object {
        private val TAG = LogViewActivity::class.simpleName
    }

    private lateinit var repo: LogRepository
    private var logsState = mutableStateOf<List<LogEntry>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repo = LogRepository.getInstance(this)

        synchronized(repo.list) {
            logsState.value = repo.list.toList()
        }

        setContent {
            VetoTheme {
                LogViewScreen(
                    logs = logsState.value,
                    onExportClick = {
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            putExtra(Intent.EXTRA_TITLE, LogRepository.filenameForExport())
                            type = "*/*"
                        }
                        startActivityForResult(intent, EXPORT_REQ_CODE)
                    },
                    onClearClick = {
                        repo.clearLog()
                        synchronized(repo.list) {
                            logsState.value = repo.list.toList()
                        }
                    }
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        this.log().d(TAG, "requestCode=$requestCode resultCode=$resultCode")

        if (requestCode == EXPORT_REQ_CODE && resultCode == RESULT_OK) {
            if (data == null) {
                this.log().d(TAG, "data is null")
                return
            }

            val uri = data.data
            if (uri == null) {
                this.log().d(TAG, "uri is null")
                return
            }

            this.log().d(TAG, "exporting logs to $uri")

            lifecycleScope.launch {
                writeToUri(this@LogViewActivity, uri) { outputStream ->
                    synchronized(repo.list) {
                        val writer = OutputStreamWriter(outputStream)
                        repo.writeAsJson(writer)
                        writer.close()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewScreen(
    logs: List<LogEntry>,
    onExportClick: () -> Unit,
    onClearClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Logs") },
            text = { Text("Are you sure you want to delete all logs? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearClick()
                    showClearDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logs", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export Logs") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                onExportClick()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear Logs") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                showClearDialog = true
                                showMenu = false
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = "No logs available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { entry ->
                    LogItemCard(entry)
                }
            }
        }
    }
}

@Composable
fun LogItemCard(entry: LogEntry) {
    val levelColor = when (entry.level) {
        "E" -> MaterialTheme.colorScheme.error
        "W" -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        "I" -> MaterialTheme.colorScheme.primary
        "D" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val levelString = when (entry.level) {
        "E" -> "ERROR"
        "W" -> "WARN"
        "I" -> "INFO"
        "D" -> "DEBUG"
        "V" -> "VERBOSE"
        else -> "UNKNOWN"
    }

    val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormatter.format(Date(entry.timeMillis))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphism(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "$levelString - ${entry.tag}",
                style = MaterialTheme.typography.labelSmall,
                color = levelColor,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.msg,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}
