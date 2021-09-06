package com.example.videoeditor.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.videoeditor.R
import org.apache.commons.io.IOUtils
import java.io.*


class SaveToGalleryWorker(context: Context, params: WorkerParameters) :
    Worker(context, params) {
    override fun doWork(): Result {
        val file = File(inputData.getString(KEY_FILE))
        val name = inputData.getString(KEY_NAME)
        val success = doActualWork(file, name)
        if (success && !file.delete()) {
            Log.w(
                TAG,
                "Could not delete downloaded file: $file"
            )
        }
        return if (success) Result.success() else Result.failure()
    }

    fun doActualWork(file: File?, name: String?): Boolean {
        val movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val context = applicationContext
        val app = File(movies, "Video2")
        if (!app.isDirectory && !app.mkdirs()) {
            Log.v(
                TAG,
                "Could not create app directory at $app"
            )
        }
        val mp4 = File(app, name)
        if (mp4.parentFile?.exists() == false) mp4.parentFile?.mkdir()

        var `is`: InputStream? = null
        var os: OutputStream? = null
        try {
            `is` = FileInputStream(file)
            os = FileOutputStream(mp4)
            IOUtils.copy(`is`, os)
        } catch (e: Exception) {
            Log.e(TAG, "Could not save video to $mp4", e)
        } finally {
            try {
                `is`?.close()
            } catch (ignore: Exception) {
            }
            try {
                os?.close()
            } catch (ignore: Exception) {
            }
        }
        val values = ContentValues(2)
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        values.put(MediaStore.Video.Media.DATA, mp4.absolutePath)
        val uri = context.contentResolver
            .insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
        if (inputData.getBoolean(KEY_NOTIFICATION, false)) {
            if (Build.VERSION.SDK_INT >= 26) {
                val channel = NotificationChannel(
                    NOTI_CHANNEL,
                    "Your channel name",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                NotificationManagerCompat.from(context).createNotificationChannel(channel)
            }

            var builder = NotificationCompat.Builder(context, NOTI_CHANNEL)
                .setSmallIcon(R.drawable.ic_effects) // dummy icon 
                .setContentText(context.getString(R.string.notification_saved_description))
                .setContentTitle(context.getString(R.string.notification_saved_title))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            NotificationManagerCompat.from(context).notify(1001, builder.build())
        }
        return true
    }

    companion object {
        const val KEY_FILE = "file"
        const val KEY_NAME = "name"
        const val KEY_NOTIFICATION = "notification"
        private const val TAG = "SaveToGalleryWorker"
        private const val NOTI_CHANNEL = "notification_channel_id"
    }
}
