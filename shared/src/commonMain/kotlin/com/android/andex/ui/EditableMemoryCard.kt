package com.android.andex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.andex.ui.theme.*

@Composable
fun EditableMemoryCard(id: String, title: String, value: String, onSave: ((String, String) -> Unit)?, onDelete: (() -> Unit)? = null) {
    var expanded by rememberSaveable(id) { mutableStateOf(false) }
    var editing by remember(id) { mutableStateOf(false) }
    var draft by remember(id) { mutableStateOf("") }
    var previous by remember(id) { mutableStateOf("") }
    LaunchedEffect(value) {
        if (editing && value != previous && value == draft) editing = false
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = AndexOnSurface)
            Text(if (expanded) "收起" else "查看", style = MaterialTheme.typography.labelMedium, color = AndexPrimary)
        }
        if (editing) {
            OutlinedTextField(draft, { draft = it }, Modifier.fillMaxWidth(),
                minLines = 5, maxLines = 12, label = { Text("记忆内容") },
                supportingText = { Text("保存后在下次对话中载入") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onSave?.invoke(draft, previous) }, enabled = draft != previous) { Text("保存") }
                TextButton(onClick = { editing = false }) { Text("取消") }
            }
        } else {
            if (expanded) {
                val chunks = remember(value) { value.ifBlank { "暂无记忆" }.chunked(2000) }
                SelectionContainer {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                        items(chunks) { chunk ->
                            Text(chunk, style = MaterialTheme.typography.bodySmall, color = AndexOnSurfaceVariant)
                        }
                    }
                }
            } else Text(value.ifBlank { "暂无记忆" }.take(240), Modifier.fillMaxWidth(), maxLines = 2,
                overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = AndexOnSurfaceVariant)
            if (expanded) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onSave != null) TextButton(onClick = {
                    previous = value; draft = value; editing = true
                }) { Text("编辑") }
                if (onDelete != null) TextButton(onClick = onDelete) { Text("删除") }
            }
        }
    }
}
