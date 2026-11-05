package com.android.andex.phonecontrol

import android.content.Context

class PhoneControlPreferences(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("phone_control", Context.MODE_PRIVATE)

    init {
        // 0.9 repair: authorized system channels used to be hidden by a second default-off switch.
        if (!preferences.getBoolean("andex_channels_default_v1", false)) {
            preferences.edit().putBoolean("enabled", true).putBoolean("andex_channels_default_v1", true).apply()
        }
    }

    var enabled: Boolean
        get() = preferences.getBoolean("enabled", true)
        set(value) { preferences.edit().putBoolean("enabled", value).apply() }

    var accessibilityPreviouslyGranted: Boolean
        get() = preferences.getBoolean("accessibility_previously_granted", false)
        set(value) { preferences.edit().putBoolean("accessibility_previously_granted", value).apply() }

    var embeddedTermux: Boolean
        get() = preferences.getBoolean("embedded_termux", true)
        set(value) { preferences.edit().putBoolean("embedded_termux", value).apply() }

    var adbEndpoint: String
        get() = preferences.getString("adb_endpoint", "").orEmpty()
        set(value) { preferences.edit().putString("adb_endpoint", value).apply() }

    var adbConnected: Boolean
        get() = preferences.getBoolean("adb_connected", false)
        set(value) { preferences.edit().putBoolean("adb_connected", value).apply() }
}
