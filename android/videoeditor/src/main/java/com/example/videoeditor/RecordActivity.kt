package com.example.videoeditor

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.segmentedprogressbar.ProgressBarListener
import com.example.videoeditor.Utils.Constant
import com.example.videoeditor.Utils.FileUtil
import com.example.videoeditor.Utils.TempUtil
import com.example.videoeditor.Utils.VideoUtil
import com.example.videoeditor.adapter.VideoOptionsAdapter
import com.example.videoeditor.databinding.ActivityRecordBinding
import com.example.videoeditor.models.RecordSegment
import com.example.videoeditor.models.Song
import com.example.videoeditor.models.VideoOptionModel
import com.example.videoeditor.viewmodel.RecorderActivityViewModel
import com.example.videoeditor.worker.MergeAudioVideoWorker
import com.example.videoeditor.worker.MergeVideosWorker
import com.example.videoeditor.worker.SaveToGalleryWorker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kaopiz.kprogresshud.KProgressHUD
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Flash
import com.otaliastudios.cameraview.controls.Mode
import com.otaliastudios.cameraview.filter.Filters
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit


class RecordActivity : AppCompatActivity() {
    private val TAG = "RecorderActivity"

    private lateinit var binding: ActivityRecordBinding
    private lateinit var mModel: RecorderActivityViewModel
    private val mHandler = Handler(Looper.getMainLooper())
    private var isFilter: Boolean = false
    private var isMusic: Boolean = false
    private var mMediaPlayer: MediaPlayer? = null
    private val mStopper = Runnable { stopRecording() }
    private var mProgress: KProgressHUD? = null
    private var audioTemp: File? = null
    val permissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mModel = ViewModelProvider(this)[RecorderActivityViewModel::class.java]

        if (!hasPermissions(*permissions)) {
            ActivityCompat.requestPermissions(
                this,
                permissions,
                Constant.READ_EXTERNAL_STORAGE_REQUEST_CODE
            )
        }

