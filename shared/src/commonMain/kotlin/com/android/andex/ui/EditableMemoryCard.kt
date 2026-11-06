package com.android.andex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.andex.ui.theme.*

internal expect fun memoryDialogProperties(): DialogProperties

@Composable
fun EditableMemoryCard(id: String, title: String, value: String, onSave: ((String, String) -> Unit)?, onDelete: (() -> Unit)? = null) {
    var opened by rememberSaveable(id) { mutableStateOf(false) }
    var editing by remember(id) { mutableStateOf(false) }
    // Large imported documents stay out of the saved-instance-state Bundle.
    var draft by remember(id) { mutableStateOf("") }
    var previous by remember(id) { mutableStateOf("") }
    LaunchedEffect(value) {
        if (editing && value != previous && value == draft) editing = false
    }
    Column(Modifier.fillMaxWidth().clickable { opened = true }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = AndexOnSurface)
            Text("查看", style = MaterialTheme.typography.labelMedium, color = AndexPrimary)
        }
        Text(value.ifBlank { "暂无记忆" }.take(240), maxLines = 2, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall, color = AndexOnSurfaceVariant)
    }
    if (opened) Dialog(
        onDismissRequest = { if (editing) editing = false else opened = false },
        properties = memoryDialogProperties(),
    ) {
        Surface(Modifier.fillMaxSize(), color = AndexBackground) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    TextButton(onClick = { if (editing) editing = false else opened = false }) { Text(if (editing) "取消" else "返回") }
                    Text(title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium, color = AndexOnSurface)
                    if (editing) {
                        TextButton(onClick = { if (draft.isBlank()) opened = false; onSave?.invoke(draft, previous) }, enabled = draft != previous) { Text("保存") }
                    } else {
                        if (onSave != null) TextButton(onClick = { previous = value; draft = value; editing = true }) { Text("编辑") }
                        if (onDelete != null) TextButton(onClick = {
                            // Let the extension's existing confirmation appear above the settings page.
                            opened = false
                            onDelete()
                        }) { Text("删除") }
                    }
                }
                HorizontalDivider(color = AndexOutline)
                if (editing) {
                    OutlinedTextField(draft, { draft = it }, Modifier.fillMaxWidth().weight(1f).padding(16.dp),
                        label = { Text("记忆内容") }, supportingText = { Text("保存后在下次对话中载入") })
                } else {
                    val chunks = remember(value) { value.ifBlank { "暂无记忆" }.chunked(2000) }
                    SelectionContainer(Modifier.weight(1f)) {
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                            itemsIndexed(chunks) { _, chunk ->
                                Text(chunk, style = MaterialTheme.typography.bodyMedium, color = AndexOnSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}
