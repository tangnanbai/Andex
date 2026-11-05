package com.android.andex.data

import com.android.andex.data.pi.PiKernelBridge
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

data class AndexAppExtensionInfo(
    val id: String,
    val name: String,
    val path: String,
)

data class AndexAppExtensionSurface(
    val id: String,
    val extensionId: String,
    val extensionName: String,
    val slot: String,
    val order: Int,
    val tree: Any?,
)

data class AndexAppExtensionComponent(
    val id: String,
    val extensionId: String,
    val extensionName: String,
    val target: String,
    val mode: String,
    val order: Int,
    val tree: Any?,
)

data class AndexAppExtensionComposerMenuItem(
    val id: String,
    val localId: String,
    val extensionId: String,
    val extensionName: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val order: Int,
    val action: String,
    val args: JSONObject,
    val selected: Boolean,
)

data class AndexAppExtensionSettingsPage(
    val id: String,
    val localId: String,
    val extensionId: String,
    val extensionName: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val order: Int,
    val trailingIcon: String = "",
    val trailingAction: String = "",
    val trailingCategory: String = "",
    val trailingArgs: JSONObject = JSONObject(),
    val sections: List<JSONObject>,
    val categories: List<AndexAppExtensionSettingsCategory> = emptyList(),
)

data class AndexAppExtensionSettingsCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val order: Int,
    val trailingIcon: String = "",
    val trailingAction: String = "",
    val trailingCategory: String = "",
    val trailingArgs: JSONObject = JSONObject(),
    val hidden: Boolean = false,
    val sections: List<JSONObject>,
)

data class AndexAppExtensionMessageType(
    val id: String,
    val type: String,
    val extensionId: String,
    val extensionName: String,
    val title: String,
    val icon: String,
)

data class AndexAppExtensionToolTitle(
    val id: String,
    val extensionId: String,
    val extensionName: String,
    val toolName: String,
    val runningTitle: String,
    val completedTitle: String,
    val priority: Int,
    val sequence: Long = 0L,
)

data class AndexAppExtensionError(
    val path: String,
    val extensionId: String,
    val phase: String,
    val message: String,
)

data class AndexAppExtensionSnapshot(
    val apiVersion: Int = 2,
    val version: Long = 0L,
    val extensions: List<AndexAppExtensionInfo> = emptyList(),
    val surfaces: List<AndexAppExtensionSurface> = emptyList(),
    val components: List<AndexAppExtensionComponent> = emptyList(),
    val composerMenuItems: List<AndexAppExtensionComposerMenuItem> = emptyList(),
    val settings: List<AndexAppExtensionSettingsPage> = emptyList(),
    val messageTypes: List<AndexAppExtensionMessageType> = emptyList(),
    val toolTitles: List<AndexAppExtensionToolTitle> = emptyList(),
    val eventNames: Set<String> = emptySet(),
    val errors: List<AndexAppExtensionError> = emptyList(),
) {
    fun surfacesAt(slot: String): List<AndexAppExtensionSurface> =
        surfaces.filter { it.slot == slot }.sortedWith(
            compareBy<AndexAppExtensionSurface> { it.order }.thenBy { it.id }
        )

    fun componentsAt(target: String): List<AndexAppExtensionComponent> =
        components.filter { it.target == target }.sortedWith(
            compareBy<AndexAppExtensionComponent> { it.order }.thenBy { it.id }
        )
}

data class AndexAppExtensionState(
    val snapshot: AndexAppExtensionSnapshot = AndexAppExtensionSnapshot(),
    val isLoading: Boolean = false,
    val error: String = "",
)

data class AndexAppExtensionNotification(
    val message: String,
    val level: String,
)

data class PiExtensionUiRequest(
    val callId: String,
    val method: String,
    val title: String,
    val message: String = "",
    val placeholder: String = "",
    val options: List<String> = emptyList(),
    val sessionId: String = "",
)

data class AndexAppExtensionEventResult(
    val handled: Boolean,
    val cancelled: Boolean,
    val reason: String,
    val payload: JSONObject,
)

