package com.yourapp.catsear

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.task.audio.classifier.Classifications
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var radarView: RadarView
    private lateinit var btnStart: Button
    private lateinit var adContainer: FrameLayout

    private var isDetecting = false
    private var audioRecord: AudioRecord? = null
    private var backgroundThread = Executors.newSingleThreadExecutor()
    private val soundLocator = SoundLocator()
    private var classifier: AudioClassifier? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        radarView = findViewById(R.id.radarView)
        btnStart = findViewById(R.id.btnStart)
        adContainer = findViewById(R.id.adContainer)

        btnStart.setOnClickListener { toggleDetection() }

        // Attempt to load the AI model for sound classification
        try {
            classifier = AudioClassifier.createFromFile(this, "sound_model.tflite")
        } catch (e: Exception) {
            // Model not found – app will still detect general sound direction
            Toast.makeText(this, "AI model missing – detecting any sound", Toast.LENGTH_LONG).show()
        }

        // Ad activation placeholder (commented out)
        // MobileAds.initialize(this) {}
        // showAdBanner()
    }

    private fun toggleDetection() {
        if (isDetecting) stopDetection() else startDetection()
    }

    private fun startDetection() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            return
        }
        val sampleRate = 16000
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC, sampleRate,
            AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize * 2
        )
        audioRecord?.startRecording()
        isDetecting = true
        btnStart.text = "STOP"
        backgroundThread.execute {
            val stereoBuffer = ShortArray(bufferSize)
            while (isDetecting && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val read = audioRecord!!.read(stereoBuffer, 0, stereoBuffer.size)
                if (read > 0) {
                    val left = ShortArray(read / 2)
                    val right = ShortArray(read / 2)
                    for (i in 0 until read step 2) {
                        left[i / 2] = stereoBuffer[i]
                        right[i / 2] = stereoBuffer[i + 1]
                    }
                    // Get direction/distance from sound locator
                    val (angleDeg, distanceFt) = soundLocator.analyze(left, right)

                    // Determine label: use AI classifier if available, else "Sound"
                    var label = "Sound"
                    if (classifier != null) {
                        try {
                            val tensorAudio = classifier!!.createInputTensorAudio()
                            val floatSamples = FloatArray(left.size) { left[it].toFloat() / 32768f }
                            tensorAudio.load(floatSamples)
                            val results: List<Classifications> = classifier!!.classify(tensorAudio)
                            if (results.isNotEmpty()) {
                                val top = results[0].categories.maxByOrNull { it.score }
                                if (top != null && top.score > 0.6) {
                                    label = top.label
                                }
                            }
                        } catch (_: Exception) { }
                    }

                    // Only update radar if sound is loud enough (avoid silence)
                    if (isLoudEnough(left)) {
                        runOnUiThread {
                            radarView.addDetection(
                                RadarView.Detection(label, angleDeg, distanceFt, System.currentTimeMillis())
                            )
                        }
                    }
                }
            }
        }
    }

    private fun stopDetection() {
        isDetecting = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        btnStart.text = "START"
    }

    private fun isLoudEnough(samples: ShortArray): Boolean {
        val sum = samples.sumOf { (it.toFloat() * it.toFloat()).toDouble() }
        val rms = kotlin.math.sqrt(sum / samples.size)
        return rms > 200 // adjust threshold as needed
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startDetection()
        }
    }

    override fun onDestroy() {
        stopDetection()
        classifier?.close()
        super.onDestroy()
    }

    // Uncomment and fill when you want to enable ads
    /*
    private fun showAdBanner() {
        val adView = AdView(this)
        adView.adSize = AdSize.BANNER
        adView.adUnitId = getString(R.string.admob_banner_id)
        adContainer.removeAllViews()
        adContainer.addView(adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
        adContainer.visibility = View.VISIBLE
    }
    */
}
