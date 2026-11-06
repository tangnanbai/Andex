package com.android.andex.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.andex.data.*
import com.android.andex.ui.theme.*

@Composable
fun ModelMentionSuggestions(options: List<ProviderModelOption>, query: ModelMentionQuery?, onSelect: (ProviderModelOption) -> Unit) {
    if (query == null) return
    val groups = options.filter { it.modelId.contains(query.query, true) || it.providerName.contains(query.query, true) }
        .groupBy { it.providerConfigId }
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 260.dp)
        .andexGlass(RoundedCornerShape(20.dp), popup = true), contentPadding = PaddingValues(vertical = 8.dp)) {
        if (groups.isEmpty()) item { Text("没有匹配的模型", Modifier.padding(16.dp), color = AndexOnSurfaceVariant) }
        groups.forEach { (id, models) ->
            item("group-$id") { Text(models.first().providerName, Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall, color = AndexOnSurfaceVariant) }
            items(models, key = { it.key }) { option ->
                Row(Modifier.fillMaxWidth().clickable { onSelect(option) }.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(option.modelId, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                        color = AndexOnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (isImageGenerationModel(option.modelId)) "生图" else "文本",
                        style = MaterialTheme.typography.labelSmall, color = AndexPrimary)
                }
            }
        }
    }
}
