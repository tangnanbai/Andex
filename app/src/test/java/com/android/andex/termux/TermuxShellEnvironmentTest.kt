package com.android.andex.termux

import com.android.andex.data.TermuxEnvironmentVariable
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class TermuxShellEnvironmentTest {
    @Test
    fun dispatchEnvironmentIncludesTermuxAndCommonUserPathsWithoutStartupCapture() {
        val script = StringBuilder().also { builder ->
            appendTermuxShellEnvironment(
                builder = builder,
                includeShellStartupPath = false,
            )
        }.toString()

        assertTrue(script.contains("export PREFIX='${TermuxContract.PrefixDirectory}'"))
        assertTrue(script.contains("\$PREFIX/bin"))
        assertTrue(script.contains("\$HOME/.local/bin"))
        assertTrue(script.contains("\$HOME/.cargo/bin"))
        assertFalse(script.contains("andex_capture_shell_path"))
    }

    @Test
    fun managedRunnerEnvironmentCapturesLoginAndInteractiveShellPath() {
        val script = StringBuilder().also { builder ->
            appendTermuxShellEnvironment(
                builder = builder,
                includeShellStartupPath = true,
            )
        }.toString()

        assertTrue(script.contains("timeout 3 \"\$PREFIX/bin/bash\" \"\$mode\""))
        assertTrue(script.contains("andex_capture_shell_path -lc"))
        assertTrue(script.contains("andex_capture_shell_path -ic"))
    }

    @Test
    fun embeddedBridgeWinsAfterStartupAndCustomPathWhileExternalTermuxKeepsOverride() {
        val gitBashCandidates = System.getenv("PATH").orEmpty().split(File.pathSeparator)
            .filter { File(it, "git.exe").isFile }
            .map { File(File(it).parentFile, "bin/bash.exe") }
        val bash = (listOf(File("C:/Program Files/Git/bin/bash.exe"), File("/bin/bash")) + gitBashCandidates)
            .firstOrNull { it.isFile }
        assumeTrue("This execution test needs Bash.", bash != null)
        val folder = Files.createTempDirectory("andex-shell-path").toFile()
        try {
            val guest = folder.absolutePath.replace('\\', '/').let {
                if (File.separatorChar == '\\') "/${it[0].lowercaseChar()}${it.substring(2)}" else it
            }
            val home = "$guest/home"
            val prefix = "$guest/usr"
            File(folder, "home/.local/bin").mkdirs()
            File(folder, "usr/bin").mkdirs()
            // Both login and interactive profiles simulate the package PATH winning.
            File(folder, "usr/bin/bash").apply {
                writeText("#!/bin/sh\nprintf '\\036%s' \"\$PREFIX/bin:/system/bin\"\n")
                setExecutable(true)
            }
            File(folder, "home/.local/bin/am").apply {
                writeText("#!/bin/sh\nprintf 'native-bridge'\n")
                setExecutable(true)
            }
            File(folder, "usr/bin/am").apply {
                writeText("#!/bin/sh\nprintf 'wrong-identity'\n")
                setExecutable(true)
            }
            for (startup in listOf(false, true)) {
                for (embedded in listOf(false, true)) {
                    val script = StringBuilder().also {
                        appendTermuxShellEnvironment(
                            it, startup,
                            listOf(TermuxEnvironmentVariable("PATH", "$prefix/bin:/usr/bin:/bin")),
                        )
                    }.toString()
                        .replace(TermuxContract.HomeDirectory, home)
                        .replace(TermuxContract.PrefixDirectory, prefix) + "\nam\n"
                    val file = File(folder, "check.sh").apply { writeText(script) }
                    val process = ProcessBuilder(bash!!.path, file.path.replace('\\', '/')).apply {
                        if (embedded) environment()["ANDEX_APP_PACKAGE"] = "com.android.andex.debug"
                        else environment().remove("ANDEX_APP_PACKAGE")
                        redirectErrorStream(true)
                    }.start()
                    val output = process.inputStream.bufferedReader().readText()
                    assertEquals(output, 0, process.waitFor())
                    assertEquals(if (embedded) "native-bridge" else "wrong-identity", output.trim())
                }
            }
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test
    fun onlyAndexWorkspaceDirectoriesArePreparedAutomatically() {
        assertTrue(
            shouldPrepareTermuxWorkingDirectory("${TermuxContract.HomeDirectory}/.andex/workspace")
        )
        assertTrue(
            shouldPrepareTermuxWorkingDirectory("${TermuxContract.HomeDirectory}/.andex/workspaces/session")
        )
        assertFalse(shouldPrepareTermuxWorkingDirectory(TermuxContract.HomeDirectory))
        assertFalse(shouldPrepareTermuxWorkingDirectory("${TermuxContract.HomeDirectory}/projects/demo"))
    }
}
