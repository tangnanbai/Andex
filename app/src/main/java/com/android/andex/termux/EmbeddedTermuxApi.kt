package com.android.andex.termux

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import com.android.andex.ui.AndexToast as Toast
import java.io.BufferedInputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * A small in-process Android API bridge. Only the listed commands are implemented;
 * installing the upstream termux-api package does not add other native capabilities.
 */
internal class EmbeddedTermuxApi(private val context: Context) {
    private val server = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
    private val token = UUID.randomUUID().toString() + UUID.randomUUID().toString()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val environment: Map<String, String> = mapOf(
        "ANDEX_TERMUX_API_URL" to "http://127.0.0.1:${server.localPort}",
        "ANDEX_TERMUX_API_TOKEN" to token,
    )

    init {
        scope.launch {
            while (!server.isClosed) {
                val socket = try { server.accept() } catch (_: Exception) { break }
                // Handle one request at a time; a local client cannot create unbounded workers.
                try { handle(socket) } catch (_: Exception) { socket.close() }
            }
        }
    }

    private suspend fun handle(socket: Socket) {
        socket.use {
            socket.soTimeout = 3000
            val input = BufferedInputStream(socket.getInputStream())
            val request = readLine(input).split(' ')
            require(request.size == 3 && request[0] == "POST") { "Invalid request." }
            val headers = mutableMapOf<String, String>()
            var headerBytes = 0
            while (true) {
                val line = readLine(input)
                if (line.isEmpty()) break
                headerBytes += line.length
                require(headerBytes <= 8192) { "Headers exceed limit." }
                val separator = line.indexOf(':')
                require(separator > 0)
                headers[line.substring(0, separator).lowercase()] = line.substring(separator + 1).trim()
            }
            val authorized = MessageDigest.isEqual(
                headers["authorization"].orEmpty().toByteArray(),
                "Bearer $token".toByteArray(),
            )
            if (!authorized) {
                reply(socket, 403, """{"ok":false,"errmsg":"Unauthorized."}""")
                return
            }
            val length = headers["content-length"]?.toIntOrNull() ?: 0
            require(length in 0..65536 && "transfer-encoding" !in headers) { "Invalid body size." }
            val body = ByteArray(length)
            var offset = 0
            while (offset < body.size) {
                val count = input.read(body, offset, body.size - offset)
                check(count > 0) { "Incomplete body." }
                offset += count
            }
            try {
                reply(socket, 200, call(request[1].removePrefix("/"), body.toString(Charsets.UTF_8)))
            } catch (error: Exception) {
                reply(socket, 400, JSONObject().put("ok", false).put("errmsg", error.message.orEmpty()).toString())
            }
        }
    }

    private suspend fun call(method: String, body: String): String = withContext(Dispatchers.Main.immediate) {
        when (method) {
            "android-am" -> {
                val spec = parseEmbeddedAndroidIntent(body.split('\u0000').dropLastWhile(String::isEmpty))
                val intent = if (spec.action == null && spec.data == null && spec.component == null && spec.packageName != null) {
                    context.packageManager.getLaunchIntentForPackage(spec.packageName)
                        ?: error("No launcher activity for ${spec.packageName}.")
                } else {
                    Intent(spec.action ?: Intent.ACTION_VIEW).apply {
                        if (spec.data != null && spec.mimeType != null) setDataAndType(Uri.parse(spec.data), spec.mimeType)
                        else if (spec.data != null) data = Uri.parse(spec.data)
                        else if (spec.mimeType != null) type = spec.mimeType
                        spec.packageName?.let { setPackage(it) }
                        spec.component?.let {
                            component = android.content.ComponentName.unflattenFromString(it)
                                ?: error("Invalid component.")
                        }
                        spec.stringExtras.forEach { (key, value) -> putExtra(key, value) }
                    }
                }
                val service = com.android.andex.phonecontrol.PhoneAccessibilityService.instance
                (service ?: context).startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                val target = intent.component?.packageName ?: intent.`package`
                val verified = if (service != null && target != null) service.awaitPackage(target) else null
                JSONObject().put("ok", true).put("calling_package", context.packageName)
                    .put("uid", android.os.Process.myUid()).put("foreground_verified", verified ?: false)
                    .apply {
                        if (verified == false) put("ok", false).put("errmsg", "Target did not stay in foreground.")
                    }.toString()
            }
            "phone-control" -> {
                val runtime = (context.applicationContext as com.android.andex.AndexApplication).runtime
                runtime.phoneControlController.execute(body)
            }
            "launch-app" -> {
                val runtime = (context.applicationContext as com.android.andex.AndexApplication).runtime
                runtime.phoneControlController.execute(
                    JSONObject().put("backend", "termux").put("action", "launch")
                        .put("package", body.trim()).toString(),
                )
            }
            "battery-status" -> {
                val battery = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    ?: error("Battery status is unavailable.")
                val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                JSONObject().put("percentage", level * 100 / scale)
                    .put("temperature", battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0)
                    .put("status", when (battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                        BatteryManager.BATTERY_STATUS_CHARGING -> "CHARGING"
                        BatteryManager.BATTERY_STATUS_DISCHARGING -> "DISCHARGING"
                        BatteryManager.BATTERY_STATUS_FULL -> "FULL"
                        else -> "UNKNOWN"
                    }).toString()
            }
            "clipboard-get" -> {
                check(com.android.andex.phonecontrol.PhoneControlPreferences(context).enabled) {
                    "Enable AI phone control in Andex before exposing clipboard content to the shell."
                }
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                check(clipboard.hasPrimaryClip()) { "Clipboard is empty or unavailable. Android may require Andex to be in the foreground." }
                clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
            }
            "clipboard-set" -> {
                check(com.android.andex.phonecontrol.PhoneControlPreferences(context).enabled) {
                    "Enable AI phone control in Andex before changing the clipboard from the shell."
                }
                context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Termux", body))
                """{"ok":true}"""
            }
            "toast" -> {
                Toast.makeText(context, body.take(2000), Toast.LENGTH_SHORT).show()
                """{"ok":true}"""
            }
            "vibrate" -> {
                val duration = body.trim().ifEmpty { "1000" }.toLong()
                require(duration in 1..5000) { "Vibration duration must be in 1..5000 ms." }
                context.getSystemService(Vibrator::class.java).vibrate(
                    VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE),
                )
                """{"ok":true}"""
            }
            "setup-storage" -> {
                if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
                    context.startActivity(Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } else if (Build.VERSION.SDK_INT < 30 &&
                    context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                ) {
                    context.startActivity(Intent(
                        context, com.android.andex.phonecontrol.PhoneControlActivity::class.java,
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                """{"ok":true}"""
            }
            else -> error("Unsupported embedded Termux API: $method.")
        }
    }

    private fun reply(socket: Socket, code: Int, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $code ${if (code == 200) "OK" else "Error"}\r\n" +
            "Content-Type: text/plain; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
        socket.getOutputStream().apply {
            write(header.toByteArray(Charsets.US_ASCII))
            write(bytes)
            flush()
        }
    }

    private fun readLine(input: BufferedInputStream): String {
        val output = java.io.ByteArrayOutputStream()
        while (output.size() < 4096) {
            val byte = input.read()
            check(byte >= 0) { "Incomplete HTTP header." }
            if (byte == 10) return output.toString("US-ASCII").removeSuffix("\r")
            output.write(byte)
        }
        error("HTTP header line exceeds limit.")
    }
}
