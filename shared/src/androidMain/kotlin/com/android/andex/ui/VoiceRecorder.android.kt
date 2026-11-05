package com.android.andex.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.*

@Composable
actual fun rememberVoiceRecorder(): VoiceRecorder {
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val recorder = remember(context) {
        AndroidVoiceRecorder(
            hasPermission = { context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED },
            requestPermission = { permission.launch(Manifest.permission.RECORD_AUDIO) },
        )
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, recorder) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) recorder.cancel()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); recorder.cancel() }
    }
    return recorder
}

private class AndroidVoiceRecorder(
    val hasPermission: () -> Boolean,
    val requestPermission: () -> Unit,
) : VoiceRecorder {
    private var record: AudioRecord? = null
    private var worker: Deferred<ByteArray>? = null
    private var recordingFlag: java.util.concurrent.atomic.AtomicBoolean? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Suppress("MissingPermission")
    override fun start(): Boolean {
        if (!hasPermission()) { requestPermission(); return false }
        cancel()
        val min = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        check(min > 0) { "设备不支持语音录音。" }
        val audio = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(min * 2, 6400))
        check(audio.state == AudioRecord.STATE_INITIALIZED) { audio.release(); "无法初始化麦克风。" }
        record = audio
        audio.startRecording()
        check(audio.recordingState == AudioRecord.RECORDSTATE_RECORDING) { cancel(); "麦克风被其他应用占用。" }
        val active = java.util.concurrent.atomic.AtomicBoolean(true)
        recordingFlag = active
        worker = scope.async {
            val output = ByteArrayOutputStream()
            val chunk = ByteArray(3200)
            // Hard cap in addition to the gesture timeout, so interruption cannot leave an unbounded recording.
            while (active.get() && output.size() < 16000 * 2 * 60) {
                val read = audio.read(chunk, 0, chunk.size)
                if (read < 0) error("麦克风录音中断，请重试。")
                if (read > 0) output.write(chunk, 0, read)
            }
            output.toByteArray()
        }
        return true
    }

    override suspend fun stop(): ByteArray {
        recordingFlag?.set(false)
        recordingFlag = null
        val audio = record
        val task = worker
        record = null; worker = null
        // A read returns within 100ms; let it finish before releasing its AudioRecord.
        return try {
            val pcm = task?.await() ?: error("录音已中断，请重试。")
            check(pcm.size >= 6400) { "录音太短，请按住后说话。" }
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
                .put("RIFF".toByteArray()).putInt(36 + pcm.size).put("WAVEfmt ".toByteArray())
                .putInt(16).putShort(1).putShort(1).putInt(16000).putInt(32000)
                .putShort(2).putShort(16).put("data".toByteArray()).putInt(pcm.size).array()
            header + pcm
        } finally {
            runCatching { audio?.stop() }; audio?.release()
        }
    }

    override fun cancel() {
        recordingFlag?.set(false)
        recordingFlag = null
        worker?.cancel(); worker = null
        val audio = record; record = null
        runCatching { audio?.stop() }; runCatching { audio?.release() }
    }
}
