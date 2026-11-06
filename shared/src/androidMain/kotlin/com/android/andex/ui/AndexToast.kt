package com.android.andex.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import com.android.andex.ui.theme.AndexOnSurface
import com.android.andex.ui.theme.AndexSurfaceHigh

/** Shared positioning for UI, extension and runtime notifications. */
object AndexToast {
    const val LENGTH_SHORT = Toast.LENGTH_SHORT
    const val LENGTH_LONG = Toast.LENGTH_LONG

    @Suppress("DEPRECATION")
    fun makeText(context: Context, text: CharSequence, duration: Int): Toast {
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        fun dp(value: Int) = (value * density).toInt()
        val label = TextView(context.applicationContext).apply {
            this.text = text
            textSize = 14f
            setTextColor(AndexOnSurface.toArgb())
            gravity = Gravity.CENTER
            maxWidth = minOf(dp(360), metrics.widthPixels - dp(48))
            setPadding(dp(20), dp(12), dp(20), dp(12))
            background = GradientDrawable().apply {
                setColor(AndexSurfaceHigh.toArgb())
                cornerRadius = dp(24).toFloat()
            }
            elevation = dp(4).toFloat()
        }
        return Toast(context.applicationContext).apply {
            this.duration = duration
            view = label
            // Center at 2/3 screen height; message height doesn't change the anchor.
            setGravity(Gravity.CENTER, 0, metrics.heightPixels / 6)
        }
    }
}
