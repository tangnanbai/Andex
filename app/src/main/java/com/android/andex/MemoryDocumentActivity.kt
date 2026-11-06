package com.android.andex

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** A short-lived SAF host; no shared-storage permission or fixed export path is required. */
class MemoryDocumentActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val request = requests[intent.getStringExtra("request")] ?: return finish()
        if (savedInstanceState == null) {
            val picker = if (request.export) Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = "text/markdown"; putExtra(Intent.EXTRA_TITLE, "Andex-memory.md")
            } else Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/markdown", "text/plain", "text/x-markdown", "application/octet-stream"))
            }
            picker.addCategory(Intent.CATEGORY_OPENABLE)
            @Suppress("DEPRECATION")
            startActivityForResult(picker, 1)
        }
    }

    @Deprecated("SAF compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val request = requests[intent.getStringExtra("request")] ?: return finish()
        val uri = data?.data
        CoroutineScope(Dispatchers.Main).launch {
        val result = withContext(Dispatchers.IO) { if (resultCode != RESULT_OK || uri == null) JSONObject().put("cancelled", true)
        else runCatching {
            if (request.export) {
                contentResolver.openOutputStream(uri, "wt")?.use { it.write(request.text.toByteArray(Charsets.UTF_8)) }
                    ?: error("无法写入文件")
                JSONObject().put("exported", true)
            } else {
                val bytes = contentResolver.openInputStream(uri)?.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                    ?: error("无法读取文件")
                JSONObject().put("text", com.android.andex.data.decodeMemoryMarkdown(bytes))
            }
        }.getOrElse { JSONObject().put("error", it.message ?: "文件操作失败") }
        }
        request.result.complete(result)
        finish()
        }
    }

    companion object {
        private data class Request(val export: Boolean, val text: String, val result: CompletableDeferred<JSONObject>)
        private val requests = ConcurrentHashMap<String, Request>()
        suspend fun open(context: Context, export: Boolean, text: String = ""): JSONObject {
            val id = UUID.randomUUID().toString()
            val pending = Request(export, text, CompletableDeferred())
            requests[id] = pending
            try {
                withContext(Dispatchers.Main) {
                    context.startActivity(Intent(context, MemoryDocumentActivity::class.java)
                        .putExtra("request", id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                val result = withTimeoutOrNull(110_000) { pending.result.await() }
                    ?: error("文件选择超时，请重新导入。")
                check(!result.has("error")) { result.optString("error") }
                return result
            } finally { requests.remove(id) }
        }
    }
}
