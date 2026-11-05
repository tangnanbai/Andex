package com.android.andex.data

import com.android.andex.runtime.SharedPiBridgeClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

data class SharedAndexExtensionInfo(
    val id: String,
    val name: String,
    val path: String,
)

data class SharedAndexExtensionSurface(
    val id: String,
    val extensionId: String,
    val extensionName: String,
    val slot: String,
    val order: Int,
    val tree: JsonElement?,
)

data class SharedAndexExtensionComponent(
    val id: String,
    val extensionId: String,
    val extensionName: String,
    val target: String,
    val mode: String,
    val order: Int,
    val tree: JsonElement?,
)

data class SharedAndexExtensionComposerMenuItem(
    val id: String,
    val localId: String,
    val extensionId: String,
    val extensionName: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val order: Int,
    val action: String,
    val args: JsonObject,
    val selected: Boolean,
)

data class SharedAndexExtensionSettingsPage(
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
    val trailingArgs: JsonObject = JsonObject(emptyMap()),
    val sections: List<JsonObject>,
    val categories: List<SharedAndexExtensionSettingsCategory> = emptyList(),
)

data class SharedAndexExtensionSettingsCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val order: Int,
    val trailingIcon: String = "",
    val trailingAction: String = "",
    val trailingCategory: String = "",
    val trailingArgs: JsonObject = JsonObject(emptyMap()),
    val hidden: Boolean = false,
    val sections: List<JsonObject>,
)

data class SharedAndexExtensionMessageType(
    val id: String,
    val type: String,
    val extensionId: String,
    val extensionName: String,
    val title: String,
    val icon: String,
)

data class SharedAndexExtensionCustomMessage(
    val id: String,
    val type: String,
    val extensionId: String,
    val tree: JsonElement?,
)

data class SharedAndexExtensionError(
    val path: String,
    val extensionId: String,
    val phase: String,
    val message: String,
)

data class SharedAndexExtensionNotification(
    val message: String,
    val level: String,
)

data class SharedPiExtensionUiRequest(
    val callId: String,
    val method: String,
    val title: String,
    val message: String = "",
    val placeholder: String = "",
    val options: List<String> = emptyList(),
)

data class SharedAndexExtensionSnapshot(
    val apiVersion: Int = 2,
    val version: Long = 0,
    val extensions: List<SharedAndexExtensionInfo> = emptyList(),
    val surfaces: List<SharedAndexExtensionSurface> = emptyList(),
    val components: List<SharedAndexExtensionComponent> = emptyList(),
    val composerMenuItems: List<SharedAndexExtensionComposerMenuItem> = emptyList(),
    val settings: List<SharedAndexExtensionSettingsPage> = emptyList(),
    val messageTypes: List<SharedAndexExtensionMessageType> = emptyList(),
    val customMessages: List<SharedAndexExtensionCustomMessage> = emptyList(),
    val eventNames: Set<String> = emptySet(),
    val errors: List<SharedAndexExtensionError> = emptyList(),
) {
    fun surfacesAt(slot: String): List<SharedAndexExtensionSurface> =
        surfaces.filter { it.slot == slot }.sortedWith(
            compareBy<SharedAndexExtensionSurface> { it.order }.thenBy { it.id }
        )

    fun componentsAt(target: String): List<SharedAndexExtensionComponent> =
        components.filter { it.target == target }.sortedWith(
            compareBy<SharedAndexExtensionComponent> { it.order }.thenBy { it.id }
        )
}

