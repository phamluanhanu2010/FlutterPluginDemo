package com.example.videoeditor.Utils

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import com.otaliastudios.transcoder.source.DataSource
import com.otaliastudios.transcoder.source.FilePathDataSource
import com.otaliastudios.transcoder.source.UriDataSource

object VideoUtil {
    private const val TAG = "VideoUtil"
    fun getDuration(context: Context?, uri: Uri): Long {
        var mmr: MediaMetadataRetriever? = null
        try {
            mmr = MediaMetadataRetriever()
            if (TextUtils.equals(uri.scheme, ContentResolver.SCHEME_FILE)) {
                mmr.setDataSource(uri.path)
            } else {
                mmr.setDataSource(context, uri)
            }
            val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (duration != null) {
                return duration.toLong()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to extract duration from $uri", e)
        } finally {
            mmr?.release()
        }
        return 0
    }

    fun createDataSource(context: Context?, file: String?): DataSource? {
        var file = file
        if (file!!.startsWith(ContentResolver.SCHEME_CONTENT)) {
            return UriDataSource(context!!, Uri.parse(file))
        }
        if (file.startsWith(ContentResolver.SCHEME_FILE)) {
            file = Uri.parse(file).path
        }
        return FilePathDataSource(file!!)
    }
}