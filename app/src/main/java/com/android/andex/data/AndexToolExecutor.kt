package com.android.andex.data

import com.android.andex.runtime.RuntimeRouter
import org.json.JSONArray
import org.json.JSONObject

data class AndexToolExecutionResult(
    val toolName: String,
    val argumentsJson: String,
    val rawOutput: String,
    val visibleOutput: String = AndexToolExecutor.sanitizeToolOutputForConversation(toolName, rawOutput),
) {
    val isError: Boolean = !AndexToolExecutor.inferToolOutputOk(visibleOutput)
}

/**
 * Adapter for Andex-owned host capabilities only. Pi Coding Agent owns all
 * filesystem, shell, search, Skill, Extension, and image-reading mechanics.
 */
class AndexToolExecutor(
    private val runtimeRouter: RuntimeRouter,
    private val agentModeController: AgentModeController? = null,
    private val phoneControlController: com.android.andex.phonecontrol.PhoneControlController? = null,
    private val webResearchTool: WebResearchTool? = null,
    private val archiveTool: ArchiveTool? = null,
) {
    val phoneControlEnabled: Boolean
        get() = phoneControlController != null
    val webResearchEnabled: Boolean get() = webResearchTool != null
    val archiveEnabled: Boolean get() = archiveTool != null
    suspend fun execute(
        settings: AppSettings,
        workspaceDirectory: String,
        termuxWorkspaceDirectory: String,
        toolName: String,
        argumentsJson: String,
        selfManagementTool: AndexSelfManagementTool? = null,
        agentModeEnabled: Boolean = false,
        currentRuntimeId: LocalRuntimeId = settings.defaultRuntimeId ?: LocalRuntimeId.Alpine,
        onRuntimeChanged: suspend (LocalRuntimeId) -> Unit = {},
        onProgress: (suspend (String) -> Unit)? = null,
    ): AndexToolExecutionResult {
        val rawOutput = when (toolName) {
            "andex_archive" -> archiveTool?.execute(argumentsJson) ?: unavailableToolOutput(toolName)
            in WebResearchTool.names -> webResearchTool?.execute(toolName, argumentsJson) ?: unavailableToolOutput(toolName)
            "phone_control" -> phoneControlController?.execute(argumentsJson) ?: unavailableToolOutput(toolName)
            "agent_display" -> if (agentModeEnabled) {
                agentModeController?.execute(
                    settings = settings,
                    workspaceDirectory = workspaceDirectory,
                    termuxWorkspaceDirectory = termuxWorkspaceDirectory,
                    argumentsJson = argumentsJson,
                ) ?: unavailableToolOutput(toolName)
            } else {
                JSONObject()
                    .put("ok", false)
                    .put("errmsg", "Agent Mode is not enabled for this chat.")
                    .toString()
            }

            "andex_runtime_manage" -> executeRuntimeManage(
                settings = settings,
                currentRuntimeId = currentRuntimeId,
                workspaceDirectory = workspaceDirectory,
                termuxWorkspaceDirectory = termuxWorkspaceDirectory,
                argumentsJson = argumentsJson,
                onRuntimeChanged = onRuntimeChanged,
            )

            in SelfManagementToolNames -> selfManagementTool?.execute(
                toolName = toolName,
                argumentsJson = argumentsJson,
            ) ?: unavailableToolOutput(toolName)

            else -> JSONObject()
                .put("ok", false)
                .put("error", "Unknown Andex host tool '$toolName'.")
                .toString()
        }
        return AndexToolExecutionResult(toolName, argumentsJson, rawOutput)
    }

    private suspend fun executeRuntimeManage(
        settings: AppSettings,
        currentRuntimeId: LocalRuntimeId,
        workspaceDirectory: String,
        termuxWorkspaceDirectory: String,
        argumentsJson: String,
        onRuntimeChanged: suspend (LocalRuntimeId) -> Unit,
    ): String {
        val arguments = runCatching { JSONObject(argumentsJson) }.getOrNull()
            ?: return JSONObject().put("ok", false).put("errmsg", "Invalid JSON arguments.").toString()
        val action = arguments.optString("action").trim()
        if (action == "status") {
            val states = LocalRuntimeId.entries.associateWith { runtimeId ->
                runtimeRouter.runtimeById(runtimeId).inspectSetup()
            }
            return JSONObject().apply {
                put("ok", true)
                put("action", "status")
                put("runtime", currentRuntimeId.storageValue)
                put("cwd", runtimeCwd(currentRuntimeId, workspaceDirectory, termuxWorkspaceDirectory))
                put(
                    "available",
                    JSONObject().apply {
                        states.forEach { (runtimeId, state) -> put(runtimeId.storageValue, state.isReady) }
                    },
                )
            }.toString()
        }
        if (action != "set") {
            return JSONObject().put("ok", false).put("errmsg", "action must be 'status' or 'set'.").toString()
        }
        val requested = LocalRuntimeId.fromStorage(arguments.optString("runtime"))
            ?: return JSONObject().put("ok", false).put("errmsg", "runtime must be 'alpine' or 'termux'.").toString()
        val setup = runtimeRouter.runtimeById(requested).inspectSetup()
        val enabled = settings.enabledRuntimeIds.isEmpty() || requested in settings.enabledRuntimeIds
        if (!setup.isReady || !enabled) {
            return JSONObject().apply {
                put("ok", false)
                put("errmsg", "${requested.displayName} runtime is unavailable.")
                put("detail", setup.detail)
                put("runtime", currentRuntimeId.storageValue)
                put("cwd", runtimeCwd(currentRuntimeId, workspaceDirectory, termuxWorkspaceDirectory))
            }.toString()
        }
        onRuntimeChanged(requested)
        return JSONObject().apply {
            put("ok", true)
            put("action", "set")
            put("runtime", requested.storageValue)
            put("cwd", runtimeCwd(requested, workspaceDirectory, termuxWorkspaceDirectory))
        }.toString()
    }

    companion object {
        val hostToolNames: Set<String> = setOf("agent_display", "phone_control", "andex_archive", *SelfManagementToolNames.toTypedArray()) + WebResearchTool.names

        fun supports(toolName: String): Boolean = toolName in hostToolNames

        fun hostToolDefinitions(
            selfManagementTool: AndexSelfManagementTool? = null,
            agentModeEnabled: Boolean = false,
            phoneControlEnabled: Boolean = false,
            webResearchEnabled: Boolean = false,
            archiveEnabled: Boolean = false,
        ): JSONArray = JSONArray().apply {
            selfManagementTool?.toolDefinitions()?.forEach { definition ->
                put(
                    flattenOpenAiToolDefinition(
                        definition = definition,
                        executionMode = if (
                            definition.optJSONObject("function")?.optString("name") == "andex_config_get"
                        ) "parallel" else "sequential",
                    ),
                )
            }
            if (agentModeEnabled) put(agentModeToolDefinition())
            if (phoneControlEnabled) put(com.android.andex.phonecontrol.PhoneControlController.toolDefinition())
            if (webResearchEnabled) WebResearchTool.definitions().forEach { put(it) }
            if (archiveEnabled) put(ArchiveTool.definition())
        }

        fun sanitizeToolOutputForConversation(toolName: String, output: String): String {
            if (toolName != "agent_display" && toolName != "phone_control") return output
            val parsed = runCatching { JSONObject(output) }.getOrNull() ?: return output
            if (!parsed.has("screenshot_base64")) return output
            parsed.remove("screenshot_base64")
            parsed.put("screenshot_injected_into_next_model_request", true)
            return parsed.toString()
        }

        fun inferToolOutputOk(output: String): Boolean {
            val parsed = runCatching { JSONObject(output) }.getOrNull() ?: return true
            return parsed.optBoolean("ok", !parsed.optBoolean("err", false))
        }
    }
}

