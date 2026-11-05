package com.android.andex.data.pi

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PiRuntimeFailureTest {
    @Test
    fun deniedTracerMustNotTriggerNodePackageInstallation() {
        assertNotNull(piRuntimeFailure("proot error: ptrace(TRACEME): Permission denied\n"))
    }

    @Test
    fun missingNodeCanStillBeInstalled() {
        assertNull(piRuntimeFailure("/bin/sh: node: not found"))
        assertNull(piRuntimeFailure(""))
    }
}
