package com.android.andex.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSnapshotProtectionTest {
    @Test
    fun switchingToUnloadedSessionPreservesStoredMessages() {
        assertTrue(shouldPreserveUnloadedChatMessages(
            PersistedChatWriteIntent.SyncSnapshot, true, true, 36, 36,
        ))
        assertTrue(shouldPreserveUnloadedChatMessages(
            PersistedChatWriteIntent.SyncSnapshot, false, true, 0, 36,
        ))
    }

    @Test
    fun explicitEmptyHistoryAndImportStillReplaceMessages() {
        assertFalse(shouldPreserveUnloadedChatMessages(
            PersistedChatWriteIntent.SyncSnapshot, true, true, 0, 36,
        ))
        assertFalse(shouldPreserveUnloadedChatMessages(
            PersistedChatWriteIntent.ReplaceFromImport, true, true, 36, 36,
        ))
        assertFalse(shouldPreserveUnloadedChatMessages(
            PersistedChatWriteIntent.SyncSnapshot, true, false, 37, 36,
        ))
    }
}
