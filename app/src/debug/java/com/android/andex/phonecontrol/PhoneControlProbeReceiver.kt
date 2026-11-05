package com.android.andex.phonecontrol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
import com.android.andex.AndexApplication
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File

/** Debug-only test hook protected by DUMP (shell/system only). No release component. */
class PhoneControlProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val encoded = intent.getStringExtra("request_b64") ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val args = String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8)
                val result = withTimeout(25_000) {
                    (context.applicationContext as AndexApplication).runtime.phoneControlController.execute(args)
                }
                File(context.cacheDir, "phone-probe-result.json").writeText(
                    JSONObject(result).put("probe_id", intent.getStringExtra("probe_id")).toString(),
                )
            } catch (error: Exception) {
                File(context.cacheDir, "phone-probe-result.json").writeText(
                    JSONObject().put("ok", false).put("probe_id", intent.getStringExtra("probe_id"))
                        .put("errmsg", error.message.orEmpty()).toString(),
                )
            } finally { pending.finish() }
        }
    }
}
