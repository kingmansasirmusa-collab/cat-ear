```kotlin
package com.yourapp.catsear

import kotlin.math.*

class SoundLocator(private val micDistanceMeters: Float = 0.12f) {
    private val sampleRate = 16000
    private val fftSize = 1024 // must be power of 2

    // Output: angle in degrees (0 = front), distance in feet
    fun analyze(left: ShortArray, right: ShortArray): Pair<Float, Float> {
        if (left.size < fftSize || right.size < fftSize) return 0f to 10f
        val angleDeg = estimateDirection(left, right)
        val distanceFt = estimateDistance(left)
        return angleDeg to distanceFt
    }

    private fun estimateDirection(left: ShortArray, right: ShortArray): Float {
        val delay = gccPhat(left, right)
        val maxDelay = micDistanceMeters / 343f
        val clampedDelay = max(-maxDelay, min(maxDelay, delay))
        val angleRad = asin(clampedDelay * 343f / micDistanceMeters)
        return Math.toDegrees(angleRad).toFloat()
    }

    private fun gccPhat(left: ShortArray, right: ShortArray): Float {
        val n = fftSize
        val leftReal = FloatArray(n) { i -> if (i < left.size) left[i].toFloat() / 32768f else 0f }
        val rightReal = FloatArray(n) { i -> if (i < right.size) right[i].toFloat() / 32768f else 0f }
        val leftFFT = FloatArray(n * 2)
        val rightFFT = FloatArray(n * 2)
        FFT.computeRealForward(leftReal, leftFFT)
        FFT.computeRealForward(rightReal, rightFFT)

        val cross = FloatArray(n * 2)
        for (i in 0 until n) {
            val re1 = leftFFT[2 * i]; val im1 = leftFFT[2 * i + 1]
            val re2 = rightFFT[2 * i]; val im2 = rightFFT[2 * i + 1]
            val crossRe = re1 * re2 + im1 * im2
            val crossIm = im1 * re2 - re1 * im2
            val mag = sqrt(crossRe * crossRe + crossIm * crossIm)
            if (mag > 1e-10f) {
                cross[2 * i] = crossRe / mag
                cross[2 * i + 1] = crossIm / mag
            }
        }
        val invOut = FloatArray(n)
        FFT.computeRealInverse(cross, invOut)
        var maxIdx = 0; var maxVal = 0f
        for (i in 0 until n) {
            if (invOut[i] > maxVal) { maxVal = invOut[i]; maxIdx = i }
        }
        val delaySamples = if (maxIdx > n / 2) maxIdx - n else maxIdx
        return delaySamples / sampleRate.toFloat()
    }

    private fun estimateDistance(samples: ShortArray): Float {
        val sum = samples.sumOf { (it.toFloat() * it.toFloat()).toDouble() }.toFloat()
        val rms = sqrt(sum / samples.size)
        // Rough calibration: adjust the reference value based on your phone.
        // A tap at 1 foot might give rms ~2000. Here we use a placeholder mapping.
        return (2000f / (rms + 1e-6f)).coerceIn(0.5f, 10f)
    }
}
```
