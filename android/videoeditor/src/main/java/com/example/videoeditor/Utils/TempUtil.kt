package com.example.videoeditor.Utils

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOCase
import org.apache.commons.io.IOUtils
import org.apache.commons.io.filefilter.FileFilterUtils
import org.apache.commons.io.filefilter.IOFileFilter


object TempUtil {
    private val CLEANUP_CUTOFF = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3)
    private const val TAG = "TempUtil"
    fun cleanupStaleFiles(context: Context) {
        val filter: IOFileFilter = FileFilterUtils.and(
            FileFilterUtils.fileFileFilter(),
            FileFilterUtils.ageFileFilter(CLEANUP_CUTOFF),
            FileFilterUtils.or(
                FileFilterUtils.suffixFileFilter(".gif", IOCase.INSENSITIVE),
                FileFilterUtils.suffixFileFilter(".png", IOCase.INSENSITIVE),
                FileFilterUtils.suffixFileFilter(".mp4", IOCase.INSENSITIVE)
            )
        )
        val stale: Collection<File> = FileUtils.listFiles(context.cacheDir, filter, null)
        if (!stale.isEmpty()) {
            for (file in stale) {
                FileUtils.deleteQuietly(file)
            }
        }
    }

    fun createCopy(context: Context, uri: Uri, suffix: String?): File {
        val temp = createNewFile(context, suffix)
        try {
            context.contentResolver.openInputStream(uri).use { `is` ->
                FileOutputStream(temp).use { os ->
                    IOUtils.copy(
                        `is`,
                        os
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not copy $uri")
        }
        return temp
    }

    fun createNewFile(context: Context, suffix: String?): File {
        return createNewFile(context.cacheDir, suffix)
    }

    fun createNewFile(directory: File?, suffix: String?): File {
        return File(directory, UUID.randomUUID().toString() + suffix)
    }

    fun createNewFileWithName(context: Context, fileName: String?): File {
        return File(context.cacheDir, fileName)
    }
}
