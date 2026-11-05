package com.android.andex.termux

/** Supported `am start` arguments. Intent dispatch is performed by the host app's Context. */
internal data class EmbeddedAndroidIntent(
    val action: String?,
    val data: String?,
    val packageName: String?,
    val component: String?,
    val mimeType: String?,
    val stringExtras: Map<String, String>,
)

internal fun parseEmbeddedAndroidIntent(arguments: List<String>): EmbeddedAndroidIntent {
    require(arguments.firstOrNull() in setOf("start", "start-activity")) {
        "Embedded am supports start/start-activity. Use andex-phone-control for screen control."
    }
    var action: String? = null
    var data: String? = null
    var packageName: String? = null
    var component: String? = null
    var mimeType: String? = null
    val extras = mutableMapOf<String, String>()
    var index = 1
    fun value(): String = arguments.getOrNull(++index) ?: error("Missing am argument value.")
    while (index < arguments.size) {
        when (val option = arguments[index]) {
            "-a" -> action = value()
            "-d" -> data = value()
            "-p" -> packageName = value()
            "-n" -> component = value()
            "-t" -> mimeType = value()
            "--es" -> {
                val key = value()
                extras[key] = value()
            }
            "--user" -> require(value() in setOf("0", "current")) { "Only the current Android user is supported." }
            "-W", "--activity-new-task" -> Unit
            else -> error("Unsupported embedded am option: $option. Use andex-launch <package> or andex-phone-control.")
        }
        index++
    }
    require(action != null || data != null || packageName != null || component != null) { "Missing Intent target." }
    return EmbeddedAndroidIntent(action, data, packageName, component, mimeType, extras)
}
