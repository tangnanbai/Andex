package com.android.andex.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.andex.ui.theme.*
import kotlin.math.roundToInt

data class ContextUsageDetails(val used: Long? = null, val limit: Long? = null, val model: String = "") {
    val fraction: Float? get() = used?.takeIf { it >= 0 }?.let { count ->
        limit?.takeIf { it > 0 }?.let { (count.toFloat() / it).coerceIn(0f, 1f) }
    }
    val label: String get() = if (used != null && limit != null && fraction != null) {
        "${contextTokenLabel(used)} / ${contextTokenLabel(limit)} (${(fraction!! * 100).roundToInt()}%)"
    } else "用量暂不可用"
}

fun contextTokenLabel(value: Long): String = value.toString().reversed().chunked(3).joinToString(",").reversed()

/** Compact ring with an anchored, bounded usage card, shared by both composers. */
@Composable
fun ContextUsageIndicator(usage: ContextUsageDetails) {
    var expanded by remember { mutableStateOf(false) }
    val accent = AndexOnSurface
    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().clickable { expanded = !expanded }
            .semantics { contentDescription = "上下文用量：${usage.label}" },
            contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(22.dp)) {
                val stroke = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round)
                drawArc(accent.copy(alpha = .16f), -90f, 360f, false, style = stroke)
                usage.fraction?.let { drawArc(accent, -90f, it * 360f, false, style = stroke) }
            }
        }
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.width(288.dp)
            .andexGlass(RoundedCornerShape(16.dp), popup = true),
            shape = RoundedCornerShape(16.dp), shadowElevation = 0.dp, tonalElevation = 0.dp,
            containerColor = androidx.compose.ui.graphics.Color.Transparent) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("上下文用量", style = MaterialTheme.typography.bodyMedium, color = accent)
                LinearProgressIndicator(progress = { usage.fraction ?: 0f },
                    modifier = Modifier.fillMaxWidth().height(5.dp), color = accent,
                    trackColor = accent.copy(alpha = .12f), drawStopIndicator = {})
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(usage.label, color = AndexOnSurfaceVariant, style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, softWrap = false)
                    Text(usage.model, modifier = Modifier.weight(1f), color = AndexOnSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
