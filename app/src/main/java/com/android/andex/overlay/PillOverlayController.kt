package com.android.andex.overlay

import com.android.andex.AndexApplication
import com.android.andex.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import android.content.*
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.*
import android.widget.*
import org.json.JSONObject
import kotlin.math.abs

class PillOverlayController(context: Context) : ContextWrapper(context), ComponentCallbacks {
    private val prefs by lazy { getSharedPreferences("pill", MODE_PRIVATE) }
    private val wm by lazy { getSystemService(WindowManager::class.java) }
    private var connected = false
    private var foreground = true
    private var tasks = emptyList<JSONObject>()
    private var expanded = false
    private val dismissed = mutableSetOf<String>()
    private var mode = OverlayMode.HIDDEN
    private var window: View? = null
    private var backdrop: View? = null
    private var title: TextView? = null
    private var detail: TextView? = null
    private var progress: ProgressBar? = null
    private var pillLabel: TextView? = null
    private var params: WindowManager.LayoutParams? = null
    private var dragging = false
    private var currentTask: JSONObject? = null
    private var x = 0
    private var y = 0
    private var health = ""

    private val enabled = MutableStateFlow(prefs.getBoolean("enabled", true))
    private val preferenceListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
        if (key == "enabled") enabled.value = preferences.getBoolean("enabled", true)
    }
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job != null) return
        x = prefs.getInt("x", dp(18))
        y = prefs.getInt("y", dp(160))
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
        registerComponentCallbacks(this)
        val runtime = (applicationContext as AndexApplication).runtime
        job = scope.launch(Dispatchers.Main.immediate) {
            combine(runtime.sessionExecutionManager.executionStates,
                runtime.appForegroundTracker.isForeground, enabled,
                runtime.taskStatusPublisher.hasRecentResults) { executions, isForeground, isEnabled, result ->
                listOf(executions.values.any { it.isRunning }, isForeground, isEnabled, result)
            }.distinctUntilChanged().collectLatest { (active, isForeground, isEnabled, recentResult) ->
                val running = active || recentResult
                foreground = isForeground
                connected = isEnabled && Settings.canDrawOverlays(this@PillOverlayController)
                if (!running || foreground || !connected) {
                    tasks = if (running) tasks else emptyList()
                    render()
                    if (running) {
                        // Expire recent results even while the app stays foreground.
                        while (isActive && runtime.taskStatusPublisher.hasRecentResults.value) {
                            runtime.taskStatusPublisher.snapshot()
                            delay(1000)
                        }
                    }
                    return@collectLatest
                }
                while (isActive) {
                    connected = enabled.value && Settings.canDrawOverlays(this@PillOverlayController)
                    val status = runtime.taskStatusPublisher.snapshot()
                    foreground = status.getBoolean("foreground")
                    val array = status.getJSONArray("tasks")
                    tasks = (0 until array.length()).map { array.getJSONObject(it) }
                    render()
                    delay(1000)
                }
            }
        }
    }

    private fun setHealth(value: String) {
        if (health == value) return
        health = value
        prefs.edit().putString("health", value).apply()
    }

    private fun render() {
        val ids = tasks.map { it.getString("run_id") }.toSet()
        dismissed.retainAll(ids)
        val visible = tasks.filter { it.getString("run_id") !in dismissed }
        currentTask = visible.firstOrNull()
        val desired = OverlayPolicy.mode(connected, foreground, ids, dismissed, expanded)
        if (desired != mode) {
            removeWindows()
            mode = desired
            when (desired) {
                OverlayMode.HIDDEN -> expanded = false
                OverlayMode.PILL -> showPill()
                OverlayMode.BUBBLE -> showBubble()
            }
        }
        val task = currentTask ?: return
        title?.text = task.optString("title", "Andex 任务")
        val seconds = task.optLong("elapsed_ms") / 1000
        detail?.text = task.optString("status", "正在运行") + "\n${seconds / 60}分${seconds % 60}秒" +
            " · 已完成 ${task.optInt("completed_tools")} 次工具调用" +
            if (visible.size > 1) "\n另有 ${visible.size - 1} 个任务" else ""
        val percent = if (task.isNull("progress")) null else task.optInt("progress").coerceIn(0, 100)
        val state = task.optString("state")
        val terminal = state in setOf("completed", "failed", "cancelled")
        progress?.let {
            it.visibility = if (mode == OverlayMode.PILL && terminal) View.GONE else View.VISIBLE
            it.isIndeterminate = percent == null && state !in setOf("failed", "waiting", "cancelled")
            if (!it.isIndeterminate) it.progress = percent ?: 0
        }
        pillLabel?.text = task.optString("label").ifBlank { OverlayPolicy.label(state) }
        if (mode == OverlayMode.PILL) {
            val label = pillLabel ?: return
            val targetWidth = (label.paint.measureText(label.text.toString()).toInt() +
                dp(if (terminal) 24 else 48)).coerceIn(dp(96), minOf(dp(196), screenSize().first))
            params?.let { p ->
                if (p.width != targetWidth) {
                    p.width = targetWidth
                    clamp(p)
                    window?.let { runCatching { wm.updateViewLayout(it, p) } }
                }
            }
        }
    }

    private fun windowParams(width: Int, height: Int) = WindowManager.LayoutParams(
        width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply { gravity = Gravity.TOP or Gravity.LEFT; title = "Andex $mode" }

    private fun screenSize(): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= 30) {
            val bounds = wm.maximumWindowMetrics.bounds
            return bounds.width() to bounds.height()
        }
        val size = android.graphics.Point()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealSize(size)
        return size.x to size.y
    }
    private fun clamp(p: WindowManager.LayoutParams) {
        val (width, height) = screenSize()
        p.x = OverlayPolicy.clamp(p.x, width, p.width)
        p.y = OverlayPolicy.clamp(p.y, height, p.height)
    }

    private fun addWindow(view: View, width: Int, height: Int) {
        val p = windowParams(width, height).apply { this.x = this@PillOverlayController.x; this.y = this@PillOverlayController.y }
        clamp(p)
        params = p
        window = view
        view.contentDescription = if (mode == OverlayMode.PILL) "Andex 后台进度" else "Andex 任务详情"
        try {
            wm.addView(view, p)
            view.pivotX = 0f
            view.pivotY = 0f
            view.scaleX = 0.88f; view.scaleY = 0.88f; view.alpha = 0f
            view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(160).start()
        } catch (_: RuntimeException) {
            removeWindows()
            mode = OverlayMode.HIDDEN
            setHealth("悬浮窗无法显示，请检查系统授权")
        }
    }
    private fun background(radius: Int) = GradientDrawable().apply {
        setColor(Color.rgb(255, 243, 247))
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), Color.rgb(236, 183, 199))
    }
    private fun text(size: Float) = TextView(this).apply {
        textSize = size; setTextColor(Color.rgb(87, 42, 61))
    }
    private fun showPill() {
        val box = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = background(22)
            elevation = dp(3).toFloat()
        }
        progress = ProgressBar(this).apply {
            indeterminateTintList = android.content.res.ColorStateList.valueOf(Color.rgb(225, 89, 132))
        }
        box.addView(progress, LinearLayout.LayoutParams(dp(18), dp(18)).apply { marginEnd = dp(5) })
        pillLabel = text(13f).apply { typeface = Typeface.DEFAULT_BOLD }
        box.addView(pillLabel)
        box.setOnClickListener { expanded = true; render() }
        installDrag(box)
        addWindow(box, dp(132), dp(36))
    }
    private fun showBubble() {
        backdrop = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            contentDescription = "收起 Andex 任务详情"
            setOnClickListener { expanded = false; render() }
        }
        val maskParams = windowParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        try { wm.addView(backdrop, maskParams) }
        catch (_: RuntimeException) { backdrop = null; expanded = false; mode = OverlayMode.HIDDEN; return }

        val width = minOf(dp(280), screenSize().first)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = background(20)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            elevation = dp(6).toFloat()
            setOnClickListener { openHost() }
        }
        val handle = text(12f).apply { text = "Andex · 拖动此处移动"; setPadding(0, 0, 0, dp(8)) }
        handle.setOnClickListener { openHost() }
        installDrag(handle)
        box.addView(handle)
        title = text(17f).apply { typeface = Typeface.DEFAULT_BOLD; maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END }
        box.addView(title)
        detail = text(13f).apply {
            maxLines = 4; ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(0, dp(6), 0, dp(6))
        }
        box.addView(detail)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            progressTintList = android.content.res.ColorStateList.valueOf(Color.rgb(225, 89, 132))
            indeterminateTintList = progressTintList
        }
        box.addView(progress, LinearLayout.LayoutParams(-1, dp(4)))
        val actions = LinearLayout(this)
        actions.addView(text(12f).apply {
            text = "打开会话"; setPadding(0, dp(12), dp(20), 0)
            setOnClickListener { openHost() }
        })
        actions.addView(text(12f).apply {
            text = "隐藏本次任务"; setPadding(0, dp(12), 0, 0)
            setOnClickListener {
                currentTask?.optString("run_id")?.let { dismissed.add(it) }
                expanded = false
                render()
            }
        })
        box.addView(actions)
        addWindow(box, width, minOf(dp(218), screenSize().second))
    }

    private fun installDrag(view: View) {
        var downX = 0f; var downY = 0f
        var originX = 0; var originY = 0
        var downAt = 0L
        val threshold = ViewConfiguration.get(this).scaledTouchSlop
        view.setOnTouchListener { target, event ->
            val p = params ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    originX = p.x; originY = p.y; downAt = event.eventTime
                    dragging = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX; val dy = event.rawY - downY
                    if (abs(dx) > threshold || abs(dy) > threshold) dragging = true
                    if (dragging) {
                        p.x = originX + dx.toInt(); p.y = originY + dy.toInt()
                        clamp(p)
                        window?.let { runCatching { wm.updateViewLayout(it, p) } }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (dragging) {
                        x = p.x; y = p.y
                        prefs.edit().putInt("x", x).putInt("y", y).apply()
                    } else if (event.actionMasked == MotionEvent.ACTION_UP && event.eventTime - downAt < 600) {
                        target.performClick()
                    }
                    dragging = false
                    true
                }
                else -> false
            }
        }
    }
    private fun openHost() {
        val id = currentTask?.optString("id") ?: return
        try {
            startActivity(Intent(OverlayNavigation.OPEN_TASK)
                .setClass(this, MainActivity::class.java)
                .putExtra(OverlayNavigation.SESSION_EXTRA, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP))
            expanded = false
            foreground = true
            render()
        } catch (_: RuntimeException) {
            Toast.makeText(this, "无法打开 Andex，请检查宿主是否已安装", Toast.LENGTH_SHORT).show()
        }
    }
    private fun removeWindows() {
        window?.animate()?.cancel()
        window?.let { runCatching { wm.removeViewImmediate(it) } }
        backdrop?.let { runCatching { wm.removeViewImmediate(it) } }
        window = null; backdrop = null; params = null
        title = null; detail = null; progress = null; pillLabel = null
    }
    override fun onConfigurationChanged(config: Configuration) {
        val oldMode = mode
        removeWindows(); mode = OverlayMode.HIDDEN
        expanded = oldMode == OverlayMode.BUBBLE
        render()
    }
    override fun onLowMemory() = Unit
    fun close() {
        job?.cancel(); job = null
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        unregisterComponentCallbacks(this)
        removeWindows()
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
