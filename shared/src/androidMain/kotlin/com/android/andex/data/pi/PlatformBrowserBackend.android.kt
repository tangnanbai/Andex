package com.android.andex.data.pi

import com.android.andex.runtime.MultiplatformLocalRuntime

internal actual fun createPlatformBrowserBackend(
    runtime: MultiplatformLocalRuntime,
): SharedBrowserBackend? = null
