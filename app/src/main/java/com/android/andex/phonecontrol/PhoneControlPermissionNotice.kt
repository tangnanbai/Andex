package com.android.andex.phonecontrol
import com.android.andex.ui.AndexGlassAlertDialog as AlertDialog

import android.content.Intent
import android.content.ComponentName
import android.content.Context
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/** Settings describes authorization; the bound instance describes readiness. */
internal fun phoneAccessibilityEnabled(context: Context): Boolean? = runCatching {
    val component = ComponentName(context, PhoneAccessibilityService::class.java)
    val configured = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val listed = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { ComponentName.unflattenFromString(it.id) == component }
    if (listed) true
    else configured?.split(':')?.any { ComponentName.unflattenFromString(it) == component }
        ?: if (Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1) == 0) false else null
}.getOrNull()

@Composable
fun PhoneControlPermissionNotice() {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val preferences = remember(context) { PhoneControlPreferences(context) }
    var notice by remember { mutableStateOf<AccessibilityNotice?>(null) }
    LaunchedEffect(owner, preferences) {
        val policy = AccessibilityNoticePolicy()
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            policy.resume()
            notice = null
            while (true) {
                val ready = PhoneAccessibilityService.instance != null
                val enabled = phoneAccessibilityEnabled(context)
                if (ready) {
                    preferences.accessibilityPreviouslyGranted = true
                    notice = null
                } else if (!preferences.enabled || (enabled == true && notice == AccessibilityNotice.PermissionDisabled)) {
                    notice = null
                }
                policy.sample(SystemClock.elapsedRealtime(), enabled, ready,
                    preferences.enabled && preferences.accessibilityPreviouslyGranted)?.let { notice = it }
                delay(1000)
            }
        }
    }
    if (notice != null) AlertDialog(
        onDismissRequest = { notice = null },
        title = { Text(if (notice == AccessibilityNotice.PermissionDisabled) "无障碍权限已关闭" else "无障碍服务暂未连接") },
        text = { Text(if (notice == AccessibilityNotice.PermissionDisabled)
            "系统设置中 Andex 的无障碍权限已关闭，手机点击、输入和屏幕读取暂不可用。请重新开启 Andex。"
            else "Andex 的无障碍权限仍已开启，但服务超过一分钟未连接。系统可能限制了后台运行，可检查系统设置或重新开启服务。") },
        confirmButton = {
            TextButton(onClick = {
                notice = null
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }) { Text("检查设置") }
        },
        dismissButton = { TextButton(onClick = { notice = null }) { Text("稍后") } },
    )
}