private fun runtimeCwd(
    runtimeId: LocalRuntimeId,
    workspaceDirectory: String,
    termuxWorkspaceDirectory: String,
): String = if (runtimeId == LocalRuntimeId.Termux) termuxWorkspaceDirectory else workspaceDirectory

private val SelfManagementToolNames = setOf(
    "andex_config_get",
    "andex_config_set",
    "andex_skill_manage",
    "andex_termux_manage",
    "andex_runtime_manage",
    "andex_agent_mode_manage",
    "andex_scheduled_task_manage",
    "andex_extension_manage",
    "andex_developer_manage",
)

private fun unavailableToolOutput(toolName: String): String = JSONObject()
    .put("ok", false)
    .put("errmsg", "Host dependency for '$toolName' is not available.")
    .toString()

private fun flattenOpenAiToolDefinition(
    definition: JSONObject,
    executionMode: String,
): JSONObject {
    val function = definition.optJSONObject("function") ?: JSONObject()
    return JSONObject().apply {
        put("name", function.optString("name"))
        put("description", function.optString("description"))
        put("parameters", relaxStrictOptionalParameters(function.optJSONObject("parameters")))
        put("execution_mode", executionMode)
    }
}

private fun relaxStrictOptionalParameters(parameters: JSONObject?): JSONObject {
    val relaxed = JSONObject((parameters ?: JSONObject().put("type", "object")).toString())
    val properties = relaxed.optJSONObject("properties") ?: return relaxed
    val required = relaxed.optJSONArray("required") ?: return relaxed
    relaxed.put(
        "required",
        JSONArray().apply {
            for (index in 0 until required.length()) {
                val name = required.optString(index)
                if (name.isNotBlank() && !properties.optJSONObject(name).allowsNull()) put(name)
            }
        },
    )
    return relaxed
}

