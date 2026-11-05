package com.android.andex.data.chatdb

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

object AndroidChatHistoryDatabaseFactory {
    @Volatile
    private var instance: ChatHistoryDatabase? = null

    fun getInstance(context: Context): ChatHistoryDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
            context.applicationContext,
            ChatHistoryDatabase::class.java,
            "andex_chat_history.db",
        ).setDriver(BundledSQLiteDriver())
            .addMigrations(*ChatHistoryMigrations)
            .build()
            .also { instance = it }
    }
}
