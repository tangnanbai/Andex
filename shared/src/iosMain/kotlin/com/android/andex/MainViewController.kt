package com.android.andex

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.uikit.OnFocusBehavior
import com.android.andex.platform.currentPlatformCapabilities
import com.android.andex.runtime.IosAlpineRuntime
import com.android.andex.runtime.NativeRuntimeHost
import com.android.andex.ui.IosComposeApp
import com.android.andex.data.createIosAndexSettingsStore
import com.android.andex.data.createIosAndexChatHistoryDatabase
import com.android.andex.platform.IosPlatformServices
import com.android.andex.platform.IosNativeSettingsHost

fun MainViewController(runtimeHost: NativeRuntimeHost): platform.UIKit.UIViewController {
    val runtime = IosAlpineRuntime(runtimeHost)
    val settingsStore = createIosAndexSettingsStore()
    val chatHistoryDatabase = createIosAndexChatHistoryDatabase()
    val platformServices = IosPlatformServices(runtimeHost)
    return ComposeUIViewController(configure = {
        // Chat handles IME insets locally; never pan the entire iPad split layout.
        onFocusBehavior = OnFocusBehavior.DoNothing
    }) {
        IosComposeApp(
            runtime = runtime,
            capabilities = currentPlatformCapabilities,
            settingsStore = settingsStore,
            chatHistoryDatabase = chatHistoryDatabase,
            platformServices = platformServices,
            nativeSettingsHost = IosNativeSettingsHost,
        )
    }
}
