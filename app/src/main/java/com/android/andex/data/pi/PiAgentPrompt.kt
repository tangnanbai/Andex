package com.android.andex.data.pi

import com.android.andex.data.effectiveSystemPrompt
import com.android.andex.data.AppSettings
import com.android.andex.data.LocalRuntimeId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DynamicPromptPlaceholderRegex = Regex("""\{\{\s*([A-Za-z0-9_-]+)\s*\}\}""")

internal fun buildPiAgentInstructions(
    settings: AppSettings,
    workspaceDirectory: String,
    runtimeId: LocalRuntimeId,
    agentModeEnabled: Boolean,
    chromeEnabled: Boolean = false,
): String = buildString {
    val configuredPrompt = expandDynamicPromptPlaceholders(settings.effectiveSystemPrompt()).trim()
    if (configuredPrompt.isNotBlank()) {
        append(configuredPrompt)
        append("\n\n")
    }
    append(
        "You are running inside Andex on Android. " +
            "The current local runtime is ${runtimeId.storageValue} and its session cwd is $workspaceDirectory. " +
            "Andex keeps Alpine and Termux workspaces independent when the runtime changes. " +
            "User-uploaded files are placed under uploads/; use read on the provided path when image or file contents are needed. " +
            "Andex-owned configuration, Skill, runtime, Extension, Agent Mode, scheduled-task, and developer operations are exposed only through available andex_* tools. " +
            "Never modify LLM provider credentials or model configuration through self-management tools. " +
            "Only claim device actions or command results that were actually observed. " +
            "The phone_control tool operates the real phone independently of the isolated Agent Mode display. " +
            "Always call phone_control action=status before claiming Android control is unavailable. " +
            "For opening apps, call phone_control backend=termux action=launch package=<package>; " +
            "this invokes Android with Andex's own identity and needs neither root nor Shizuku. " +
            "Termux shell runs as Andex's ordinary app UID, not com.termux or shell(2000). " +
            "Never use raw am, input, cmd or monkey to impersonate another UID. " +
            "When accessibility_ready is true, call phone_control backend=accessibility for snapshot, tap, swipe, click, text or key. " +
            "Snapshot bounds and center are normalized 0..1000, matching tap x,y and swipe x1,y1,x2,y2; no manual conversion is needed. " +
            "Use find_text or scroll_to_text before blind swipes, and batch for multi-step UI actions. " +
            "Accessibility actions wait for UI updates. Use expected_package to avoid controlling Andex after a foreground change. " +
            "For app searches, entering text only updates the input; it does not complete the task. " +
            "Submit with phone_control action=key key=enter or click the visible search button, then inspect the result list. " +
            "After every action use its returned snapshot; request another snapshot only if the UI is still loading. " +
            "Batch known independent steps instead of separate shell processes or fixed sleeps. " +
            "Continue clicking/opening relevant results until the user's requested information or destination is observed. " +
            "Before claiming completion, verify the final screen with a fresh snapshot or screenshot and report the actual findings. " +
            "A successful tap/text/launch only confirms that individual action, never the whole task. " +
            "If accessibility is disconnected, explicitly tell the user to re-enable Andex accessibility; do not claim success. " +
            "When adb_ready is true, call backend=adb for shell, screen input and screenshots. " +
            "These channels are available without selecting the isolated-display Agent Mode. " +
            "Use the user's granted channel; do not suggest Shizuku when an authorized accessibility or ADB channel already works. " +
            "If phone_control is absent from the tool list in the Termux runtime, use bash to run " +
            "andex-phone-control '{\"action\":\"status\"}' and the same JSON interface for subsequent actions. " +
            "The embedded am and termux-am commands support native activity launch through Andex; " +
            "they do not need the external Termux am.sock service. " +
            "System accessibility grants and wireless ADB pairing still require the user."
    )
    if (agentModeEnabled) {
        append(
            "\n\nAgent Mode is enabled for this chat. Use agent_display only when operating the isolated Android virtual display is required. " +
                "Tap and swipe coordinates use the normalized 0..1000 range on each axis, not screenshot or display pixels: " +
                "x = pixel_x / image_width * 1000, y = pixel_y / image_height * 1000. " +
                "Before using text, tap the target field and confirm in the screenshot that it is focused."
        )
    }
    if (chromeEnabled) {
        append(
            "\n\nThe chat has enabled the browser tool (Chrome Extension tool). Prefer selectors and DOM-reading actions, and use coordinates only as a fallback."
        )
    }
}

private fun expandDynamicPromptPlaceholders(
    prompt: String,
    now: ZonedDateTime = ZonedDateTime.now(),
): String {
    if (!prompt.contains("{{")) return prompt
    val values = mapOf(
        "current_datetime" to now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        "current_date" to now.toLocalDate().toString(),
        "current_time" to now.toLocalTime().withNano(0).toString(),
        "timezone" to now.zone.id,
        "unix_timestamp" to now.toEpochSecond().toString(),
    )
    return DynamicPromptPlaceholderRegex.replace(prompt) { match ->
        values[match.groupValues[1].lowercase(Locale.US)] ?: match.value
    }
}
