package com.android.andex.data

import com.android.andex.data.chatdb.ChatHistoryDatabase
import com.android.andex.data.chatdb.createIosChatHistoryDatabase
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSFileManager
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
private fun iosApplicationSupportDirectory(): String {
    val path = NSHomeDirectory() + "/Library/Application Support"
    NSFileManager.defaultManager.createDirectoryAtPath(
        path = path,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    return path
}

fun createIosAndexSettingsStore(): AndexSettingsStore =
    createAndexSettingsStore(
        iosApplicationSupportDirectory() + "/andex_settings.preferences_pb",
    )

fun createIosAndexChatHistoryDatabase(): ChatHistoryDatabase =
    createIosChatHistoryDatabase(
        iosApplicationSupportDirectory() + "/andex_chat_history.db",
    )
