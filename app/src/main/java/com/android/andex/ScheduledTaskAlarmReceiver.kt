package com.android.andex

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.android.andex.data.ScheduledTaskScheduler

class ScheduledTaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ScheduledTaskScheduler.ActionRunScheduledTask) return
        val taskId = intent.getStringExtra(ScheduledTaskScheduler.ExtraTaskId).orEmpty()
        if (taskId.isBlank()) return
        val pendingResult = goAsync()
        context.andexRuntime.handleScheduledTaskAlarm(taskId, pendingResult)
    }
}