private fun JSONObject?.allowsNull(): Boolean = when (val type = this?.opt("type")) {
    "null" -> true
    is JSONArray -> (0 until type.length()).any { type.optString(it) == "null" }
    else -> false
}

private fun agentModeToolDefinition(): JSONObject = JSONObject().apply {
    put("name", "agent_display")
    put(
        "description",
        "Operate Andex Agent Mode on an isolated Android virtual display. Use this only when Agent Mode is selected in the chat composer. " +
            "tap/swipe coordinates are normalized 0..1000 on each axis, independent of resolution; values above 1000 are rejected. " +
            "Results report width/height (display pixels), image_width/image_height (screenshot pixels), " +
            "cursor_norm_x/cursor_norm_y (last touch point, normalized) and cursor_x/cursor_y (the same point in display pixels).",
    )
    put(
        "parameters",
        JSONObject().apply {
            put("type", "object")
            put(
                "properties",
                JSONObject().apply {
                    put("action", stringProperty("One of: list_apps, start, status, launch, tap, swipe, key, text, screenshot, stop."))
                    put("query", stringProperty("For list_apps: optional app label, package, or activity filter."))
                    put("include_system", booleanProperty("For list_apps: whether to include system apps."))
                    put("max_results", integerProperty("For list_apps: maximum number of apps to return."))
                    put("target", stringProperty("For launch: package name or exact app label."))
                    listOf("x", "y", "x1", "y1", "x2", "y2").forEach { key ->
                        val axis = if (key.startsWith("x")) "width" else "height"
                        put(
                            key,
                            integerProperty(
                                "For ${if (key.length == 1) "tap" else "swipe"}: normalized $axis coordinate in 0..1000 " +
                                    "(0 = left/top edge, 1000 = right/bottom edge), NOT screenshot or display pixels. " +
                                    "Convert a screenshot pixel with pixel / image_$axis * 1000.",
                            ),
                        )
                    }
                    put("duration_ms", integerProperty("For swipe: gesture duration in milliseconds (50..10000)."))
                    put("key", stringProperty("For key: Android key code name or number."))
                    put(
                        "text",
                        stringProperty(
                            "For text: text to insert into the focused field; any Unicode (Chinese, emoji, ...) is supported. " +
                                "Tap the field first and confirm it is focused. Fails if no window on the display has input focus.",
                        ),
                    )
                },
            )
            put("required", JSONArray().put("action"))
            put("additionalProperties", false)
        },
    )
    put("execution_mode", "sequential")
}

private fun stringProperty(description: String): JSONObject = JSONObject()
    .put("type", "string")
    .put("description", description)

private fun integerProperty(description: String): JSONObject = JSONObject()
    .put("type", "integer")
    .put("description", description)

private fun booleanProperty(description: String): JSONObject = JSONObject()
    .put("type", "boolean")
    .put("description", description)
