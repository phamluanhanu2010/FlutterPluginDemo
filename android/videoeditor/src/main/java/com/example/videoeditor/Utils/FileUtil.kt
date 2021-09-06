package com.example.videoeditor.Utils

import android.util.Log
import java.io.*
import java.lang.Exception


object FileUtil {
    private const val TAG = "FileUtil"
    fun copyFile(from: File, to: File): Boolean {
        try {
            FileInputStream(from).use { `is` ->
                FileOutputStream(to).use { os ->
                    val buffer = ByteArray(1024)
                    var length: Int
                    while (`is`.read(buffer).also { length = it } > 0) {
                        os.write(buffer, 0, length)
                    }
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy $from to $to", e)
        }
        return false
    }

    @Throws(IOException::class)
    fun copyFile(`in`: InputStream, out: OutputStream) {
        val buffer = ByteArray(1024)
        var read: Int
        while (`in`.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
        }
    }
}
