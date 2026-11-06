package com.android.andex

import android.app.Activity
import android.app.ActivityManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build

/** Keep the window and the system's task/rotation backdrop on the same opaque color. */
fun Activity.applyAndexWindowBackground(background: Int) {
    val color = background or Color.BLACK
    window.setBackgroundDrawable(ColorDrawable(color))
    window.decorView.setBackgroundColor(color)
    if (Build.VERSION.SDK_INT >= 33) {
        setTaskDescription(ActivityManager.TaskDescription.Builder()
            .setLabel("Andex")
            .setIcon(R.mipmap.ic_launcher)
            .setPrimaryColor(color)
            .setBackgroundColor(color)
            .setStatusBarColor(color)
            .setNavigationBarColor(color)
            .build())
    }
}
