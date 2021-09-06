package com.example.videoeditor.worker


import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.videoeditor.Utils.VideoUtil
import com.google.common.util.concurrent.ListenableFuture
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import com.otaliastudios.transcoder.engine.TrackType
import com.otaliastudios.transcoder.source.BlankAudioDataSource
import com.otaliastudios.transcoder.source.ClipDataSource
import com.otaliastudios.transcoder.source.DataSource
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import com.otaliastudios.transcoder.strategy.size.PassThroughResizer
import java.io.File
import java.util.concurrent.TimeUnit


class MergeAudioVideoWorker(context: Context, params: WorkerParameters) :
    ListenableWorker(context, params) {
    override fun startWork(): ListenableFuture<Result> {
        val audio = inputData.getString(KEY_AUDIO)
        val video = File(inputData.getString(KEY_VIDEO))
        val output = File(inputData.getString(KEY_OUTPUT))
        return CallbackToFutureAdapter.getFuture { completer: CallbackToFutureAdapter.Completer<Result> ->
            doActualWork(video, audio, output, completer)
            null
        }
    }

    private fun doActualWork(
        video: File,
        audio: String?,
        output: File,
        completer: CallbackToFutureAdapter.Completer<Result>
    ) {
        var audio2: DataSource? = VideoUtil.createDataSource(
            applicationContext, audio
        )
        val transcoder = Transcoder.into(output.absolutePath)
        val duration1 = TimeUnit.MICROSECONDS.toMillis(audio2?.durationUs!!)
        val duration2: Long = VideoUtil.getDuration(applicationContext, Uri.fromFile(video))
        if (duration1 > duration2) {
            audio2 = ClipDataSource(
                audio2, 0, TimeUnit.MILLISECONDS.toMicros(duration2)
            )
            transcoder.addDataSource(TrackType.AUDIO, audio2)
        } else {
            transcoder.addDataSource(TrackType.AUDIO, audio2)
            transcoder.addDataSource(
                TrackType.AUDIO,
                BlankAudioDataSource(
                    TimeUnit.MILLISECONDS.toMicros(duration2 - duration1)
                )
            )
        }
        transcoder.addDataSource(TrackType.VIDEO, video.absolutePath)
        transcoder.setListener(object : TranscoderListener {
            override fun onTranscodeProgress(progress: Double) {}
            override fun onTranscodeCompleted(code: Int) {
                Log.d(TAG, "Merging audio/video has finished.")
                completer.set(Result.success())
                if (!video.delete()) {
                    Log.w(
                        TAG,
                        "Could not delete video file: $video"
                    )
                }
            }

            override fun onTranscodeCanceled() {
                Log.d(TAG, "Merging audio/video was cancelled.")
                completer.setCancelled()
                if (!output.delete()) {
                    Log.w(
                        TAG,
                        "Could not delete failed output file: $output"
                    )
                }
            }

            override fun onTranscodeFailed(e: Throwable) {
                Log.d(TAG, "Merging audio/video failed with error.", e)
                completer.setException(e)
                if (!output.delete()) {
                    Log.w(
                        TAG,
                        "Could not delete failed output file: $output"
                    )
                }
            }
        })
        transcoder.setVideoTrackStrategy(
            DefaultVideoStrategy.Builder(PassThroughResizer())
                .build()
        )
        transcoder.transcode()
    }

    companion object {
        const val KEY_AUDIO = "audio"
        const val KEY_OUTPUT = "output"
        const val KEY_VIDEO = "video"
        private const val TAG = "MergeAudioVideoWorker"
    }
}