class AndexAppExtensionManager(
    private val bridge: PiKernelBridge,
    private val scope: CoroutineScope,
    private val diagnosticLogger: AndexDiagnosticLogger = AndexDiagnosticLogger.NoOp,
    private val modKernel: AndexModKernel? = null,
    private val loadOptionsProvider: suspend () -> PiExtensionLoadOptions = {
        PiExtensionLoadOptions()
    },
) {
    private val started = AtomicBoolean(false)
    private val refreshMutex = Mutex()
    private val _state = MutableStateFlow(AndexAppExtensionState())
    private val _notifications = MutableSharedFlow<AndexAppExtensionNotification>(
        extraBufferCapacity = 8,
    )
    private val _piUiRequest = MutableStateFlow<PiExtensionUiRequest?>(null)
    private val pendingPiUiRequests = ArrayDeque<PiExtensionUiRequest>()
    private val piUiRequestLock = Any()
    private var subscriptionJob: Job? = null
    private var invalidationJob: Job? = null
    private var latestContextJson = "{}"

    @Volatile
    private var hostHandler: (suspend (String, JSONObject) -> JSONObject)? = null

    val state: StateFlow<AndexAppExtensionState> = _state.asStateFlow()
    val notifications: SharedFlow<AndexAppExtensionNotification> = _notifications.asSharedFlow()
    val piUiRequest: StateFlow<PiExtensionUiRequest?> = _piUiRequest.asStateFlow()

    fun setHostHandler(handler: suspend (String, JSONObject) -> JSONObject) {
        hostHandler = handler
    }

    fun clearHostHandler() {
        hostHandler = null
    }

    private fun publishSnapshot(
        snapshot: AndexAppExtensionSnapshot,
        isLoading: Boolean = false,
        error: String = "",
    ) {
        modKernel.syncScriptToolTitles(snapshot.toolTitles)
        _state.value = AndexAppExtensionState(
            snapshot = snapshot,
            isLoading = isLoading,
            error = error,
        )
    }

    fun start(context: JSONObject = JSONObject()) {
        latestContextJson = context.toString()
        if (!started.compareAndSet(false, true)) return
        subscriptionJob = scope.launch {
            while (true) {
                try {
                    bridge.subscribeAndexExtensions(::handleBridgeEvent)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    diagnosticLogger.exception(
                        category = "andex_extension",
                        event = "subscription_failed",
                        throwable = throwable,
                        level = "warn",
                    )
                    delay(2_000L)
                }
            }
        }
        scope.launch {
            reload(context)
        }
    }

    fun updateContext(context: JSONObject) {
        val serialized = context.toString()
        if (serialized == latestContextJson) return
        latestContextJson = serialized
        if (!started.get()) {
            start(context)
            return
        }
        scope.launch {
            refresh(context)
        }
    }

    suspend fun reload(context: JSONObject = currentContext()): Result<AndexAppExtensionSnapshot> =
        runCatching {
            refreshMutex.withLock {
                _state.value = _state.value.copy(isLoading = true, error = "")
                val response = bridge.reloadAndexExtensions(
                    context = context,
                    loadOptions = loadOptionsProvider(),
                    onEvent = ::handleBridgeEvent,
                )
                val snapshot = parseSnapshot(response.optJSONObject("snapshot"))
                val reloadError = response.extensionReloadError()
                publishSnapshot(snapshot, error = reloadError)
                snapshot
            }
        }.onFailure(::recordFailure)

    suspend fun refresh(context: JSONObject = currentContext()): Result<AndexAppExtensionSnapshot> =
        runCatching {
            refreshMutex.withLock {
                val response = bridge.getAndexExtensions(
                    context = context,
                    loadOptions = loadOptionsProvider(),
                    onEvent = ::handleBridgeEvent,
                )
                val snapshot = parseSnapshot(response.optJSONObject("snapshot"))
                publishSnapshot(snapshot)
                snapshot
            }
        }.onFailure(::recordFailure)

    fun invokeAction(
        extensionId: String,
        action: String,
        args: JSONObject = JSONObject(),
        context: JSONObject = currentContext(),
    ) {
        scope.launch {
            runCatching {
                val response = bridge.invokeAndexExtensionAction(
                    extensionId = extensionId,
                    action = action,
                    args = args,
                    context = context,
                    onEvent = ::handleBridgeEvent,
                )
                parseSnapshot(response.optJSONObject("snapshot"))
            }.onSuccess { snapshot ->
                publishSnapshot(snapshot)
            }.onFailure(::recordFailure)
        }
    }

    suspend fun dispatchEvent(
        event: String,
        data: JSONObject = JSONObject(),
        context: JSONObject = currentContext(),
    ): Result<AndexAppExtensionEventResult> {
        if (event !in _state.value.snapshot.eventNames) {
            return Result.success(
                AndexAppExtensionEventResult(
                    handled = false,
                    cancelled = false,
                    reason = "",
                    payload = data,
                )
            )
        }
        return runCatching {
            val response = bridge.dispatchAndexExtensionEvent(
                event = event,
                data = data,
                context = context,
                onEvent = ::handleBridgeEvent,
            )
            response.optJSONObject("snapshot")?.let(::parseSnapshot)?.let { snapshot ->
                publishSnapshot(snapshot)
            }
            AndexAppExtensionEventResult(
                handled = response.optBoolean("handled"),
                cancelled = response.optBoolean("cancelled"),
                reason = response.optString("reason"),
                payload = response.optJSONObject("payload") ?: data,
            )
        }.onFailure(::recordFailure)
    }

    fun emitEvent(
        event: String,
        data: JSONObject = JSONObject(),
        context: JSONObject = currentContext(),
    ) {
        if (event !in _state.value.snapshot.eventNames) return
        scope.launch {
            dispatchEvent(event, data, context)
        }
    }

    suspend fun handleAgentBridgeEvent(
        event: String,
        payload: JSONObject,
    ) {
        handleBridgeEvent(event, payload)
    }

    fun respondToPiExtensionUiRequest(
        callId: String,
        value: Any?,
    ) {
        val request = synchronized(piUiRequestLock) {
            val current = _piUiRequest.value
            if (current?.callId != callId) return
            _piUiRequest.value = pendingPiUiRequests.removeFirstOrNull()
            current
        }
        scope.launch {
            bridge.sendAndexHostResult(
                callId = request.callId,
                result = JSONObject().put("value", value ?: JSONObject.NULL),
            )
        }
    }

    fun waitingUiSessionIds(): Set<String> = synchronized(piUiRequestLock) {
        (listOfNotNull(_piUiRequest.value) + pendingPiUiRequests)
            .map { it.sessionId }.filter { it.isNotBlank() }.toSet()
    }

    private suspend fun handleBridgeEvent(
        event: String,
        payload: JSONObject,
    ) {
        when (event) {
            "andex_invalidated" -> {
                scheduleRefresh()
            }

            "andex_notification" -> {
                _notifications.emit(
                    AndexAppExtensionNotification(
                        message = payload.optString("message"),
                        level = payload.optString("level").ifBlank { "info" },
                    )
                )
            }

            "andex_host_call" -> {
                val callId = payload.optString("call_id")
                val method = payload.optString("method")
                val args = payload.optJSONObject("args") ?: JSONObject()
                if (method == "pi_extension_notify") {
                    _notifications.emit(
                        AndexAppExtensionNotification(
                            message = args.optString("message"),
                            level = args.optString("type").ifBlank { "info" },
                        )
                    )
                    bridge.sendAndexHostResult(
                        callId = callId,
                        result = JSONObject().put("notified", true),
                    )
                    return
                }
                if (method in PiExtensionInteractiveUiMethods) {
                    val request = PiExtensionUiRequest(
                        callId = callId,
                        method = method,
                        title = args.optString("title"),
                        message = args.optString("message"),
                        placeholder = args.optString("placeholder"),
                        options = args.optJSONArray("options").toStringList(),
                        sessionId = args.optString("session_id"),
                    )
                    synchronized(piUiRequestLock) {
                        if (_piUiRequest.value == null) {
                            _piUiRequest.value = request
                        } else {
                            pendingPiUiRequests.addLast(request)
                        }
                    }
                    return
                }
                val result = runCatching {
                    val handler = hostHandler
                        ?: error("The Andex UI host is not attached.")
                    handler(method, args)
                }
                bridge.sendAndexHostResult(
                    callId = callId,
                    result = result.getOrDefault(JSONObject()),
                    error = result.exceptionOrNull()?.message.orEmpty(),
                )
            }
        }
    }

    private fun currentContext(): JSONObject =
        runCatching { JSONObject(latestContextJson) }.getOrElse { JSONObject() }

    private fun scheduleRefresh() {
        if (invalidationJob?.isActive == true) return
        invalidationJob = scope.launch {
            delay(100L)
            refresh()
        }
    }

    private fun recordFailure(throwable: Throwable) {
        if (throwable is CancellationException) return
        diagnosticLogger.exception(
            category = "andex_extension",
            event = "operation_failed",
            throwable = throwable,
        )
        _state.value = _state.value.copy(
            isLoading = false,
            error = throwable.message ?: throwable.javaClass.simpleName,
        )
    }
}

