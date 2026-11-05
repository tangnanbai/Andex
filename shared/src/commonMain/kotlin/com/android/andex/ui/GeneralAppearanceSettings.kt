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
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.android.andex.data.VolcAsrSettings
import com.android.andex.data.VolcAsrClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.android.andex.ui.theme.*

@Composable
fun GeneralAppearanceSettings(
    accent: String,
    background: String,
    onColors: (String, String) -> Unit,
    voice: VolcAsrSettings,
    onVoice: (VolcAsrSettings) -> Unit,
) {
    var configuring by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val dark = AndexBackground == androidx.compose.ui.graphics.Color.Black
        val selectedAccent = resolvedThemeColor(accent, dark)
        val selectedBackground = resolvedBackgroundColor(background, accent, dark)
        ThemeColorRow("主题色", "文字、图标、按钮、提示、高亮与描边", selectedAccent) { color ->
            val keepBackground = if (ThemeAccentOptions.any { it.id == accent } && background == "auto") selectedBackground.id else background
            onColors(color.id, keepBackground)
        }
        ThemeColorRow("背景色", "页面背景 · 表面轻度区分", selectedBackground) { color ->
            onColors(selectedAccent.id, color.id)
        }
        TextButton(onClick = { onColors("auto", "auto") }, modifier = Modifier.align(Alignment.End)) {
            Text("恢复默认 · 粉白 / 粉黑")
        }
        HorizontalDivider(color = AndexOutlineSoft)
        Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { configuring = true }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("语音输入", style = MaterialTheme.typography.titleMedium, color = AndexOnSurface)
                Text(if (voice.isConfigured) "火山 ASR · 已配置" else "配置火山 ASR · 按住输入框说话",
                    style = MaterialTheme.typography.bodySmall, color = AndexOnSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = AndexOnSurfaceVariant)
        }
    }
    if (configuring) VolcAsrSettingsDialog(voice, { configuring = false }) {
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
            (ThemeColorOptions.takeLast(2) + ThemeColorOptions.dropLast(2)).forEach { item ->
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
private fun VolcAsrSettingsDialog(current: VolcAsrSettings, onDismiss: () -> Unit, onSave: (VolcAsrSettings) -> Unit) {
    var apiKey by remember { mutableStateOf(current.apiKey) }
    var endpoint by remember { mutableStateOf(current.endpoint) }
    var testing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    var appId by remember { mutableStateOf(current.appId) }
    var accessKey by remember { mutableStateOf(current.accessKey) }
    var legacy by remember { mutableStateOf(current.apiKey.isBlank() && current.appId.isNotBlank()) }
    var error by remember { mutableStateOf<String?>(null) }
    fun configuration() = if (legacy) VolcAsrSettings(appId = appId.trim(), accessKey = accessKey.trim(), endpoint = endpoint.trim())
        else VolcAsrSettings(apiKey = apiKey.trim(), endpoint = endpoint.trim())
    LaunchedEffect(endpoint, apiKey, appId, accessKey, legacy) { result = null; error = null }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("火山 ASR") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("使用大模型录音文件识别极速版。松开后将录音发送至火山引擎，识别文字填入草稿，不会自动发送消息。",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(endpoint, { endpoint = it }, label = { Text("服务地址") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), enabled = !testing)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("使用旧版 App ID / Access Token", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Switch(checked = legacy, onCheckedChange = { legacy = it })
                }
                if (legacy) {
                    OutlinedTextField(appId, { appId = it }, label = { Text("App ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(accessKey, { accessKey = it }, label = { Text("Access Token") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                } else OutlinedTextField(apiKey, { apiKey = it }, label = { Text("API Key") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Text("资源 ID：volc.bigasr.auc_turbo\n首次使用需允许麦克风权限。单次最长录音 60 秒，上滑取消。",
                    style = MaterialTheme.typography.bodySmall)
                TextButton(enabled = !testing && configuration().isConfigured, onClick = {
                    val selected = configuration()
                    testing = true; result = null; error = null
                    scope.launch {
                        val client = VolcAsrClient()
                        try { result = client.testConnection(selected) }
                        catch (cancel: CancellationException) { throw cancel }
                        catch (failure: Exception) { error = failure.message ?: "检测失败，请检查网络。" }
                        finally { testing = false; client.close() }
                    }
                }) {
                    if (testing) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(if (testing) "正在检测…" else "检测连接")
                }
                Text("检测会发送一段短静音以校验地址、鉴权和资源权限。", style = MaterialTheme.typography.bodySmall)
                result?.let { Text(it, color = AndexOnSurface) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = { onSave(VolcAsrSettings()) }) { Text("清除配置并停用") }
            }
        },
        confirmButton = { TextButton(onClick = {
            val updated = configuration()
            if (updated.isConfigured) onSave(updated) else error = "请填写完整的 ASR 凭据"
        }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