        initView()
        initAction()
    }


    fun hasPermissions(vararg permissions: String): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constant.READ_EXTERNAL_STORAGE_REQUEST_CODE && grantResults.size > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

        } else {
            ActivityCompat.requestPermissions(
                this,
                permissions,
                Constant.READ_EXTERNAL_STORAGE_REQUEST_CODE
            )
        }

    }

    private fun initView() {
        binding.cameraView.setLifecycleOwner(this)
        binding.cameraView.mode = Mode.VIDEO

        var newPostOptions = listOf(
            VideoOptionModel(getString(R.string.record_option_effects), R.drawable.ic_effects),
            VideoOptionModel(getString(R.string.record_option_beauty), R.drawable.ic_beauty),
            VideoOptionModel(getString(R.string.record_option_filter), R.drawable.ic_filter),
            VideoOptionModel(getString(R.string.record_option_music), R.drawable.ic_music),
            VideoOptionModel(getString(R.string.record_option_timer), R.drawable.ic_timer)
        )
        var videoOptionsAdapter: VideoOptionsAdapter =
            VideoOptionsAdapter(newPostOptions)
        videoOptionsAdapter.onItemClicked = this::videoOptions
        val llm = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        binding.rcOptions.setLayoutManager(llm)
        binding.rcOptions.adapter = videoOptionsAdapter

    }

    private fun initAction() {
        binding.imgClose.setOnClickListener {
            if (mModel.segments.isEmpty()) {
                finish()
            } else {
                confirmClose()
            }

        }

        binding.segments.enableAutoProgressView(Constant.MAX_DURATION)
        binding.segments.setDividerColor(Color.WHITE)
        binding.segments.setDividerEnabled(true)
        binding.segments.setDividerWidth(2f)
        binding.segments.listener = object : ProgressBarListener {
            override fun TimeinMill(mills: Long) {

            }
        }
        binding.segments.setShader(
            intArrayOf(
                ContextCompat.getColor(this, R.color.pink),
                ContextCompat.getColor(this, R.color.pink)
            )
        )

        binding.cameraView.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(result: PictureResult) {
                super.onPictureTaken(result)

            }

            override fun onVideoRecordingEnd() {
                Log.v(TAG, "Video recording has ended.")
                binding.segments.pause()
                binding.segments.addDivider()
                mHandler.removeCallbacks(mStopper)
                mHandler.postDelayed({
                    saveCurrentRecording()
                    recordUI(false)
                }, 500)
                if (mMediaPlayer != null) {
                    mMediaPlayer!!.pause()
                }
            }

            override fun onVideoRecordingStart() {
                Log.v(TAG, "Video recording has started.")
                binding.segments.resume()
                if (mMediaPlayer != null) {
                    mMediaPlayer?.start()
                }
            }
        })

        binding.imgRecord.setOnClickListener {
            if (binding.cameraView.isTakingVideo) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        binding.imgStopRecord.setOnClickListener {
            stopRecording()
        }

        binding.imgFlash.setOnClickListener {
            if (binding.cameraView.isTakingVideo()) {
                Toast.makeText(this, R.string.recorder_error_in_progress, Toast.LENGTH_SHORT)
                    .show()
            } else {
                binding.cameraView.setFlash(if (binding.cameraView.getFlash() == Flash.OFF) Flash.TORCH else Flash.OFF)
                if (binding.cameraView.facing == Facing.BACK) {
                    binding.imgFlash.setImageResource(if (binding.cameraView.getFlash() == Flash.OFF) R.drawable.ic_flash_off else R.drawable.ic_music) // Design has not provided flash on icon so just dummy icon now
                }
            }
        }

        binding.imgCameraSwitch.setOnClickListener {
            if (binding.cameraView.isTakingVideo()) {
                Toast.makeText(this, R.string.recorder_error_in_progress, Toast.LENGTH_SHORT)
                    .show()
            } else {
                binding.cameraView.toggleFacing()
                if (binding.cameraView.facing == Facing.FRONT) {
                    binding.imgFlash.setImageResource(R.drawable.ic_flash_off)
                }
            }
        }

        binding.imgFinishRecordFirst.setOnClickListener {
            if (mModel.segments.isEmpty()) {
                Toast.makeText(this, R.string.recorder_error_no_clips, Toast.LENGTH_SHORT).show()
            } else if (mModel.recorded() < Constant.MIN_DURATION) {
                Toast.makeText(this, R.string.recorder_error_too_short, Toast.LENGTH_SHORT).show()
            } else {
                submitForConcat(mModel.segments, mModel.audio)
            }
        }

        binding.imgFinishRecordSecond.setOnClickListener {
            Toast.makeText(this, R.string.recorder_error_in_progress, Toast.LENGTH_SHORT).show()
        }

        dummyAudioData()
    }

    private fun startRecording() {
        val recorded: Long = mModel?.recorded()!!
        if (recorded >= Constant.MAX_DURATION) {
            Toast.makeText(
                this,
                R.string.recorder_error_maxed_out,
                Toast.LENGTH_SHORT
            ).show()
        } else {
            mModel.video = TempUtil.createNewFile(this, ".mp4")
            binding.cameraView.takeVideoSnapshot(
                mModel.video!!, (Constant.MAX_DURATION - recorded).toInt()
            )

            recordUI(true)
        }
    }

    private fun stopRecording() {
        binding.cameraView.stopVideo()
    }

    private fun recordUI(record: Boolean) {
        if (record) {
            binding.loutBottom.visibility = View.GONE
            binding.loutStopRecord.visibility = View.VISIBLE
            binding.imgStopRecord.startRippleAnimation()
            binding.imgFinishRecordSecond.visibility =
                if (mModel.recorded() > Constant.MIN_DURATION) View.VISIBLE else View.GONE
        } else {
            binding.loutBottom.visibility = View.VISIBLE
            binding.loutStopRecord.visibility = View.GONE
            binding.imgFinishRecordFirst.visibility =
                if (mModel.recorded() > Constant.MIN_DURATION) View.VISIBLE else View.GONE
            if (binding.imgStopRecord.isRippleAnimationRunning)
                binding.imgStopRecord.startRippleAnimation()
        }
    }

    private fun saveCurrentRecording() {
        val duration: Long = VideoUtil.getDuration(this, Uri.fromFile(mModel.video))
        val segment = RecordSegment()
        segment.file = mModel.video!!.absolutePath
        segment.duration = duration
        mModel.segments.add(segment)
        mModel.video = null
    }

    private fun videoOptions(option: VideoOptionModel, position: Int) {
        when (position) {
            0 -> Toast.makeText(this, "x == ${position}", Toast.LENGTH_SHORT).show()
            1 -> Toast.makeText(this, "x == ${position}", Toast.LENGTH_SHORT).show()
            2 -> {
                if (isFilter) {
                    binding.cameraView.filter = Filters.NONE.newInstance()
                } else
                    binding.cameraView.filter = Filters.SEPIA.newInstance()

                isFilter = !isFilter
            }
            3 -> {
                if (mModel.segments.isEmpty()) {
                    if (isMusic) {
                        setupSong(null, null)
                    } else
                        setupSong(
                            Song(
                                "This is the name of the song This is the name of the song This is the name of the song",
                                0,
                                "artist"
                            ), Uri.parse(audioTemp?.absolutePath)
                        )

                    isMusic = !isMusic
                } else if (mModel.audio == null) {
                    Toast.makeText(this, R.string.message_song_select, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.message_song_change, Toast.LENGTH_SHORT).show()
                }
            }
            4 -> {
                if ((Constant.MAX_DURATION - mModel.recorded()) < 10000) {
                    Toast.makeText(this, R.string.message_timer_is_max, Toast.LENGTH_SHORT).show()
                } else {
                    startTimer()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mMediaPlayer != null) {
            if (mMediaPlayer!!.isPlaying) {
                mMediaPlayer!!.stop()
            }
            mMediaPlayer!!.release()
            mMediaPlayer = null
        }

        for (segment in mModel.segments) {
            val file = File(segment.file)
            if (!file.delete()) {
                Log.v(TAG, "Could not delete record segment file: $file")
            }
        }
    }

    private fun setupSong(song: Song?, file: Uri?) {
        if (song == null) {
            binding.tvMusicTitle.visibility = View.GONE
            binding.tvMusicTitle.isSelected = false
            mMediaPlayer = null
            mModel.audio = null
        } else {
            mMediaPlayer = MediaPlayer.create(this, file)
            mMediaPlayer?.setOnCompletionListener(OnCompletionListener { mp: MediaPlayer? ->
                mMediaPlayer = null

            })
            binding.tvMusicTitle.visibility = View.VISIBLE
            binding.tvMusicTitle.text = song?.title
            binding.tvMusicTitle.isSelected = true
            mModel.audio = file
        }
    }

    @SuppressLint("SetTextI18n")
    private fun startTimer() {
        binding.tvCount.text = ""
        val duration: Long = 10000L
        val timer: CountDownTimer = object : CountDownTimer(3000, 1000) {
            override fun onTick(remaining: Long) {
                mHandler.post {
                    binding.tvCount.text = "${TimeUnit.MILLISECONDS.toSeconds(remaining) + 1}"
                }
            }

            override fun onFinish() {
                mHandler.post { binding.loutCountDown.visibility = View.GONE }
                startRecording()
                mHandler.postDelayed(mStopper, duration)
            }
        }
        binding.loutCountDown.visibility = View.VISIBLE
        timer.start()
    }

    private fun confirmClose() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.confirmation_close_recording)
            .setNegativeButton(R.string.cancel_button) { dialog, i -> dialog.cancel() }
            .setPositiveButton(R.string.close_button) { dialog, i ->
                dialog.dismiss()
                finish()
            }
            .show()
    }

    private fun submitForConcat(segments: List<RecordSegment>, audio: Uri?) {
        mProgress = KProgressHUD.create(this)
            .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
            .setLabel(getString(R.string.progress_title))
            .setCancellable(false)
            .show()
        val videos: MutableList<String?> = ArrayList()
        for (segment in segments) {
            videos.add(segment.file)
        }

        val merged = TempUtil.createNewFile(this, ".mp4")
        val data: Data = Data.Builder()
            .putStringArray(MergeVideosWorker.KEY_INPUTS, videos.toTypedArray())
            .putString(MergeVideosWorker.KEY_OUTPUT, merged.absolutePath)
            .build()
        val request: OneTimeWorkRequest = OneTimeWorkRequest.Builder(MergeVideosWorker::class.java)
            .setInputData(data)
            .build()
        val wm: WorkManager = WorkManager.getInstance(this)
        wm.enqueue(request)
        wm.getWorkInfoByIdLiveData(request.getId())
            .observe(this) { info ->
                val ended =
                    info.getState() === WorkInfo.State.CANCELLED || info.getState() === WorkInfo.State.FAILED || info.getState() === WorkInfo.State.SUCCEEDED

                if (ended) {
                    mProgress?.dismiss()
                }
                if (info.getState() === WorkInfo.State.SUCCEEDED) {
                    audio?.let {
                        if (!it.path.isNullOrEmpty()) {
//                            submitForMerge(merged.absolutePath, audio?.path)
                            submitForMerge(
                                merged.absolutePath,
                                audio?.toString()
                            ) // Just dummy for now
                        }
                    } ?: run {
                        saveVideoToGallery(merged)
                    }
                }
            }
    }

    private fun submitForMerge(video: String, audio: String?) {
        mProgress = KProgressHUD.create(this)
            .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
            .setLabel(getString(R.string.progress_title))
            .setCancellable(false)
            .show()
        val merged = TempUtil.createNewFile(this, ".mp4")
        val data = Data.Builder()
            .putString(MergeAudioVideoWorker.KEY_AUDIO, audio)
            .putString(MergeAudioVideoWorker.KEY_VIDEO, video)
            .putString(MergeAudioVideoWorker.KEY_OUTPUT, merged.absolutePath)
            .build()
        val request = OneTimeWorkRequest.Builder(MergeAudioVideoWorker::class.java)
            .setInputData(data)
            .build()
        val wm = WorkManager.getInstance(this)
        wm.enqueue(request)
        wm.getWorkInfoByIdLiveData(request.id)
            .observe(this, { info: WorkInfo ->
                val ended =
                    info.getState() === WorkInfo.State.CANCELLED || info.getState() === WorkInfo.State.FAILED || info.getState() === WorkInfo.State.SUCCEEDED
                if (ended) {
                    mProgress?.dismiss()
                }
                if (info.state == WorkInfo.State.SUCCEEDED) {
                    Log.v(TAG, "submitForMerge: " + merged.absolutePath)
                    saveVideoToGallery(merged)
                }
            })
    }

    private fun saveVideoToGallery(video: File) {
        mProgress = KProgressHUD.create(this)
            .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
            .setLabel(getString(R.string.progress_title))
            .setCancellable(false)
            .show()
        val name = "VDO_" + System.currentTimeMillis().toString() + ".mp4"
        val data = Data.Builder()
            .putString(SaveToGalleryWorker.KEY_FILE, video.absolutePath)
            .putString(SaveToGalleryWorker.KEY_NAME, name)
            .putBoolean(SaveToGalleryWorker.KEY_NOTIFICATION, true)
            .build()

        val request = OneTimeWorkRequest.Builder(SaveToGalleryWorker::class.java)
            .setInputData(data)
            .build()

        val wm = WorkManager.getInstance(this)
        wm.enqueue(request)
        wm.getWorkInfoByIdLiveData(request.id)
            .observe(this, { info: WorkInfo ->
                val ended =
                    info.getState() === WorkInfo.State.CANCELLED || info.getState() === WorkInfo.State.FAILED || info.getState() === WorkInfo.State.SUCCEEDED
                if (ended) {
                    mProgress?.dismiss()
                }
                if (info.state == WorkInfo.State.SUCCEEDED) {
                    Log.v(TAG, "saveVideoToGallery: Done")
                    Toast.makeText(this, "Video has been saved to gallery", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }
            })
    }

    private fun dummyAudioData() {
        try {
            val inputStream = resources.openRawResource(R.raw.savagelovelaxedsirenbeat)
            audioTemp = TempUtil.createNewFileWithName(this, "savagelovelaxedsirenbeat.mp3")
            FileUtil.copyFile(inputStream, FileOutputStream(audioTemp))
        } catch (e: IOException) {
            throw RuntimeException("Can't create temp file ", e)
        }
    }
}