private val PiExtensionInteractiveUiMethods = setOf(
    "pi_extension_select",
    "pi_extension_confirm",
    "pi_extension_input",
)

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index -> optString(index).takeIf(String::isNotBlank) }
}

internal fun JSONObject.extensionReloadError(): String {
    if (optBoolean("reloaded", true)) return ""
    val errors = optJSONArray("errors") ?: return "Andex extensions rejected the reload."
    return buildList {
        for (index in 0 until errors.length()) {
            errors.optJSONObject(index)
                ?.optString("error")
                ?.takeIf(String::isNotBlank)
                ?.let(::add)
        }
    }.distinct().take(3).joinToString("; ")
        .ifBlank { "Andex extensions rejected the reload." }
}

internal fun parseAndexAppExtensionSnapshot(json: JSONObject?): AndexAppExtensionSnapshot {
    if (json == null) return AndexAppExtensionSnapshot()
    return AndexAppExtensionSnapshot(
        apiVersion = json.optInt("api_version", 2),
        version = json.optLong("version"),
        extensions = json.optJSONArray("extensions").objects().map { item ->
            AndexAppExtensionInfo(
                id = item.optString("id"),
                name = item.optString("name"),
                path = item.optString("path"),
            )
        },
        surfaces = json.optJSONArray("surfaces").objects().map { item ->
            AndexAppExtensionSurface(
                id = item.optString("id"),
                extensionId = item.optString("extension_id"),
                extensionName = item.optString("extension_name"),
                slot = item.optString("slot"),
                order = item.optInt("order"),
                tree = item.opt("tree"),
            )
        },
        components = json.optJSONArray("components").objects().map { item ->
            AndexAppExtensionComponent(
                id = item.optString("id"),
                extensionId = item.optString("extension_id"),
                extensionName = item.optString("extension_name"),
                target = item.optString("target"),
                mode = item.optString("mode").ifBlank { "wrap" },
                order = item.optInt("order"),
                tree = item.opt("tree"),
            )
        },
        composerMenuItems = json.optJSONArray("composer_menu_items").objects().map { item ->
            AndexAppExtensionComposerMenuItem(
                id = item.optString("id"),
                localId = item.optString("local_id"),
                extensionId = item.optString("extension_id"),
                extensionName = item.optString("extension_name"),
                title = item.optString("title"),
                subtitle = item.optString("subtitle"),
                icon = item.optString("icon").ifBlank { "extension" },
                order = item.optInt("order"),
                action = item.optString("action"),
                args = item.optJSONObject("args") ?: JSONObject(),
                selected = item.optBoolean("selected"),
            )
        },
        settings = json.optJSONArray("settings").objects().map { item ->
            AndexAppExtensionSettingsPage(
                id = item.optString("id"),
                localId = item.optString("local_id"),
                extensionId = item.optString("extension_id"),
                extensionName = item.optString("extension_name"),
                title = item.optString("title"),
                subtitle = item.optString("subtitle"),
                icon = item.optString("icon").ifBlank { "settings" },
                order = item.optInt("order"),
                trailingIcon = item.optString("trailing_icon").ifBlank { item.optString("trailingIcon") },
                trailingAction = item.optString("trailing_action").ifBlank { item.optString("trailingAction") },
                trailingCategory = item.optString("trailing_category").ifBlank { item.optString("trailingCategory") },
                trailingArgs = item.optJSONObject("trailing_args") ?: item.optJSONObject("trailingArgs") ?: JSONObject(),
                sections = item.optJSONArray("sections").objects(),
                categories = item.optJSONArray("categories").objects().map { category ->
                    AndexAppExtensionSettingsCategory(
                        id = category.optString("id"),
                        title = category.optString("title"),
                        subtitle = category.optString("subtitle"),
                        icon = category.optString("icon").ifBlank { "settings" },
                        order = category.optInt("order"),
                        trailingIcon = category.optString("trailing_icon").ifBlank { category.optString("trailingIcon") },
                        trailingAction = category.optString("trailing_action").ifBlank { category.optString("trailingAction") },
                        trailingCategory = category.optString("trailing_category").ifBlank { category.optString("trailingCategory") },
                        trailingArgs = category.optJSONObject("trailing_args") ?: category.optJSONObject("trailingArgs") ?: JSONObject(),
                        hidden = category.optBoolean("hidden", false),
                        sections = category.optJSONArray("sections").objects(),
                    )
                }.sortedWith(compareBy<AndexAppExtensionSettingsCategory> { it.order }.thenBy { it.id }),
            )
        },
        messageTypes = json.optJSONArray("message_types").objects().map { item ->
            AndexAppExtensionMessageType(
                id = item.optString("id"),
                type = item.optString("type"),
                extensionId = item.optString("extension_id"),
                extensionName = item.optString("extension_name"),
                title = item.optString("title"),
                icon = item.optString("icon").ifBlank { "extension" },
            )
        },
        toolTitles = json.optJSONArray("tool_titles").objects().map { item ->
            AndexAppExtensionToolTitle(
                id = item.optString("id"),
                extensionId = item.optString("extension_id"),
                extensionName = item.optString("extension_name"),
                toolName = item.optString("tool_name"),
                runningTitle = item.optString("running_title"),
                completedTitle = item.optString("completed_title"),
                priority = item.optInt("priority", 100),
                sequence = item.optLong("sequence"),
            )
        }.sortedWith(
            compareBy<AndexAppExtensionToolTitle> { it.priority }
                .thenBy { it.sequence }
                .thenBy { it.id },
        ),
        eventNames = json.optJSONArray("event_names").strings().toSet(),
        errors = json.optJSONArray("errors").objects().map { item ->
            AndexAppExtensionError(
                path = item.optString("path"),
                extensionId = item.optString("extension_id"),
                phase = item.optString("phase"),
                message = item.optString("error"),
            )
        },
    )
}

private fun parseSnapshot(json: JSONObject?): AndexAppExtensionSnapshot =
    parseAndexAppExtensionSnapshot(json)

private const val ScriptToolTitleOwner = "andex-script-extensions"

private fun AndexModKernel?.syncScriptToolTitles(
    toolTitles: List<AndexAppExtensionToolTitle>,
) {
    this ?: return
    this.toolTitles.unregisterOwner(ScriptToolTitleOwner)
    toolTitles
        .filter { title ->
            title.toolName.isNotBlank() &&
                title.runningTitle.isNotBlank() &&
                title.completedTitle.isNotBlank()
        }
        .forEach { title ->
            this.toolTitles.register(
                toolName = title.toolName,
                runningTitle = title.runningTitle,
                completedTitle = title.completedTitle,
                owner = ScriptToolTitleOwner,
                priority = title.priority,
            )
        }
}

private fun JSONArray?.objects(): List<JSONObject> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.let(::add)
        }
    }
}

private fun JSONArray?.strings(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optString(index).takeIf(String::isNotBlank)?.let(::add)
        }
    }
}
