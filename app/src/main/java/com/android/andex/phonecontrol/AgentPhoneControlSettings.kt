package com.android.andex.phonecontrol

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.android.andex.AndexApplication
import com.android.andex.ui.SettingsCardGroup
import com.android.andex.ui.SettingsActionButton
import com.android.andex.ui.SettingsSubtleActionButton
import com.android.andex.ui.SettingsToggleRow
import com.android.andex.ui.theme.AndexOnSurfaceVariant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Direct settings controls, independent of virtual-display authorization. */
@Composable
fun AgentPhoneControlSettings() {
    val context = LocalContext.current
    val controller = (context.applicationContext as AndexApplication).runtime.phoneControlController
    val preferences = controller.preferences
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(JSONObject()) }
    var enabled by remember { mutableStateOf(preferences.enabled) }
    var pairingEndpoint by rememberSaveable { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    var endpoint by rememberSaveable { mutableStateOf(preferences.adbEndpoint) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }
    LaunchedEffect(controller) {
        while (true) { status = controller.status(); delay(2000) }
    }
    fun operation(block: suspend () -> JSONObject) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val response = block()
                result = if (response.optBoolean("ok", false)) "操作成功" else
                    response.optString("error").ifBlank { response.optString("stderr").ifBlank { "操作已结束，请检查连接状态" } }
                status = controller.status()
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { result = error.message.orEmpty() }
            finally { busy = false }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsCardGroup {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsToggleRow("手机控制", "允许 Agent 使用已授权的 ADB 和无障碍操控手机",
                    enabled, { enabled = it; preferences.enabled = it })
                HorizontalDivider()
                Text("无障碍", style = MaterialTheme.typography.titleMedium)
                Text(if (status.optBoolean("accessibility_ready")) "已授权 · 可以点击、滑动和读取屏幕" else if (status.optBoolean("accessibility_authorized")) "权限已开启 · 正在等待系统连接服务" else "权限已关闭 · 请在系统设置中启用 Andex",
                    style = MaterialTheme.typography.bodySmall, color = AndexOnSurfaceVariant)
                SettingsActionButton("无障碍授权", {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }, modifier = Modifier.fillMaxWidth())
            }
        }
        SettingsCardGroup {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("ADB", style = MaterialTheme.typography.titleMedium)
                Text(if (status.optBoolean("adb_ready")) "已连接 · 可以执行 shell 和屏幕操作" else "未连接",
                    style = MaterialTheme.typography.bodySmall, color = AndexOnSurfaceVariant)
                SettingsSubtleActionButton("无线调试设置", {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(pairingEndpoint, { pairingEndpoint = it }, Modifier.fillMaxWidth(),
                    label = { Text("配对地址（IP:端口）") }, singleLine = true, enabled = !busy)
                OutlinedTextField(pairingCode, { pairingCode = it.filter(Char::isDigit).take(6) },
                    Modifier.fillMaxWidth(), label = { Text("六位配对码") }, singleLine = true, enabled = !busy,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                SettingsActionButton("配对", {
                    val code = pairingCode; pairingCode = ""
                    operation { controller.pair(pairingEndpoint.trim(), code) }
                }, modifier = Modifier.fillMaxWidth(), enabled = !busy && pairingCode.length == 6 && pairingEndpoint.isNotBlank())
                OutlinedTextField(endpoint, { endpoint = it }, Modifier.fillMaxWidth(),
                    label = { Text("连接地址（IP:端口）") }, singleLine = true, enabled = !busy)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsActionButton("连接", { operation { controller.connect(endpoint.trim()) } },
                        modifier = Modifier.weight(1f), enabled = !busy && endpoint.isNotBlank())
                    SettingsSubtleActionButton("断开", { operation { controller.disconnect() } },
                        modifier = Modifier.weight(1f), enabled = !busy && preferences.adbConnected)
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (result.isNotBlank()) Text(result, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
