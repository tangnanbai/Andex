package com.android.andex.phonecontrol
import com.android.andex.ui.AndexGlassAlertDialog as AlertDialog

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.android.andex.AndexAppRuntime
import com.android.andex.AndexApplication
import com.android.andex.R
import com.android.andex.data.AppSettings
import com.android.andex.ui.AlpineTerminalScreen
import com.android.andex.ui.theme.AndexTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

class PhoneControlActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val runtime = (application as AndexApplication).runtime
        setContent {
            val settings by runtime.settingsRepository.settings.collectAsState(initial = AppSettings())
            AndexTheme(themeMode = settings.themeMode, themeAccent = settings.themeAccent, themeBackground = settings.themeBackground, language = settings.language) {
                PhoneControlScreen(runtime, intent.getBooleanExtra("open_terminal", false), ::finish)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneControlScreen(runtime: AndexAppRuntime, openTerminal: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val controller = runtime.phoneControlController
    val preferences = controller.preferences
    val scope = rememberCoroutineScope()
    var terminal by rememberSaveable { mutableStateOf(openTerminal) }
    var enabled by remember { mutableStateOf(preferences.enabled) }
    var consent by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(JSONObject()) }
    var output by remember { mutableStateOf("") }
    var pairingEndpoint by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    var endpoint by rememberSaveable { mutableStateOf(preferences.adbEndpoint) }

    fun runOperation(block: suspend () -> JSONObject) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                output = block().toString(2)
                status = controller.status()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                output = error.message.orEmpty()
            } finally {
                busy = false
            }
        }
    }

    fun openSettings(action: String, data: Uri? = null) {
        runCatching { context.startActivity(Intent(action, data)) }
            .onFailure { output = it.message.orEmpty() }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        scope.launch { status = controller.status() }
    }
    LaunchedEffect(Unit) {
        if (preferences.embeddedTermux) {
            val setup = runtime.bashTool.embeddedRuntime.initialize()
            if (setup.isReady && preferences.embeddedTermux) runtime.settingsRepository.markEmbeddedTermuxReady()
            if (!setup.isReady) output = setup.detail
        }
        status = controller.status()
    }

    fun closeTerminal() {
        if (openTerminal) onBack() else terminal = false
    }
    BackHandler(terminal) { closeTerminal() }
    if (terminal) {
        val launch = remember(runtime) {
            suspend { runCatching { runtime.bashTool.createEmbeddedTerminalLaunchSpec() } }
        }
        AlpineTerminalScreen(launch, ::closeTerminal, terminalName = "Termux")
        return
    }

    if (consent) {
        AlertDialog(
            onDismissRequest = { consent = false },
            title = { Text(stringResource(R.string.phone_control_allow_title)) },
            text = { Text(stringResource(R.string.phone_control_allow_message)) },
            confirmButton = {
                TextButton(onClick = {
                    preferences.enabled = true
                    enabled = true
                    consent = false
                }) { Text(stringResource(R.string.phone_control_allow)) }
            },
            dismissButton = {
                TextButton(onClick = { consent = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.phone_control_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.phone_control_back))
                    }
                },
                actions = {
                    IconButton(enabled = !busy, onClick = { runOperation { controller.status() } }) {
                        Icon(Icons.Rounded.Refresh, stringResource(R.string.phone_control_refresh))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ToggleRow(stringResource(R.string.phone_control_ai_enabled), enabled, !busy) { value ->
                if (value) consent = true
                else {
                    preferences.enabled = false
                    enabled = false
                }
            }
            HorizontalDivider()
            Text("ADB", style = MaterialTheme.typography.titleMedium)
            ReadyState(status.optBoolean("adb_ready"))
            OutlinedButton(enabled = !busy, onClick = { openSettings(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS) }) {
                Text(stringResource(R.string.phone_control_developer_settings))
            }
            OutlinedButton(enabled = !busy, onClick = { runOperation { controller.installAdb() } }) {
                Text(stringResource(R.string.phone_control_install_adb))
            }
            OutlinedTextField(
                value = pairingEndpoint, onValueChange = { pairingEndpoint = it },
                label = { Text(stringResource(R.string.phone_control_pair_endpoint)) },
                singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = pairingCode, onValueChange = { pairingCode = it.filter(Char::isDigit).take(6) },
                label = { Text(stringResource(R.string.phone_control_pair_code)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(),
            )
            Button(enabled = !busy && pairingCode.length == 6 && pairingEndpoint.isNotBlank(), onClick = {
                val code = pairingCode
                pairingCode = ""
                runOperation { controller.pair(pairingEndpoint, code) }
            }) { Text(stringResource(R.string.phone_control_pair)) }
            OutlinedTextField(
                value = endpoint, onValueChange = { endpoint = it },
                label = { Text(stringResource(R.string.phone_control_connect_endpoint)) },
                singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(enabled = !busy && endpoint.isNotBlank(), onClick = { runOperation { controller.connect(endpoint) } }) {
                    Text(stringResource(R.string.phone_control_connect))
                }
                OutlinedButton(enabled = !busy && preferences.adbConnected, onClick = { runOperation { controller.disconnect() } }) {
                    Text(stringResource(R.string.phone_control_disconnect))
                }
            }
            HorizontalDivider()
            Text(stringResource(R.string.phone_control_accessibility), style = MaterialTheme.typography.titleMedium)
            ReadyState(status.optBoolean("accessibility_ready"))
            OutlinedButton(enabled = !busy, onClick = { openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS) }) {
                Text(stringResource(R.string.phone_control_accessibility_settings))
            }
            if (com.android.andex.BuildConfig.DEBUG) OutlinedButton(enabled = !busy, onClick = {
                runOperation {
                    val report = JSONObject().put("status_before", controller.status())
                    val spec = runtime.bashTool.createEmbeddedTerminalLaunchSpec()
                    val env = spec.environment.associate {
                        it.substringBefore('=') to it.substringAfter('=')
                    }
                    val bridgeResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val request = okhttp3.Request.Builder()
                            .url(env.getValue("ANDEX_TERMUX_API_URL") + "/android-am")
                            .header("Authorization", "Bearer " + env.getValue("ANDEX_TERMUX_API_TOKEN"))
                            .post(okhttp3.RequestBody.create(null,
                                "start\u0000-n\u0000com.android.settings/.Settings\u0000"))
                            .build()
                        okhttp3.OkHttpClient().newCall(request).execute().use {
                            JSONObject(it.body?.string().orEmpty()).put("http_status", it.code)
                        }
                    }
                    report.put("termux_native_am", bridgeResult)
                    kotlinx.coroutines.delay(5000)
                    if (PhoneAccessibilityService.instance != null) {
                        report.put("accessibility_snapshot", JSONObject(controller.execute(
                            """{"backend":"accessibility","action":"snapshot"}""")))
                        report.put("foreground_after_5_seconds", PhoneAccessibilityService.instance?.foregroundPackage())
                        report.put("find_text", JSONObject(controller.execute(
                            """{"backend":"accessibility","action":"find_text","selector_text":"设置","expected_package":"com.android.settings"}""")))
                        report.put("missing_coordinate", JSONObject(controller.execute(
                            """{"backend":"accessibility","action":"swipe","foo_x":500,"foo_y":800}""")))
                        report.put("batch", JSONObject(controller.execute(
                            """{"backend":"accessibility","action":"batch","expected_package":"com.android.settings","actions":[{"action":"scroll","direction":"down"},{"action":"snapshot","max_nodes":30},{"action":"find_text","selector_text":"设置"}]}""")))
                        report.put("wrong_foreground_guard", JSONObject(controller.execute(
                            """{"backend":"accessibility","action":"tap","x":500,"y":500,"expected_package":"com.example.nonexistent"}""")))
                        report.put("swipe_alias", JSONObject(controller.execute(
                            """{"backend":"accessibility","action":"swipe","start_x":500,"start_y":700,"end_x":500,"end_y":300,"duration_ms":200,"expected_package":"com.android.settings"}""")))
                        report.put("scroll_to_text", JSONObject(controller.execute(
                            """{"backend":"accessibility","action":"scroll_to_text","selector_text":"Andex-nonexistent-test-text","max_scrolls":1,"direction":"up","expected_package":"com.android.settings"}""")))
                        report.put("default_termux_screen", JSONObject(controller.execute(
                            """{"action":"snapshot","max_nodes":20,"expected_package":"com.android.settings"}""")))
                        report.put("accessibility_back", JSONObject(controller.execute(
                            """{"backend":"accessibility","action":"key","key":"back"}""")))
                    }
                    if (android.os.Build.MODEL.contains("SM-S9260") ||
                        android.os.Build.FINGERPRINT.contains("generic")) {
                        report.put("adb_connect", controller.connect("127.0.0.1:5555"))
                    }
                    if (preferences.adbConnected) {
                        report.put("adb_shell", JSONObject(controller.execute(
                            """{"backend":"adb","action":"shell","command":"id"}""")))
                    }
                    report.put("status_after", controller.status())
                    java.io.File(context.cacheDir, "andex-phone-repair-check.json").writeText(report.toString(2))
                    context.startActivity(Intent(context, PhoneControlActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                    report
                }
            }) { Text("验证本机控制修复") }
            if (com.android.andex.BuildConfig.DEBUG) OutlinedButton(
                enabled = !busy,
                onClick = {
                    runOperation {
                        val report = JSONObject()
                        report.put("adb", controller.installAdb())
                        if (android.os.Build.MODEL.contains("SM-S9260") ||
                            android.os.Build.FINGERPRINT.contains("generic")) {
                            report.put("adb_connect", controller.connect("127.0.0.1:5555"))
                            if (preferences.adbConnected && preferences.enabled) report.put("adb_shell",
                                JSONObject(controller.execute(
                                    """{"backend":"adb","action":"shell","command":"id; input keyevent KEYCODE_HOME"}""")))
                            kotlinx.coroutines.delay(300)
                            context.startActivity(Intent(context, PhoneControlActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                        }
                        val tools = com.android.andex.data.ArchiveTool(context)
                        val folder = java.io.File(context.cacheDir, "tool-check-${java.util.UUID.randomUUID()}").apply { mkdirs() }
                        val source = java.io.File(folder, "hello.txt").apply { writeText("Andex archive round trip\n") }
                        val archive = java.io.File(folder, "sample.7z")
                        val destination = java.io.File(folder, "extracted")
                        report.put("compress", JSONObject(tools.execute(JSONObject()
                            .put("action", "compress").put("archive", archive.path)
                            .put("sources", org.json.JSONArray(listOf(source.path))).toString())))
                        report.put("extract", JSONObject(tools.execute(JSONObject()
                            .put("action", "extract").put("archive", archive.path)
                            .put("destination", destination.path).toString())))
                        report.put("archive_round_trip", destination.walkTopDown()
                            .any { it.isFile && it.name == source.name && it.readText() == source.readText() })
                        if (preferences.enabled && PhoneAccessibilityService.instance != null) {
                            report.put("launch_settings", JSONObject(controller.execute(
                                """{"backend":"accessibility","action":"launch","package":"com.android.settings"}""")))
                            kotlinx.coroutines.delay(1000)
                            report.put("snapshot", JSONObject(controller.execute(
                                """{"backend":"accessibility","action":"snapshot"}""")))
                            report.put("tap", JSONObject(controller.execute(
                                """{"backend":"accessibility","action":"tap","x":500,"y":400}""")))
                            kotlinx.coroutines.delay(400)
                            report.put("back", JSONObject(controller.execute(
                                """{"backend":"accessibility","action":"key","key":"back"}""")))
                            report.put("home", JSONObject(controller.execute(
                                """{"backend":"accessibility","action":"key","key":"home"}""")))
                            kotlinx.coroutines.delay(500)
                            context.startActivity(Intent(context, PhoneControlActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                        }
                        val web = com.android.andex.data.WebResearchTool(context)
                        report.put("github", JSONObject(web.execute("andex_github",
                            """{"action":"repository","repository":"termux/termux-app"}""")))
                        report.put("page", JSONObject(web.execute("andex_web_page",
                            """{"url":"https://example.com"}""")))
                        val pageId = report.getJSONObject("page").optString("page_id")
                        if (pageId.isNotBlank()) report.put("find_page", JSONObject(web.execute("andex_web_page",
                            JSONObject().put("page_id", pageId).put("find_text", "documentation").toString())))
                        listOf("bing", "baidu", "google").forEach { engine ->
                            report.put("search_$engine", JSONObject(web.execute("andex_web_search",
                                JSONObject().put("query", "Termux GitHub").put("engine", engine).put("limit", 3).toString())))
                        }
                        java.io.File(context.cacheDir, "andex-tool-check.json").writeText(report.toString(2))
                        report
                    }
                },
            ) { Text("验证内置工具与无障碍") }
            if (busy) CircularProgressIndicator()
            if (output.isNotBlank()) Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("JSON 报告", Modifier.weight(1f))
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Andex report", output))
                    }) { Text("复制") }
                    TextButton(onClick = { output = "" }) { Text("清空") }
                }
                SelectionContainer {
                    Text(output, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth().height(240.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .verticalScroll(rememberScrollState()).padding(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onChange, enabled = enabled)
    }
}

@Composable
private fun ReadyState(ready: Boolean) {
    Text(
        stringResource(if (ready) R.string.phone_control_ready else R.string.phone_control_not_ready),
        color = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
}
