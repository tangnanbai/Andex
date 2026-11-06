package com.android.andex.phonecontrol

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

class PhoneAccessibilityService : AccessibilityService() {
    private val persistenceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.Main.immediate)
    private var persistenceJob: kotlinx.coroutines.Job? = null
    private var foregroundActive = false
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        PhoneControlPreferences(this).accessibilityPreviouslyGranted = true
        // Accessibility authorization belongs to the system. Keep the authorized service
        // alive when the UI task is swiped away; never disable/re-enable it on app exit.
        persistenceJob?.cancel()
        persistenceJob = persistenceScope.launch {
            while (true) {
                runCatching { updatePersistence() }
                delay(3000)
            }
        }
    }

    private fun updatePersistence() {
        if (!PhoneControlPreferences(this).enabled) {
            if (foregroundActive) stopForeground(STOP_FOREGROUND_REMOVE)
            foregroundActive = false
            return
        }
        if (foregroundActive) return
        val channel = "andex_accessibility"
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(android.app.NotificationManager::class.java).createNotificationChannel(
                android.app.NotificationChannel(channel, "手机控制连接", android.app.NotificationManager.IMPORTANCE_LOW),
            )
        }
        val open = android.app.PendingIntent.getActivity(this, 0,
            android.content.Intent(this, com.android.andex.MainActivity::class.java),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        val notification = androidx.core.app.NotificationCompat.Builder(this, channel)
            .setSmallIcon(com.android.andex.R.drawable.ic_notification_small)
            .setContentTitle("Andex · 手机控制已就绪")
            .setContentText("保持已授权的无障碍连接；可在 Agent 模式关闭手机控制。")
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).build()
        androidx.core.app.ServiceCompat.startForeground(this, 1003, notification,
            if (Build.VERSION.SDK_INT >= 34) android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
        foregroundActive = true
    }

    private var lastWindowEvent = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event != null) lastWindowEvent = SystemClock.uptimeMillis()
    }
    override fun onInterrupt() = Unit
    override fun onUnbind(intent: android.content.Intent?): Boolean {
        persistenceJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        foregroundActive = false
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        persistenceScope.cancel()
        if (instance === this) instance = null
        super.onDestroy()
    }

    suspend fun execute(source: JSONObject): JSONObject = withContext(Dispatchers.Main.immediate) {
        val arguments = canonicalPhoneArguments(source)
        check(PhoneControlPreferences(this@PhoneAccessibilityService).enabled) { "AI phone control is disabled." }
        val action = arguments.optString("action")
        if (action in listOf("snapshot", "find_text")) awaitStable(minimumMillis = 0)
        val expected = arguments.optString("expected_package")
        if (expected.isNotBlank()) {
            val actual = foregroundPackage()
            check(actual == expected) { "Foreground package is '$actual', expected '$expected'; action cancelled." }
        }
        val startedAt = SystemClock.uptimeMillis()
        val result = when (action) {
            "snapshot" -> snapshot(arguments)
            "find_text" -> findText(arguments)
            "scroll" -> scroll(arguments)
            "scroll_to_text" -> scrollToText(arguments)
            "tap", "swipe" -> {
                val bounds = displayBounds()
                val path = Path()
                val swipe = arguments.optString("action") == "swipe"
                val x = phoneCoordinate(arguments, if (swipe) "x1" else "x", bounds.width())
                val y = phoneCoordinate(arguments, if (swipe) "y1" else "y", bounds.height())
                path.moveTo(x.toFloat(), y.toFloat())
                if (swipe) {
                    path.lineTo(
                        phoneCoordinate(arguments, "x2", bounds.width()).toFloat(),
                        phoneCoordinate(arguments, "y2", bounds.height()).toFloat(),
                    )
                }
                val duration = if (swipe) arguments.optLong("duration_ms", 400).coerceIn(50, 5000) else 80
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, duration)).build()
                val accepted = withTimeout(7000) {
                    suspendCancellableCoroutine { continuation ->
                        val callback = object : GestureResultCallback() {
                            override fun onCompleted(gestureDescription: GestureDescription?) {
                                if (continuation.isActive) continuation.resume(true)
                            }
                            override fun onCancelled(gestureDescription: GestureDescription?) {
                                if (continuation.isActive) continuation.resume(false)
                            }
                        }
                        if (!dispatchGesture(gesture, callback, null) && continuation.isActive) {
                            continuation.resume(false)
                        }
                    }
                }
                JSONObject().put("ok", accepted)
            }
            "click", "text" -> actOnNode(arguments)
            "key" -> {
                if (arguments.optString("key").lowercase() in listOf("enter", "search", "submit")) {
                    return@withContext submitInput(arguments)
                }
                val action = when (arguments.optString("key").lowercase()) {
                    "back" -> GLOBAL_ACTION_BACK
                    "home" -> GLOBAL_ACTION_HOME
                    "recents" -> GLOBAL_ACTION_RECENTS
                    "notifications" -> GLOBAL_ACTION_NOTIFICATIONS
                    else -> error("Accessibility key must be back, home, recents, notifications or enter.")
                }
                JSONObject().put("ok", performGlobalAction(action))
            }
            "screenshot" -> {
                check(Build.VERSION.SDK_INT >= 30) { "Accessibility screenshots require Android 11 or newer." }
                withTimeout(6000) {
                    suspendCancellableCoroutine { continuation ->
                        takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
                            override fun onSuccess(result: ScreenshotResult) {
                                try {
                                    val bitmap = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
                                        ?: error("Unable to read screenshot.")
                                    val software = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                                    bitmap.recycle()
                                    if (continuation.isActive) continuation.resume(screenshotJson(software))
                                    else software.recycle()
                                } catch (error: Exception) {
                                    if (continuation.isActive) continuation.resumeWithException(error)
                                } finally {
                                    result.hardwareBuffer.close()
                                }
                            }
                            override fun onFailure(errorCode: Int) {
                                if (continuation.isActive) continuation.resumeWithException(
                                    IllegalStateException("Android denied the screenshot ($errorCode)."),
                                )
                            }
                        })
                    }
                }
            }
            else -> error("Unsupported accessibility action.")
        }
        if (action in listOf("tap", "swipe", "click", "text", "key", "scroll") && result.optBoolean("ok")) {
            result.put("settled", awaitStable())
        }
        if (arguments.optBoolean("include_snapshot", action in listOf("tap", "swipe", "click", "text", "key", "scroll")) && action != "snapshot") {
            result.put("snapshot", snapshot(arguments))
        }
        if (action == "text") result.put("effect", "input_updated")
            .put("next_step", "Text entry is not search completion. Submit with key=enter or click the search button, then inspect results.")
        result.put("elapsed_ms", SystemClock.uptimeMillis() - startedAt)
    }

    private suspend fun submitInput(arguments: JSONObject): JSONObject {
        check(Build.VERSION.SDK_INT >= 30) { "IME enter requires Android 11+. Click the visible submit/search button instead." }
        var editable: AccessibilityNodeInfo? = null
        val root = foregroundRoot() ?: error("No accessible foreground window.")
        visitNodes(root) { node ->
            if (node.isFocused && node.isEditable && node.isVisibleToUser) { editable = node; true } else false
        }
        val node = editable ?: error("No focused input. Focus the search field or click the visible search button.")
        val ok = node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
        val result = JSONObject().put("ok", ok).put("effect", if (ok) "submit_requested" else "submit_rejected")
        if (ok) result.put("settled", awaitStable())
        else result.put("errmsg", "App rejected IME enter. Click its visible search/submit button instead.")
        if (arguments.optBoolean("include_snapshot", true)) result.put("snapshot", snapshot(arguments))
        return result
    }

    /** Bounded event-based settling avoids caller sleeps and never waits indefinitely on animations. */
    suspend fun awaitStable(minimumMillis: Long = 80, maximumMillis: Long = 700): Boolean {
        val start = SystemClock.uptimeMillis()
        while (SystemClock.uptimeMillis() - start < maximumMillis) {
            val now = SystemClock.uptimeMillis()
            if (now - start >= minimumMillis && now - lastWindowEvent >= 100) return true
            delay(25)
        }
        return false
    }

    private fun foregroundRoot(): AccessibilityNodeInfo? =
        windows.firstOrNull {
            it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION && it.isActive
        }?.root ?: windows.firstOrNull {
            it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }?.root ?: rootInActiveWindow

    fun foregroundPackage(): String = foregroundRoot()?.packageName?.toString().orEmpty()

    suspend fun awaitPackage(packageName: String, timeoutMillis: Long = 2500): Boolean {
        val start = SystemClock.uptimeMillis()
        do {
            if (foregroundPackage() == packageName) {
                awaitStable()
                return foregroundPackage() == packageName
            }
            delay(60)
        } while (SystemClock.uptimeMillis() - start < timeoutMillis)
        return false
    }

    private fun displayBounds(): Rect {
        val window = windows.firstOrNull { it.isActive }
        val bounds = Rect()
        window?.getBoundsInScreen(bounds)
        // Coordinates always cover the full physical screen, not just a dialog window.
        val display = getSystemService(android.hardware.display.DisplayManager::class.java)
            .getDisplay(Display.DEFAULT_DISPLAY)
        val point = android.graphics.Point()
        @Suppress("DEPRECATION")
        display?.getRealSize(point)
        return Rect(0, 0, point.x.takeIf { it > 0 } ?: bounds.right, point.y.takeIf { it > 0 } ?: bounds.bottom)
    }

    private fun nodeJson(node: AccessibilityNodeInfo, bounds: Rect, detailed: Boolean): JSONObject {
        val rect = Rect().also(node::getBoundsInScreen)
        return JSONObject().apply {
            put("node_id", observedNodeId(node))
            if (!node.viewIdResourceName.isNullOrBlank()) put("view_id", node.viewIdResourceName)
            val text = if (node.isPassword) "" else node.text?.toString().orEmpty().take(500)
            val description = if (node.isPassword) "" else node.contentDescription?.toString().orEmpty().take(500)
            if (text.isNotBlank()) put("text", text)
            if (description.isNotBlank() && description != text) put("description", description)
            if (node.isClickable) put("clickable", true)
            if (node.isEditable) put("editable", true)
            if (node.isScrollable) put("scrollable", true)
            if (node.isPassword) put("password", true)
            put("bounds", JSONArray(listOf(
                normalizedBound(rect.left, bounds.width()), normalizedBound(rect.top, bounds.height()),
                normalizedBound(rect.right, bounds.width()), normalizedBound(rect.bottom, bounds.height()),
            )))
            put("center", JSONArray(listOf(
                normalizedBound(rect.centerX(), bounds.width()), normalizedBound(rect.centerY(), bounds.height()),
            )))
            if (detailed) {
                put("class", node.className?.toString().orEmpty())
                put("pixel_bounds", JSONArray(listOf(rect.left, rect.top, rect.right, rect.bottom)))
            }
        }
    }

    /** Node handles bind the window, source identity, label and observed geometry.
     * Recycled list rows and moved nodes must be observed again before acting. */
    private fun observedNodeId(node: AccessibilityNodeInfo): String {
        val rect = Rect().also(node::getBoundsInScreen)
        val identity = listOf(node.windowId, node.hashCode(), node.packageName, node.viewIdResourceName,
            node.className, if (node.isPassword) "" else node.text,
            if (node.isPassword) "" else node.contentDescription, rect.flattenToString(), node.isEnabled).joinToString("\u0000")
        return java.security.MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
            .take(12).joinToString("") { "%02x".format(it.toInt() and 255) }
    }

    private fun snapshot(arguments: JSONObject = JSONObject()): JSONObject {
        val nodes = JSONArray()
        val bounds = displayBounds()
        val root = foregroundRoot() ?: error("No accessible foreground window.")
        val detailed = arguments.optBoolean("detailed")
        val interactiveOnly = arguments.optBoolean("interactive_only")
        val limit = arguments.optInt("max_nodes", 80).coerceIn(1, 500)
        val seen = HashSet<String>()
        var omitted = 0
        visitNodes(root) { node ->
            if (!node.isVisibleToUser) return@visitNodes false
            val interactive = node.isClickable || node.isEditable || node.isScrollable
            val labeled = !node.isPassword && (!node.text.isNullOrBlank() || !node.contentDescription.isNullOrBlank())
            if ((interactiveOnly && !interactive) || (!detailed && !interactive && !labeled)) return@visitNodes false
            val json = nodeJson(node, bounds, detailed)
            // Only collapse identical labels at identical locations, preserving distinct controls.
            val signature = json.toString()
            if (!seen.add(signature)) return@visitNodes false
            if (nodes.length() < limit) nodes.put(json) else omitted++
            false
        }
        return JSONObject().put("ok", true).put("package", root.packageName?.toString().orEmpty())
            .put("width", bounds.width()).put("height", bounds.height())
            .put("coordinate_space", "normalized_0_1000").put("nodes", nodes)
            .put("omitted_nodes", omitted).put("truncated", omitted > 0)
    }

    private fun findText(arguments: JSONObject): JSONObject {
        val query = arguments.optString("selector_text").ifBlank { arguments.optString("text") }
        require(query.isNotBlank()) { "find_text/scroll_to_text requires selector_text or text." }
        val matches = JSONArray()
        val bounds = displayBounds()
        val root = foregroundRoot() ?: error("No accessible foreground window.")
        val exact = arguments.optBoolean("exact")
        val seen = HashSet<String>()
        visitNodes(root) { node ->
            if (node.isVisibleToUser && !node.isPassword) {
                val labels = listOf(node.text?.toString().orEmpty(), node.contentDescription?.toString().orEmpty())
                if (labels.any { if (exact) it == query else it.contains(query, ignoreCase = true) }) {
                    val json = nodeJson(node, bounds, false)
                    if (seen.add(json.toString())) matches.put(json)
                }
            }
            false
        }
        return JSONObject().put("ok", true).put("found", matches.length() > 0)
            .put("package", root.packageName?.toString().orEmpty())
            .put("coordinate_space", "normalized_0_1000").put("matches", matches)
    }

    private fun scroll(arguments: JSONObject): JSONObject {
        val direction = arguments.optString("direction", "down").lowercase()
        val action = when (direction) {
            "down", "forward" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            "up", "backward" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            "left" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT.id
            "right" -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT.id
            else -> error("direction must be down, up, left, right, forward or backward.")
        }
        val root = foregroundRoot() ?: error("No accessible foreground window.")
        val viewId = arguments.optString("view_id")
        var target: AccessibilityNodeInfo? = null
        var largestArea = -1L
        visitNodes(root) { node ->
            if (node.isVisibleToUser && node.isScrollable && node.isEnabled &&
                (viewId.isBlank() || node.viewIdResourceName == viewId) &&
                node.actionList.any { it.id == action }) {
                val rect = Rect().also(node::getBoundsInScreen)
                val area = rect.width().toLong() * rect.height()
                if (area > largestArea) { target = node; largestArea = area }
            }
            false
        }
        check(target != null) { "No visible scrollable node supports '$direction'; specify view_id or use swipe." }
        val scrolled = target!!.performAction(action)
        return JSONObject().put("ok", true).put("scrolled", scrolled).put("end_reached", !scrolled)
    }

    private suspend fun scrollToText(arguments: JSONObject): JSONObject {
        val maxScrolls = arguments.optInt("max_scrolls", 12).coerceIn(0, 30)
        var result = findText(arguments)
        val deadline = SystemClock.uptimeMillis() + 20_000
        var count = 0
        var endReached = false
        while (!result.optBoolean("found") && count < maxScrolls && SystemClock.uptimeMillis() < deadline) {
            val before = snapshot().toString()
            val step = scroll(arguments)
            if (!step.optBoolean("scrolled")) { endReached = true; break }
            count++
            awaitStable(minimumMillis = 200)
            val expected = arguments.optString("expected_package")
            check(expected.isBlank() || foregroundPackage() == expected) {
                "Foreground package changed during scroll_to_text; stopped."
            }
            result = findText(arguments)
            if (!result.optBoolean("found") && before == snapshot().toString()) { endReached = true; break }
        }
        return result.put("scrolls", count).put("end_reached", endReached)
            .put("limit_reached", !result.optBoolean("found") && !endReached)
    }

    private fun actOnNode(arguments: JSONObject): JSONObject {
        val root = foregroundRoot() ?: error("No accessible foreground window.")
        val writing = arguments.optString("action") == "text"
        val viewId = arguments.optString("view_id")
        val selectorText = arguments.optString("selector_text")
        val nodeId = arguments.optString("node_id")
        require(nodeId.isNotBlank() || viewId.isNotBlank() || selectorText.isNotBlank() || writing) { "Specify node_id, view_id or selector_text." }
        var matches = 0
        var target: AccessibilityNodeInfo? = null
        visitNodes(root) { node ->
            val matchesSelector = when {
                nodeId.isNotBlank() -> observedNodeId(node) == nodeId
                viewId.isNotBlank() -> node.viewIdResourceName == viewId
                selectorText.isNotBlank() -> node.text?.toString() == selectorText || node.contentDescription?.toString() == selectorText
                else -> node.isFocused && node.isEditable
            }
            if (node.isVisibleToUser && matchesSelector) {
                matches++
                target = node
            }
            false
        }
        require(matches == 1) {
            if (nodeId.isNotBlank()) "Observed node is stale or unavailable. Take a new snapshot before acting."
            else "Selector matched $matches nodes; use node_id from a fresh snapshot or a unique view_id."
        }
        val node = target!!
        val ok = if (writing) {
            require(arguments.has("text")) { "Missing text." }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, arguments.getString("text"))
            })
        } else {
            var clickable: AccessibilityNodeInfo? = node
            repeat(8) {
                if (clickable?.isClickable != true) clickable = clickable?.parent
            }
            clickable?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
        }
        return JSONObject().put("ok", ok).apply {
            if (!ok) put("errmsg", if (writing) "App rejected text input." else "App rejected node click; inspect the screen and try its visible center with tap.")
        }
    }

    private fun visitNodes(root: AccessibilityNodeInfo, block: (AccessibilityNodeInfo) -> Boolean) {
        val queue = java.util.ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var count = 0
        while (queue.isNotEmpty() && count++ < 500) {
            val node = queue.removeFirst()
            if (block(node)) return
            for (index in 0 until minOf(node.childCount, 100)) {
                node.getChild(index)?.let(queue::addLast)
            }
        }
    }

    companion object {
        @Volatile var instance: PhoneAccessibilityService? = null
            private set
    }
}

internal fun screenshotJson(bitmap: Bitmap): JSONObject {
    val scale = minOf(1.0, 1280.0 / maxOf(bitmap.width, bitmap.height))
    val image = if (scale < 1) Bitmap.createScaledBitmap(
        bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1),
        (bitmap.height * scale).toInt().coerceAtLeast(1), true,
    ) else bitmap
    return try {
        val output = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 85, output)
        JSONObject().put("ok", true).put("width", bitmap.width).put("height", bitmap.height)
            .put("image_width", image.width).put("image_height", image.height)
            .put("screenshot_mime_type", "image/jpeg")
            .put("screenshot_base64", android.util.Base64.encodeToString(output.toByteArray(), android.util.Base64.NO_WRAP))
    } finally {
        if (image !== bitmap) image.recycle()
        bitmap.recycle()
    }
}
