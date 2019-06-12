package com.wynk.download.issue

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.wynk.download.issue.sinks.OfflineDataSink
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


const val TAG = "prashant_player"

class MainActivity : AppCompatActivity(), Player.EventListener {

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
        Log.d(TAG, "onPlaybackParametersChanged")
    }

    override fun onSeekProcessed() {
        Log.d(TAG, "onSeekProcessed")
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
        Log.d(TAG, "onTracksChanged")
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        Log.d(TAG, "onPlayerError ${error.printStackTrace()}")
    }

    override fun onLoadingChanged(isLoading: Boolean) {
        Log.d(TAG, "onLoadingChanged")
    }

    override fun onPositionDiscontinuity(reason: Int) {
        Log.d(TAG, "onPositionDiscontinuity")
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        Log.d(TAG, "onRepeatModeChanged")
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        Log.d(TAG, "onShuffleModeEnabledChanged")
    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
        Log.d(TAG, "onTimelineChanged")
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        Log.d(TAG, "onPlayerStateChanged :: playbackState: $playbackState")
    }

    private lateinit var player: SimpleExoPlayer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //check for permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED
        ) {
            //ask for permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), 1
                )
            }
        }

        button.setOnClickListener {
//            playOfflineSongs()
            decryptOfflineSongs()
        }
    }

    private fun initPlayer() {
        val trackSelectionFactory = AdaptiveTrackSelection.Factory(DefaultBandwidthMeter())
        val trackSelector = DefaultTrackSelector(trackSelectionFactory)
        val loadControl = DefaultLoadControl.Builder().setBufferDurationsMs(
            60000,
            600000,
            DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
            DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
        ).setTargetBufferBytes(10 * 1024 * 1024).createDefaultLoadControl()

        val renderersFactory = DefaultRenderersFactory(this)
        player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector, loadControl)
        player.addListener(this)
    }


    private fun playMedia(source: MediaSource) {
        player.prepare(source)
        player.playWhenReady = true
    }

    private fun playOfflineSongs() {
        val path = Environment.getExternalStorageDirectory().toString() + "/wynk-rented"
        val outPath = Environment.getExternalStorageDirectory().toString() + "/wynk-rented-decrypted"
        val directory = File(path)
        val files = directory.listFiles()
        Log.d(TAG, "Size: " + files.size)
        for (i in files.indices) {
            val inFile = files[i]
            val filePath = inFile.path
            val songId = inFile.name
            Log.d(TAG, "FileName:$filePath   songId:$songId")
            val outFile = File(outPath, songId)
            val mCryptoHelper = CryptoHelper(songId)
            mCryptoHelper.decrypt(FileInputStream(inFile), FileOutputStream(outFile))
//            FileUtils.copy()
//            val source =
//                ExtractorMediaSource.Factory(MusicDataSourceFactory(songId, filePath))
//                    .setExtractorsFactory(DefaultExtractorsFactory()).setMinLoadableRetryCount(1)
//                    .createMediaSource(Uri.parse(filePath))
//            playMedia(source)
            break
        }
    }

    private fun decryptOfflineSongs() {
        val path = Environment.getExternalStorageDirectory().toString() + "/wynk-rented"
        val outPath = Environment.getExternalStorageDirectory().toString() + "/wynk-rented-decrypted"
        val directory = File(path)
        val files = directory.listFiles()
        Log.d(TAG, "Size: " + files.size)
        for (i in files.indices) {
            val inFile = files[i]
            val filePath = inFile.path
            val songId = inFile.name
            Log.d(TAG, "FileName:$filePath   songId:$songId")
//            val outFile = File(outPath, songId)
//            val mCryptoHelper = CryptoHelper(songId)
//            mCryptoHelper.decrypt(FileInputStream(inFile), FileOutputStream(outFile))
            val ds = RentDataSource(songId, DefaultBandwidthMeter(), filePath)
            val datasink = OfflineDataSink(songId, false)
//            val copyDataSource = CopyDataSource(ds,datasink, true)
            FileUtils.copy(ds, datasink)

        }
    }

    override fun onPause() {
        super.onPause()
        releaseExoPlayer()
    }

    override fun onResume() {
        super.onResume()
        initPlayer()
    }

    override fun onStart() {
        super.onStart()
        initPlayer()
    }

    override fun onStop() {
        super.onStop()
        releaseExoPlayer()
    }

    private fun releaseExoPlayer() {
        player.release()
    }
}