class SharedAndexExtensionManager(
    private val bridge: SharedPiBridgeClient,
    private val hostHandler: suspend (String, JsonObject) -> JsonObject,
) {
    private val mutex = Mutex()
    private val uiRequestMutex = Mutex()
    private val pendingUiRequests = ArrayDeque<SharedPiExtensionUiRequest>()
    private val _notifications = MutableSharedFlow<SharedAndexExtensionNotification>(
        extraBufferCapacity = 8,
    )
    private val _piUiRequest = MutableStateFlow<SharedPiExtensionUiRequest?>(null)
    var snapshot: SharedAndexExtensionSnapshot = SharedAndexExtensionSnapshot()
        private set
    var error: String = ""
        private set

    val notifications: SharedFlow<SharedAndexExtensionNotification> = _notifications.asSharedFlow()
    val piUiRequest: StateFlow<SharedPiExtensionUiRequest?> = _piUiRequest.asStateFlow()

    suspend fun refresh(
        context: JsonObject = JsonObject(emptyMap()),
    ): SharedAndexExtensionSnapshot = mutex.withLock {
        val response = bridge.getAndexExtensions(context, ::handleEvent)
        parseResponse(response)
    }

    suspend fun reload(
        context: JsonObject = JsonObject(emptyMap()),
    ): SharedAndexExtensionSnapshot = mutex.withLock {
        val response = bridge.reloadAndexExtensions(context, ::handleEvent)
        parseResponse(response)
    }

    suspend fun invokeAction(
        extensionId: String,
        action: String,
        args: JsonObject = JsonObject(emptyMap()),
        context: JsonObject = JsonObject(emptyMap()),
    ): SharedAndexExtensionSnapshot = mutex.withLock {
        val response = bridge.invokeAndexExtensionAction(
            extensionId = extensionId,
            action = action,
            args = args,
            context = context,
            onEvent = ::handleEvent,
        )
        parseResponse(response)
    }

    suspend fun dispatchEvent(
        event: String,
        data: JsonObject = JsonObject(emptyMap()),
        context: JsonObject = JsonObject(emptyMap()),
    ): JsonObject {
        if (event !in snapshot.eventNames) return JsonObject(emptyMap())
        return mutex.withLock {
            val response = bridge.dispatchAndexExtensionEvent(
                event = event,
                data = data,
                context = context,
                onEvent = ::handleEvent,
            )
            response.objectOrNull("snapshot")?.let {
                snapshot = parseSharedAndexExtensionSnapshot(it)
            }
            response
        }
    }

    suspend fun subscribe(
        onInvalidated: suspend () -> Unit,
    ) {
        bridge.subscribeAndexExtensions { event, payload ->
            handleEvent(event, payload)
            if (event == "andex_invalidated") onInvalidated()
        }
    }

    suspend fun respondToPiExtensionUiRequest(
        callId: String,
        value: JsonElement?,
    ) {
        val request = uiRequestMutex.withLock {
            val current = _piUiRequest.value
            if (current?.callId != callId) return
            _piUiRequest.value = pendingUiRequests.removeFirstOrNull()
            current
        }
        bridge.sendAndexHostResult(
            callId = request.callId,
            result = JsonObject(mapOf("value" to (value ?: JsonNull))),
        )
    }

    private suspend fun handleEvent(event: String, payload: JsonObject) {
        when (event) {
            "andex_notification" -> _notifications.emit(
                SharedAndexExtensionNotification(
                    message = payload.string("message"),
                    level = payload.string("level").ifBlank { "info" },
                )
            )
            "andex_host_call" -> {
                val callId = payload.string("call_id")
                val method = payload.string("method")
                val args = payload.objectOrNull("args") ?: JsonObject(emptyMap())
                if (method == "pi_extension_notify") {
                    _notifications.emit(
                        SharedAndexExtensionNotification(
                            message = args.string("message"),
                            level = args.string("type").ifBlank { "info" },
                        )
                    )
                    bridge.sendAndexHostResult(
                        callId = callId,
                        result = JsonObject(mapOf("notified" to JsonPrimitive(true))),
                    )
                    return
                }
                if (method in SharedPiExtensionInteractiveUiMethods) {
                    val request = SharedPiExtensionUiRequest(
                        callId = callId,
                        method = method,
                        title = args.string("title"),
                        message = args.string("message"),
                        placeholder = args.string("placeholder"),
                        options = (args["options"] as? JsonArray)
                            .orEmpty()
                            .mapNotNull { option ->
                                (option as? JsonPrimitive)
                                    ?.contentOrNull
                                    ?.takeIf(String::isNotBlank)
                            },
                    )
                    uiRequestMutex.withLock {
                        if (_piUiRequest.value == null) {
                            _piUiRequest.value = request
                        } else {
                            pendingUiRequests.addLast(request)
                        }
                    }
                    return
                }
                val result = runCatching { hostHandler(method, args) }
                bridge.sendAndexHostResult(
                    callId = callId,
                    result = result.getOrDefault(JsonObject(emptyMap())),
                    error = result.exceptionOrNull()?.message.orEmpty(),
                )
            }
        }
    }

    private fun parseResponse(response: JsonObject): SharedAndexExtensionSnapshot {
        snapshot = parseSharedAndexExtensionSnapshot(response.objectOrNull("snapshot"))
        error = (response["errors"] as? JsonArray)
            .orEmpty()
            .mapNotNull { (it as? JsonObject)?.string("error")?.takeIf(String::isNotBlank) }
            .take(3)
            .joinToString("; ")
        return snapshot
    }
}

private val SharedPiExtensionInteractiveUiMethods = setOf(
    "pi_extension_select",
    "pi_extension_confirm",
    "pi_extension_input",
)

