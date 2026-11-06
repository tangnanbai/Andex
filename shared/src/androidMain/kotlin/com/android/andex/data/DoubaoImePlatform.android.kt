package com.android.andex.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import io.github.jaredmdobson.concentus.OpusApplication
import io.github.jaredmdobson.concentus.OpusEncoder
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

actual fun createOpusPacketEncoder(): OpusPacketEncoder = object : OpusPacketEncoder {
    private val encoder = OpusEncoder(16000, 1, OpusApplication.OPUS_APPLICATION_AUDIO)
    override fun encode(pcm: ByteArray): ByteArray {
        require(pcm.size == 640)
        val samples = ShortArray(320) { i -> ((pcm[i * 2].toInt() and 255) or (pcm[i * 2 + 1].toInt() shl 8)).toShort() }
        val output = ByteArray(4000)
        val count = encoder.encode(samples, 0, 320, output, 0, output.size)
        check(count > 0) { "Opus 音频编码失败。" }
        return output.copyOf(count)
    }
    override fun close() = Unit
}

@Composable actual fun rememberDoubaoImeAsr(): DoubaoImeAsr {
    val context = LocalContext.current.applicationContext
    return remember(context) { DoubaoImeAsr(AndroidDoubaoCredentials(context)) }
}

/** AES/GCM key stays in Android Keystore; ciphertext lives only in private app storage. */
private class AndroidDoubaoCredentials(context: Context) : DoubaoCredentialStore {
    private val preferences = context.getSharedPreferences("doubao_ime_private", Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val keystore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keystore.getKey(Alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(Alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generateKey()
        }
    }
    override suspend fun read(): DoubaoCredentials? = withContext(Dispatchers.IO) {
        val encoded = preferences.getString("credentials", null) ?: return@withContext null
        try {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            }
            Json.decodeFromString<DoubaoCredentials>(cipher.doFinal(bytes.copyOfRange(12, bytes.size)).decodeToString())
        } catch (_: Exception) { error("语音凭据无法解密，未重新注册设备。请检查应用数据恢复状态。") }
    }
    override suspend fun write(value: DoubaoCredentials) = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val bytes = cipher.iv + cipher.doFinal(Json.encodeToString(value).encodeToByteArray())
        check(preferences.edit().putString("credentials", Base64.encodeToString(bytes, Base64.NO_WRAP)).commit()) {
            "无法保存语音凭据。"
        }
    }
    companion object { private const val Alias = "andex.doubao-ime.credentials.v1" }
}
