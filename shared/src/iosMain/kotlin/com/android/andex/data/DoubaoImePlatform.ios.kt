package com.android.andex.data

import androidx.compose.runtime.*
import kotlinx.serialization.json.Json

/** Configured by the native host using Keychain and AVAudioConverter. */
object IosOnlineVoiceBindings {
    var credentialStoreError: String? = null
    var readCredentials: () -> String? = { null }
    var writeCredentials: (String) -> Unit = { error("语音存储尚未就绪。") }
    var encoderFactory: () -> OpusPacketEncoder = { error("音频编码尚未就绪。") }
    var beginRecording: (((ByteArray) -> Unit)) -> Boolean = { false }
    var finishRecording: () -> Unit = {}
    var cancelRecording: () -> Unit = {}
    fun newClient(): DoubaoImeAsr = DoubaoImeAsr(object : DoubaoCredentialStore {
        override suspend fun read(): DoubaoCredentials? {
            val value = readCredentials()
            check(credentialStoreError == null) { credentialStoreError.orEmpty() }
            return value?.let {
                try { Json.decodeFromString<DoubaoCredentials>(it) }
                catch (_: Exception) { error("语音凭据格式错误，未重新注册设备。") }
            }
        }
        override suspend fun write(value: DoubaoCredentials) {
            writeCredentials(Json.encodeToString(value))
            check(credentialStoreError == null) { credentialStoreError.orEmpty() }
        }
    })
}

actual fun createOpusPacketEncoder(): OpusPacketEncoder = IosOnlineVoiceBindings.encoderFactory()

@Composable actual fun rememberDoubaoImeAsr(): DoubaoImeAsr = remember {
    IosOnlineVoiceBindings.newClient()
}
