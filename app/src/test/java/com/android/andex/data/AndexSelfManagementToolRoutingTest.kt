package com.android.andex.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndexSelfManagementToolRoutingTest {
    @Test
    fun scheduledTaskToolIsRoutedByPiHostExecutor() {
        assertTrue(AndexToolExecutor.supports("andex_scheduled_task_manage"))
    }

    @Test
    fun piExtensionToolIsRoutedByPiHostExecutor() {
        assertTrue(AndexToolExecutor.supports("andex_extension_manage"))
    }

    @Test
    fun createExtensionSkillIsAvailableWithoutDefaultSelection() {
        assertTrue(CreateExtensionSkillId in BuiltInAgentSkillIds)
        assertFalse(CreateExtensionSkillId in AppSettings().defaultSelectedSkillIds)
    }
}
