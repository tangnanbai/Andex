package com.android.andex.data.pi

/** A missing node binary can be installed; a tracer denied by Android cannot. */
internal fun piRuntimeFailure(stderr: String): String? = when {
    stderr.contains("ptrace(TRACEME): Permission denied", ignoreCase = true) ->
        "Android 拒绝启动本地运行环境（PRoot ptrace 权限被拒绝），对话尚未发送到模型。" +
            "当前设备或模拟器不支持此运行方式；重新填写 API Key 或安装 Node 无法解决。"
    else -> null
}
