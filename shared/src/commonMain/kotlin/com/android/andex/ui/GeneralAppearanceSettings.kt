package com.android.andex.ui
import com.android.andex.ui.AndexGlassAlertDialog as AlertDialog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.android.andex.data.VoiceInputSettings
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.android.andex.ui.theme.*

@Composable
fun GeneralAppearanceSettings(
    accent: String,
    background: String,
    onColors: (String, String) -> Unit,
    voice: VoiceInputSettings,
    onVoice: (VoiceInputSettings) -> Unit,
) {
    var configuring by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val dark = AndexBackground == androidx.compose.ui.graphics.Color.Black
        val selectedAccent = resolvedThemeColor(accent, dark)
        val selectedBackground = resolvedBackgroundColor(background, accent, dark)
        ThemeColorRow("主题色", "文字、图标、按钮、提示、高亮与描边", selectedAccent) { color ->
            val keepBackground = if (dark) "black" else if (ThemeAccentOptions.any { it.id == accent } && background == "auto") selectedBackground.id else background
            onColors(color.id, keepBackground)
        }
        ThemeColorRow("背景色", "页面背景 · 表面轻度区分", selectedBackground) { color ->
            onColors(selectedAccent.id, color.id)
        }
        HorizontalDivider(color = AndexOutlineSoft)
        Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { configuring = true }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("语音输入", style = MaterialTheme.typography.titleMedium, color = AndexOnSurface)
                Text(if (voice.isConfigured) "豆包输入法非官方 ASR · 默认服务" else "语音输入已关闭",
                    style = MaterialTheme.typography.bodySmall, color = AndexOnSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = AndexOnSurfaceVariant)
        }
    }
    if (configuring) VoiceInputSettingsDialog(voice, { configuring = false }) {
        onVoice(it); configuring = false
    }
}

@Composable
private fun ThemeColorRow(title: String, description: String, selected: ThemeColorOption, onSelect: (ThemeColorOption) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = AndexOnSurface, modifier = Modifier.weight(1f))
            Text(selected.label, style = MaterialTheme.typography.bodySmall, color = AndexOnSurfaceVariant)
        }
        Text(description, style = MaterialTheme.typography.bodySmall, color = AndexOnSurfaceVariant)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
        val swatchSize = ((maxWidth - 16.dp) / 9).coerceIn(28.dp, 40.dp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            // Neutrals stay visible even on narrow displays; the row can scroll for the remaining colors.
            ThemeColorOptions.sortedByDescending { it.color.luminance() }.forEach { item ->
                val active = selected.id == item.id
                Box(Modifier.size(swatchSize).clip(CircleShape).clickable { onSelect(item) }
                    .semantics(mergeDescendants = true) { contentDescription = "$title：${item.label}"; this.selected = active }
                    .padding(3.dp)
                    .border(if (active) 2.dp else 1.dp, if (active) AndexOnSurface else AndexOutline, CircleShape)
                    .padding(4.dp).clip(CircleShape).background(item.color), contentAlignment = Alignment.Center) {
                    if (active) Icon(Icons.Rounded.Check, null, tint = themeForeground(item.color), modifier = Modifier.size(15.dp))
                }
            }
        }
        }
    }
}

@Composable
private fun VoiceInputSettingsDialog(current: VoiceInputSettings, onDismiss: () -> Unit, onSave: (VoiceInputSettings) -> Unit) {
    var enabled by remember { mutableStateOf(current.enabled) }
    var testing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val client = com.android.andex.data.rememberDoubaoImeAsr()
    DisposableEffect(client) { onDispose { client.close() } }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("语音输入") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("启用语音输入", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Switch(enabled, { enabled = it })
                }
                HorizontalDivider(color = AndexOutlineSoft)
                Text("豆包输入法非官方 ASR", style = MaterialTheme.typography.titleSmall)
                Text("按住说话时持续上传音频，中间文字仅预览；松手后等待最终整句，再一次性填入草稿，不会自动发送消息。",
                    style = MaterialTheme.typography.bodySmall)
                Text("无需配置 API Key。录音直接发送到豆包输入法在线服务；设备凭据保存在本机私有存储，不长期保存音频。非官方接入，可用性和额度由服务端决定。",
                    style = MaterialTheme.typography.bodySmall, color = AndexOnSurfaceVariant)
                Text("首次使用需允许麦克风权限。单次最长 60 秒，上滑取消；唤起键盘后长按可粘贴。",
                    style = MaterialTheme.typography.bodySmall, color = AndexOnSurfaceVariant)
                TextButton(enabled = enabled && !testing, onClick = {
                    testing = true; result = null; error = null
                    scope.launch {
                        try { client.probe(); result = "连接与鉴权正常" }
                        catch (cancel: CancellationException) { throw cancel }
                        catch (failure: Exception) {
                            if (failure.message.orEmpty().contains("没有识别到完整语音"))
                                result = "连接与鉴权正常 · 静音检测无识别文本"
                            else error = failure.message ?: "检测失败，请重试"
                        } finally { testing = false }
                    }
                }) { Text(if (testing) "正在检测…" else "检测连接") }
                Text("检测仅确认连接与鉴权；实际识别请在对话中按住说话。", style = MaterialTheme.typography.bodySmall)
                result?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AndexOnSurface) }
                error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(VoiceInputSettings(enabled)) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
