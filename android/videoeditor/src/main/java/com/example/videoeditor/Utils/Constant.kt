package com.example.videoeditor.Utils

import java.util.concurrent.TimeUnit

interface Constant {
    companion object {
        val MAX_DURATION = TimeUnit.SECONDS.toMillis(60)
        val MIN_DURATION = TimeUnit.SECONDS.toMillis(3)
        val READ_EXTERNAL_STORAGE_REQUEST_CODE = 123

    }
}