internal fun parseSharedAndexExtensionSnapshot(
    json: JsonObject?,
): SharedAndexExtensionSnapshot {
    if (json == null) return SharedAndexExtensionSnapshot()
    return SharedAndexExtensionSnapshot(
        apiVersion = json.int("api_version") ?: 2,
        version = json.long("version") ?: 0,
        extensions = json.objects("extensions").map { item ->
            SharedAndexExtensionInfo(
                id = item.string("id"),
                name = item.string("name"),
                path = item.string("path"),
            )
        },
        surfaces = json.objects("surfaces").map { item ->
            SharedAndexExtensionSurface(
                id = item.string("id"),
                extensionId = item.string("extension_id"),
                extensionName = item.string("extension_name"),
                slot = item.string("slot"),
                order = item.int("order") ?: 0,
                tree = item["tree"],
            )
        },
        components = json.objects("components").map { item ->
            SharedAndexExtensionComponent(
                id = item.string("id"),
                extensionId = item.string("extension_id"),
                extensionName = item.string("extension_name"),
                target = item.string("target"),
                mode = item.string("mode").ifBlank { "wrap" },
                order = item.int("order") ?: 0,
                tree = item["tree"],
            )
        },
        composerMenuItems = json.objects("composer_menu_items").map { item ->
            SharedAndexExtensionComposerMenuItem(
                id = item.string("id"),
                localId = item.string("local_id"),
                extensionId = item.string("extension_id"),
                extensionName = item.string("extension_name"),
                title = item.string("title"),
                subtitle = item.string("subtitle"),
                icon = item.string("icon").ifBlank { "extension" },
                order = item.int("order") ?: 0,
                action = item.string("action"),
                args = item["args"] as? JsonObject ?: JsonObject(emptyMap()),
                selected = item.boolean("selected"),
            )
        },
        settings = json.objects("settings").map { item ->
            SharedAndexExtensionSettingsPage(
                id = item.string("id"),
                localId = item.string("local_id"),
                extensionId = item.string("extension_id"),
                extensionName = item.string("extension_name"),
                title = item.string("title"),
                subtitle = item.string("subtitle"),
                icon = item.string("icon").ifBlank { "settings" },
                order = item.int("order") ?: 0,
                trailingIcon = item.string("trailing_icon").ifBlank { item.string("trailingIcon") },
                trailingAction = item.string("trailing_action").ifBlank { item.string("trailingAction") },
                trailingCategory = item.string("trailing_category").ifBlank { item.string("trailingCategory") },
                trailingArgs = item["trailing_args"] as? JsonObject
                    ?: item["trailingArgs"] as? JsonObject ?: JsonObject(emptyMap()),
                sections = item.objects("sections"),
                categories = item.objects("categories").map { category ->
                    SharedAndexExtensionSettingsCategory(
                        id = category.string("id"),
                        title = category.string("title"),
                        subtitle = category.string("subtitle"),
                        icon = category.string("icon").ifBlank { "settings" },
                        order = category.int("order") ?: 0,
                        trailingIcon = category.string("trailing_icon").ifBlank { category.string("trailingIcon") },
                        trailingAction = category.string("trailing_action").ifBlank { category.string("trailingAction") },
                        trailingCategory = category.string("trailing_category").ifBlank { category.string("trailingCategory") },
                        trailingArgs = category["trailing_args"] as? JsonObject
                            ?: category["trailingArgs"] as? JsonObject ?: JsonObject(emptyMap()),
                        hidden = category["hidden"]?.jsonPrimitive?.booleanOrNull ?: false,
                        sections = category.objects("sections"),
                    )
                }.sortedWith(compareBy<SharedAndexExtensionSettingsCategory> { it.order }.thenBy { it.id }),
            )
        },
        messageTypes = json.objects("message_types").map { item ->
            SharedAndexExtensionMessageType(
                id = item.string("id"),
                type = item.string("type"),
                extensionId = item.string("extension_id"),
                extensionName = item.string("extension_name"),
                title = item.string("title"),
                icon = item.string("icon").ifBlank { "extension" },
            )
        },
        customMessages = json.objects("custom_messages").map { item ->
            SharedAndexExtensionCustomMessage(
                id = item.string("id"),
                type = item.string("type"),
                extensionId = item.string("extension_id"),
                tree = item["tree"],
            )
        },
        eventNames = (json["event_names"] as? JsonArray)
            .orEmpty()
            .mapNotNull { it.jsonPrimitive.contentOrNull }
            .toSet(),
        errors = json.objects("errors").map { item ->
            SharedAndexExtensionError(
                path = item.string("path"),
                extensionId = item.string("extension_id"),
                phase = item.string("phase"),
                message = item.string("error"),
            )
        },
    )
}

private fun JsonObject.string(name: String): String =
    get(name)?.jsonPrimitive?.contentOrNull.orEmpty()

private fun JsonObject.int(name: String): Int? =
    get(name)?.jsonPrimitive?.intOrNull

private fun JsonObject.long(name: String): Long? =
    get(name)?.jsonPrimitive?.longOrNull

private fun JsonObject.boolean(name: String): Boolean =
    get(name)?.jsonPrimitive?.booleanOrNull ?: false

private fun JsonObject.objectOrNull(name: String): JsonObject? = get(name) as? JsonObject

private fun JsonObject.objects(name: String): List<JsonObject> =
    (get(name) as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
