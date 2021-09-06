package com.example.videoeditor.worker

import android.content.Context
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.videoeditor.Utils.VideoUtil
import com.google.common.util.concurrent.ListenableFuture
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import com.otaliastudios.transcoder.TranscoderOptions
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import com.otaliastudios.transcoder.strategy.size.PassThroughResizer
import java.io.File


class MergeVideosWorker(context: Context, params: WorkerParameters) :
    ListenableWorker(context, params) {
    override fun startWork(): ListenableFuture<Result> {
        val paths = inputData.getStringArray(KEY_INPUTS)
        val output = File(inputData.getString(KEY_OUTPUT))
        val files = arrayOfNulls<File>(paths!!.size)
        for (i in paths.indices) {
            files[i] = File(paths[i])
        }
        return CallbackToFutureAdapter.getFuture { completer ->
            doActualWork(files, output, completer)
            null
        }
    }

    private fun doActualWork(
        inputs: Array<File?>,
        output: File,
        completer: CallbackToFutureAdapter.Completer<Result>
    ) {
        val transcoder: TranscoderOptions.Builder = Transcoder.into(output.absolutePath)
        for (input in inputs) {
            VideoUtil.createDataSource(applicationContext, input!!.absolutePath)?.let {
                transcoder.addDataSource(
                    it
                )
            }
        }
        transcoder.setListener(object : TranscoderListener {
            override fun onTranscodeProgress(progress: Double) {}
            override fun onTranscodeCompleted(code: Int) {
                Log.d(TAG, "Merging video files has finished.")
                completer.set(Result.success())
                for (input in inputs) {
                    if (!input!!.delete()) {
                        Log.w(
                            TAG,
                            "Could not delete input file: $input"
                        )
                    }
                }
            }

            override fun onTranscodeCanceled() {
                Log.d(TAG, "Merging video files was cancelled.")
                completer.setCancelled()
                if (!output.delete()) {
                    Log.w(
                        TAG,
                        "Could not delete failed output file: $output"
                    )
                }
            }

            override fun onTranscodeFailed(e: Throwable) {
                Log.d(TAG, "Merging video files failed with error.", e)
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
            DefaultVideoStrategy.Builder(PassThroughResizer()).build()
        )
        transcoder.transcode()
    }

    companion object {
        const val KEY_INPUTS = "inputs"
        const val KEY_OUTPUT = "output"
        private const val TAG = "MergeVideosWorker"
    }
}
