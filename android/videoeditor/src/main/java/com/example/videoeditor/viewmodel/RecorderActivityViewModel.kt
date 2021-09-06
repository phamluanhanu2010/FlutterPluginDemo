package com.example.videoeditor.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.videoeditor.models.RecordSegment
import java.io.File
import java.util.*

class RecorderActivityViewModel : ViewModel() {
    var audio: Uri? = null
    internal var segments: MutableList<RecordSegment> =
        ArrayList<RecordSegment>()
    var songId = 0
    var video: File? = null
    fun recorded(): Long {
        var recorded: Long = 0
        for (segment in segments) {
            recorded += segment.duration
        }
        return recorded
    }
}