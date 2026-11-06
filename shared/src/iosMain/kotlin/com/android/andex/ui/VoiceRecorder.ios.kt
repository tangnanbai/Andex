@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.android.andex.ui

import androidx.compose.runtime.*
import kotlinx.cinterop.*
import platform.AVFAudio.*
import platform.AudioToolbox.kAudioFormatLinearPCM
import platform.Foundation.*
import platform.posix.memcpy
import com.android.andex.platform.SharedApplicationLifecycle

@Composable
actual fun ObserveVoiceInputInterruptions(onInterrupted: () -> Unit) {
    val current by rememberUpdatedState(onInterrupted)
    LaunchedEffect(Unit) {
        SharedApplicationLifecycle.backgrounded.collect { background -> if (background) current() }
    }
}

@Composable
actual fun rememberVoiceRecorder(): VoiceRecorder {
    val recorder = remember { IosVoiceRecorder() }
    DisposableEffect(recorder) { onDispose { recorder.cancel() } }
    return recorder
}

private class IosVoiceRecorder : VoiceRecorder {
    private var streaming = false
    private var audio: AVAudioRecorder? = null
    private var url: NSURL? = null
    override fun start(onPcm: ((ByteArray) -> Unit)?): Boolean {
        val session = AVAudioSession.sharedInstance()
        if (session.recordPermission != AVAudioSessionRecordPermissionGranted) {
            session.requestRecordPermission { }
            return false
        }
        cancel()
        if (onPcm != null) {
            streaming = com.android.andex.data.IosOnlineVoiceBindings.beginRecording(onPcm)
            return streaming
        }
        session.setCategory(AVAudioSessionCategoryRecord, error = null)
        session.setActive(true, error = null)
        val file = NSURL.fileURLWithPath(NSTemporaryDirectory() + "andex-voice-${NSUUID().UUIDString}.wav")
        url = file
        val recorder = AVAudioRecorder(file, mapOf<Any?, Any?>(
            AVFormatIDKey to kAudioFormatLinearPCM,
            AVSampleRateKey to 16000.0,
            AVNumberOfChannelsKey to 1,
            AVLinearPCMBitDepthKey to 16,
            AVLinearPCMIsFloatKey to false,
            AVLinearPCMIsBigEndianKey to false,
        ), error = null)
        audio = recorder
        check(recorder.recordForDuration(60.0)) { cancel(); "无法开始录音，请检查麦克风权限。" }
        return true
    }
    override suspend fun stop(): ByteArray {
        if (streaming) {
            com.android.andex.data.IosOnlineVoiceBindings.finishRecording()
            streaming = false
            return ByteArray(0)
        }
        val recorder = audio ?: error("录音已中断，请重试。")
        recorder.stop()
        val data = url?.let { NSData.dataWithContentsOfURL(it) } ?: error("无法读取录音。")
        check(data.length >= 6444uL) { "录音太短，请按住后说话。" }
        return ByteArray(data.length.toInt()).also { result ->
            result.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
            cancel()
        }
    }
    override fun cancel() {
        if (streaming) com.android.andex.data.IosOnlineVoiceBindings.cancelRecording()
        streaming = false
        audio?.stop(); audio = null
        url?.let { NSFileManager.defaultManager.removeItemAtURL(it, error = null) }; url = null
        AVAudioSession.sharedInstance().setActive(false, error = null)
    }
}
