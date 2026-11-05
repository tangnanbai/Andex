package com.android.andex.phonecontrol

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import com.android.andex.termux.TermuxBashTool
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

class PhoneControlController(
    context: Context,
    private val bashTool: TermuxBashTool,
) {
    private val context = context.applicationContext
    val preferences = PhoneControlPreferences(this.context)
    private val actionMutex = Mutex()
    private val adbMutex = Mutex()
    val nativeTools = com.android.andex.runtime.BundledNativeTools(this.context)

    suspend fun installAdb(): JSONObject = withContext(Dispatchers.IO) {
        adbMutex.withLock {
            nativeTools.execute("adb", listOf("version"))
        }
    }

    suspend fun pair(endpoint: String, code: String): JSONObject = withContext(Dispatchers.IO) {
        adbMutex.withLock {
            val target = validateAdbEndpoint(endpoint)
            require(Regex("""^\d{6}$""").matches(code)) { "Pairing code must contain six digits." }
            // Pairing codes never go through TermuxBashTool's command/analytics logging.
            val result = embeddedAdb(listOf("pair", target, code), 30_000)
            result.remove("command")
            result
        }
    }

    suspend fun connect(endpoint: String): JSONObject = withContext(Dispatchers.IO) {
        adbMutex.withLock {
            val target = validateAdbEndpoint(endpoint)
            preferences.adbConnected = false
            val connection = embeddedAdb(listOf("connect", target), 15_000)
            if (!connection.optBoolean("ok")) return@withLock connection
            val probe = embeddedAdb(listOf("-s", target, "get-state"), 10_000)
            val connected = probe.optBoolean("ok") && probe.optString("stdout").trim() == "device"
            if (connected) {
                preferences.adbEndpoint = target
                preferences.adbConnected = true
            }
            probe.put("ok", connected).put("endpoint", target)
        }
    }

    suspend fun disconnect(): JSONObject = withContext(Dispatchers.IO) {
        adbMutex.withLock {
            val target = preferences.adbEndpoint
            preferences.adbConnected = false
            if (target.isBlank()) JSONObject().put("ok", true)
            else embeddedAdb(listOf("disconnect", validateAdbEndpoint(target)), 10_000)
        }
    }

    suspend fun status(): JSONObject = withContext(Dispatchers.IO) {
        val adbReady = adbMutex.withLock {
            val ready = if (!preferences.adbConnected || preferences.adbEndpoint.isBlank()) false
            else try {
                val probe = embeddedAdb(listOf("-s", validateAdbEndpoint(preferences.adbEndpoint), "get-state"), 5000)
                probe.optBoolean("ok") && probe.optString("stdout").trim() == "device"
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
            if (!ready) preferences.adbConnected = false
            ready
        }
        JSONObject().put("ok", true).put("enabled", preferences.enabled)
            .put("enhanced_control_enabled", preferences.enabled)
            .put("default_backend", "termux").put("termux_ready", bashTool.embeddedRuntime.isInstalled)
            .put("adb_ready", adbReady).put("adb_endpoint", preferences.adbEndpoint)
            .put("accessibility_ready", PhoneAccessibilityService.instance != null)
            .put("app_package", context.packageName).put("app_uid", android.os.Process.myUid())
            .put("termux_identity", "ordinary Andex app UID; launch uses native Android bridge")
    }

    suspend fun execute(argumentsJson: String): String = actionMutex.withLock {
        try {
            val arguments = JSONObject(argumentsJson)
            if (arguments.optString("action") == "batch") {
                val actions = arguments.optJSONArray("actions") ?: error("batch requires an actions array.")
                require(actions.length() in 1..20) { "batch requires 1..20 actions." }
                val results = org.json.JSONArray()
                var ok = true
                val start = android.os.SystemClock.uptimeMillis()
                for (index in 0 until actions.length()) {
                    check(android.os.SystemClock.uptimeMillis() - start < 30_000) { "Batch exceeded 30 seconds." }
                    val item = JSONObject(actions.getJSONObject(index).toString())
                    if (!item.has("include_snapshot")) item.put("include_snapshot", index == actions.length() - 1)
                    require(item.optString("action") !in listOf("batch", "shell")) {
                        "batch cannot contain batch or shell actions; call shell separately."
                    }
                    if (!item.has("backend") && arguments.has("backend")) item.put("backend", arguments.get("backend"))
                    if (!item.has("expected_package") && arguments.has("expected_package")) {
                        item.put("expected_package", arguments.get("expected_package"))
                    }
                    val result = try { executeOne(item) } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        JSONObject().put("ok", false).put("errmsg", error.message.orEmpty())
                    }
                    results.put(result)
                    if (!result.optBoolean("ok")) {
                        ok = false
                        if (arguments.optBoolean("stop_on_error", true)) break
                    }
                }
                return@withLock JSONObject().put("ok", ok).put("action", "batch").put("results", results).toString()
            }
            executeOne(arguments).toString()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            JSONObject().put("ok", false).put("errmsg", error.message.orEmpty()).toString()
        }
    }

    private suspend fun executeOne(source: JSONObject): JSONObject {
            val arguments = canonicalPhoneArguments(source)
            val action = arguments.optString("action")
            if (action == "status") return status()
            val requestedBackend = arguments.optString("backend", "termux").ifBlank { "termux" }.lowercase()
            val backend = if (requestedBackend == "termux" && action in ScreenActions) {
                check(preferences.enabled) { "Enable phone control in Settings > Agent Mode first." }
                when {
                    PhoneAccessibilityService.instance != null -> "accessibility"
                    preferences.adbConnected && action !in listOf("scroll", "find_text", "scroll_to_text", "click") -> "adb"
                    else -> error("Screen control needs authorized accessibility or connected ADB; native launch and shell remain available.")
                }
            } else requestedBackend
            if (backend != "termux") {
                check(preferences.enabled) { "Enable phone control in Settings > Agent Mode first." }
            }
            val result = when (backend) {
                "termux" -> executeTermux(arguments)
                "adb" -> adbMutex.withLock { executeAdb(arguments) }
                "accessibility" -> {
                    val service = PhoneAccessibilityService.instance
                        ?: error("Enable Andex's accessibility service in Android Settings > Accessibility.")
                    if (action == "launch") launch(arguments) else service.execute(arguments)
                }
                else -> error("backend must be termux, adb or accessibility.")
            }
            return result.put("backend", backend).put("requested_backend", requestedBackend).put("action", action)
    }

    private suspend fun executeTermux(arguments: JSONObject): JSONObject = when (arguments.optString("action")) {
        "shell" -> {
            val command = arguments.optString("command")
            require(command.isNotBlank()) { "Missing command." }
            check(preferences.embeddedTermux) { "The phone control Termux backend requires embedded Termux." }
            JSONObject(bashTool.execute(JSONObject().put("command", command).toString()))
        }
        "launch" -> launch(arguments)
        "battery" -> withContext(Dispatchers.IO) {
            val battery = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                ?: error("Battery status is unavailable.")
            JSONObject().put("ok", true)
                .put("level", battery.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1))
                .put("scale", battery.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100))
                .put("status", battery.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1))
                .put("plugged", battery.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0))
        }
        else -> error("Termux runs with ordinary app permissions. Use adb or accessibility for screen input. Supported Termux actions: shell, launch, battery.")
    }

    private suspend fun launch(arguments: JSONObject): JSONObject = withContext(Dispatchers.Main) {
        val packageName = arguments.optString("package")
        require(Regex("""^[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+)+$""").matches(packageName)) { "Invalid package name." }
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: error("No launcher activity found for $packageName.")
        // An enabled AccessibilityService can launch from the background. It never
        // starts Andex again when the tool completes.
        val service = PhoneAccessibilityService.instance
        (service ?: context).startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val foreground = service?.awaitPackage(packageName)
        JSONObject().put("ok", foreground != false).put("package", packageName)
            .put("launched", true).put("foreground_verified", foreground ?: false).apply {
                if (service != null) put("foreground_package", service.foregroundPackage())
                if (foreground == false) put("errmsg", "Activity launch requested but target did not stay in foreground.")
                if (foreground == true && arguments.optBoolean("include_snapshot", true)) {
                    put("snapshot", service?.execute(JSONObject().put("action", "snapshot")))
                }
            }
    }

    private suspend fun executeAdb(arguments: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        check(preferences.adbConnected) { "Connect ADB in Phone control first." }
        val endpoint = validateAdbEndpoint(preferences.adbEndpoint)
        val adb = listOf("-s", endpoint)
        val probe = embeddedAdb(adb + "get-state", 5000)
        if (!probe.optBoolean("ok") || probe.optString("stdout").trim() != "device") {
            preferences.adbConnected = false
            error("ADB is disconnected. Wireless debugging ports can change; reconnect in Phone control.")
        }
        val action = arguments.optString("action")
        val shell = when (action) {
            "shell" -> arguments.optString("command").also { require(it.isNotBlank()) { "Missing command." } }
            "key" -> "input keyevent " + when (arguments.optString("key").lowercase()) {
                "back" -> "4"
                "home" -> "3"
                "recents" -> "187"
                "notifications" -> "83"
                "enter" -> "66"
                else -> error("Unsupported key.")
            }
            "text" -> {
                require(arguments.has("text")) { "Missing text." }
                val text = arguments.getString("text")
                require(text.all { it.code in 32..126 } && '%' !in text) {
                    "ADB input text supports ASCII without percent signs; use accessibility for Unicode text."
                }
                "input text ${shellQuote(text.replace(" ", "%s"))}"
            }
            "launch" -> {
                val packageName = arguments.optString("package")
                require(Regex("""^[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+)+$""").matches(packageName)) { "Invalid package name." }
                "monkey -p ${shellQuote(packageName)} -c android.intent.category.LAUNCHER 1"
            }
            "tap", "swipe" -> {
                val size = embeddedAdb(adb + listOf("shell", "wm size"), 5000)
                check(size.optBoolean("ok")) { "Cannot read ADB display size." }
                val dimensions = Regex("""(?:Physical|Override) size:\s*(\d+)x(\d+)""")
                    .findAll(size.optString("stdout")).lastOrNull() ?: error("Cannot parse ADB display size.")
                var width = dimensions.groupValues[1].toInt()
                var height = dimensions.groupValues[2].toInt()
                // wm size is natural orientation; input coordinates follow current rotation.
                val display = embeddedAdb(adb + listOf("shell", "dumpsys input"), 5000)
                val rotation = Regex("""orientation=(\d)""").find(display.optString("stdout"))?.groupValues?.get(1)?.toInt()
                if (rotation == 1 || rotation == 3) {
                    val swap = width
                    width = height
                    height = swap
                }
                fun x(key: String) = phoneCoordinate(arguments, key, width)
                fun y(key: String) = phoneCoordinate(arguments, key, height)
                if (action == "tap") "input tap ${x("x")} ${y("y")}"
                else "input swipe ${x("x1")} ${y("y1")} ${x("x2")} ${y("y2")} ${arguments.optInt("duration_ms", 400).coerceIn(50, 5000)}"
            }
            "snapshot" -> {
                val remote = "/data/local/tmp/andex-ui-${UUID.randomUUID()}.xml"
                "uiautomator dump ${shellQuote(remote)} >/dev/null && cat ${shellQuote(remote)}; rc=\$?; rm -f ${shellQuote(remote)}; exit \$rc"
            }
            "screenshot" -> return@withContext adbScreenshot(adb)
            else -> error("Unsupported ADB action.")
        }
        embeddedAdb(adb + listOf("shell", shell), 30_000)
    }

    private suspend fun adbScreenshot(adb: List<String>): JSONObject {
        val name = ".andex-screen-${UUID.randomUUID()}.png"
        val host = File(context.cacheDir, name)
        try {
            val result = nativeTools.execute("adb", adb + listOf("exec-out", "screencap", "-p"), 20_000, host)
            check(result.optBoolean("ok")) { result.optString("stderr").ifBlank { "ADB screenshot failed." } }
            require(host.length() in 1..20L * 1024 * 1024) { "Screenshot size is invalid." }
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(host.path, options)
            require(options.outWidth in 1..8192 && options.outHeight in 1..8192 &&
                options.outWidth.toLong() * options.outHeight <= 32_000_000
            ) { "Screenshot dimensions are invalid." }
            val width = options.outWidth
            val height = options.outHeight
            options.inJustDecodeBounds = false
            options.inSampleSize = 1
            while (maxOf(width, height) / options.inSampleSize > 2560) options.inSampleSize *= 2
            return screenshotJson(BitmapFactory.decodeFile(host.path, options) ?: error("Screenshot is unreadable."))
                .put("width", width).put("height", height)
        } finally {
            host.delete()
        }
    }

    private suspend fun embeddedAdb(arguments: List<String>, timeout: Long): JSONObject =
        nativeTools.execute("adb", arguments, timeout)

    companion object {
        private val ScreenActions = setOf(
            "snapshot", "screenshot", "tap", "swipe", "click", "text", "key", "scroll", "find_text", "scroll_to_text",
        )
        fun toolDefinition(): JSONObject = JSONObject().apply {
            put("name", "phone_control")
            put("description", "Control the user's real Android phone. Default backend is termux (ordinary app privileges, available out of the box). " +
                "First call status. Authorized accessibility and connected ADB work in any chat, without root/Shizuku or isolated Agent Mode. " +
                "Use termux action=launch for native app launch with Andex's identity. Default termux screen actions delegate to authorized accessibility, then connected ADB. " +
                "Select adb or accessibility explicitly to choose a channel; results report the actual backend. " +
                "Snapshot bounds and centers use normalized 0..1000; tap uses x,y and swipe uses x1,y1,x2,y2 in the same space. " +
                "Explicit coordinate_space=pixels accepts physical display pixels. Accessibility automatically waits for UI changes. " +
                "Use scroll, find_text or scroll_to_text to locate text. Batch executes up to 20 actions in one call. " +
                "Mutating accessibility actions return a fresh snapshot by default. Reuse it to avoid extra round trips. " +
                "Text input alone never completes search: use key=enter or click Search, inspect results and open relevant items. " +
                "Only report task completion after verifying the requested final result, not merely ok=true on an input action. " +
                "Use expected_package to prevent actions on the wrong foreground app. Snapshots are compact by default. " +
                "Use accessibility text for Unicode. Password nodes are redacted. Pairing and permission grants are user-only.")
            put("execution_mode", "sequential")
            put("parameters", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    fun string(name: String, description: String) {
                        put(name, JSONObject().put("type", "string").put("description", description))
                    }
                    string("backend", "termux (default), adb, or accessibility.")
                    string("action", "status, shell, launch, battery, snapshot, screenshot, tap, swipe, click, text, key, scroll, find_text, scroll_to_text, batch.")
                    string("command", "For shell: command to execute.")
                    string("package", "For launch: Android package name.")
                    string("text", "For text: replacement text.")
                    string("node_id", "Observed node handle from snapshot/find_text. Re-observe after navigation or scrolling; stale handles are rejected.")
                    string("view_id", "For accessibility click/text: unique view_id from snapshot.")
                    string("selector_text", "For accessibility click/text: unique exact node text or description.")
                    string("coordinate_space", "normalized_0_1000 (default) or pixels. Snapshot bounds are always normalized.")
                    string("expected_package", "Before accessibility actions, require this foreground package; stop if it changes.")
                    string("direction", "For accessibility scroll/scroll_to_text: down (default), up, left, right.")
                    string("key", "back, home, recents, notifications, enter (submit/search the focused field; Android 11+).")
                    listOf("x", "y", "x1", "y1", "x2", "y2").forEach {
                        put(it, JSONObject().put("type", "number").put("minimum", 0))
                    }
                    put("duration_ms", JSONObject().put("type", "integer").put("minimum", 50).put("maximum", 5000))
                    put("max_scrolls", JSONObject().put("type", "integer").put("minimum", 0).put("maximum", 30))
                    put("max_nodes", JSONObject().put("type", "integer").put("minimum", 1).put("maximum", 500))
                    listOf("detailed", "interactive_only", "include_snapshot", "exact", "stop_on_error").forEach {
                        put(it, JSONObject().put("type", "boolean"))
                    }
                })
                val itemProperties = JSONObject(getJSONObject("properties").toString())
                itemProperties.getJSONObject("action").put("description", "One action; batch and shell cannot be nested.")
                getJSONObject("properties").put("actions", JSONObject().put("type", "array")
                    .put("minItems", 1).put("maxItems", 20).put("items", JSONObject()
                        .put("type", "object").put("properties", itemProperties)
                        .put("required", org.json.JSONArray().put("action")).put("additionalProperties", false)))
                put("required", org.json.JSONArray().put("action"))
                put("additionalProperties", false)
            })
        }
    }
}
