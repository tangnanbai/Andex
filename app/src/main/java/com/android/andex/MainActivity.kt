package com.android.andex

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.andex.ui.AndexApp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : AppCompatActivity() {
    private var externalSessionId by androidx.compose.runtime.mutableStateOf<String?>(null)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val background = getSharedPreferences("window_appearance", MODE_PRIVATE).getInt("background", android.graphics.Color.BLACK)
        applyAndexWindowBackground(background)
        enableEdgeToEdge()
        window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN,
        )
        acceptTaskIntent(intent)
        setContent {
            val focus = androidx.compose.ui.platform.LocalFocusManager.current
            val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
            val orientation = androidx.compose.ui.platform.LocalConfiguration.current.orientation
            androidx.compose.runtime.LaunchedEffect(orientation) {
                focus.clearFocus(force = true)
                keyboard?.hide()
            }
            AndexApp(
                onNotificationPermissionRequested = ::maybeRequestNotificationPermission,
                externalSessionId = externalSessionId,
                onExternalSessionConsumed = { externalSessionId = null },
            )
        }
        requestAndPrepareTermuxStorage()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptTaskIntent(intent)
    }

    private fun acceptTaskIntent(intent: Intent?) {
        if (intent?.action == com.android.andex.overlay.OverlayNavigation.OPEN_TASK) {
            externalSessionId = intent.getStringExtra(
                com.android.andex.overlay.OverlayNavigation.SESSION_EXTRA,
            )?.takeIf { it.isNotBlank() && it.length <= 200 }
        }
    }

    private fun requestAndPrepareTermuxStorage() {
        val preferences = getSharedPreferences("first_launch_permissions", MODE_PRIVATE)
        if (preferences.getBoolean("storage_requested", false)) return
        preferences.edit().putBoolean("storage_requested", true).apply()
        // Application startup already initializes Termux and prepares ~/storage.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                runCatching {
                    startActivity(Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        android.net.Uri.parse("package:$packageName"),
                    ))
                }
            }
        } else if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                4102,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        (application as AndexApplication).runtime.nativeModManager.notifyUiStable()
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
