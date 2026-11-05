package com.android.andex.overlay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.andex.AndexApplication
import com.android.andex.data.AppSettings
import com.android.andex.ui.*
import com.android.andex.ui.theme.AndexTheme
import com.android.andex.ui.theme.AndexOnSurfaceVariant
import kotlinx.coroutines.delay

class PillSettingsActivity : AppCompatActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        enableEdgeToEdge()
        val runtime = (application as AndexApplication).runtime
        val preferences = getSharedPreferences("pill", MODE_PRIVATE)
        setContent {
            val settings by runtime.settingsRepository.settings.collectAsState(initial = AppSettings())
            var allowed by remember { mutableStateOf(Settings.canDrawOverlays(this)) }
            var enabled by remember { mutableStateOf(preferences.getBoolean("enabled", true)) }
            LaunchedEffect(Unit) {
                while (true) { allowed = Settings.canDrawOverlays(this@PillSettingsActivity); delay(1000) }
            }
            AndexTheme(themeMode = settings.themeMode, themeAccent = settings.themeAccent, themeBackground = settings.themeBackground, language = settings.language) {
                SubPageScaffold(title = "后台进度", onBack = ::finish) {
                    SettingsCardGroup {
                        Column(Modifier.padding(16.dp)) {
                            SettingsToggleRow(title = "显示后台进度",
                                subtitle = "仅在 Agent 执行任务且 Andex 不在前台时显示",
                                checked = enabled, onCheckedChange = { value ->
                                    enabled = value
                                    preferences.edit().putBoolean("enabled", value).apply()
                                    if (value && !allowed) openOverlayPermission()
                                    if (value && Build.VERSION.SDK_INT >= 33 &&
                                        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                                        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                                })
                        }
                        CardDivider()
                        SettingsNavRow(icon = Icons.Rounded.Layers, title = "悬浮窗权限",
                            subtitle = if (allowed) "已授权" else "未授权 · 点击前往系统设置",
                            onClick = ::openOverlayPermission)
                    }
                    Spacer(Modifier.height(16.dp))
                    SettingsCardGroup {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("显示与交互", style = MaterialTheme.typography.titleMedium)
                            Text("运行中⏳ · 搜索🔎 · 运行命令⚙️ · 完成✅ · 错误❌",
                                style = MaterialTheme.typography.bodyMedium)
                            Text("拖动调整位置，点击展开任务详情，再点详情回到对应会话。完成和错误会显示 8 秒后收起。",
                                style = MaterialTheme.typography.bodySmall, color = AndexOnSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    SettingsCardGroup {
                        SettingsNavRow(icon = Icons.Rounded.BatteryFull, title = "电池与后台运行",
                            subtitle = "如果进度被系统关闭，请允许 Andex 在后台运行",
                            onClick = {
                                runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                                    .onFailure { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
                            })
                    }
                }
            }
        }
    }

    private fun openOverlayPermission() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }
}
