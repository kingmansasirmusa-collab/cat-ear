package com.yourapp.catsear

import kotlin.math.*

object FFT {
    fun computeRealForward(input: FloatArray, output: FloatArray) {
        val n = input.size
        val real = input.copyOf()
        val imag = FloatArray(n)
        compute(real, imag, forward = true)
        for (i in 0 until n) {
            output[2 * i] = real[i]
            output[2 * i + 1] = imag[i]
        }
    }

    fun computeRealInverse(input: FloatArray, output: FloatArray) {
        val n = input.size / 2
        val real = FloatArray(n) { input[2 * it] }
        val imag = FloatArray(n) { input[2 * it + 1] }
        compute(real, imag, forward = false)
        for (i in 0 until n) {
            output[i] = real[i]
        }
    }

    private fun compute(real: FloatArray, imag: FloatArray, forward: Boolean) {
        val n = real.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tempReal = real[i]; real[i] = real[j]; real[j] = tempReal
                val tempImag = imag[i]; imag[i] = imag[j]; imag[j] = tempImag
            }
        }
        var length = 2
        while (length <= n) {
            val halfLen = length / 2
            val angle = 2.0 * PI / length * (if (forward) -1 else 1)
            val wReal = cos(angle).toFloat()
            val wImag = sin(angle).toFloat()
            for (i in 0 until n step length) {
                var wR = 1.0f
                var wI = 0.0f
                for (k in i until i + halfLen) {
                    val tempReal = wR * real[k + halfLen] - wI * imag[k + halfLen]
                    val tempImag = wR * imag[k + halfLen] + wI * real[k + halfLen]
                    real[k + halfLen] = real[k] - tempReal
                    imag[k + halfLen] = imag[k] - tempImag
                    real[k] += tempReal
                    imag[k] += tempImag
                    val newWr = wR * wReal - wI * wImag
                    wI = wR * wImag + wI * wReal
                    wR = newWr
                }
            }
            length = length shl 1
        }
        if (!forward) {
            for (i in 0 until n) {
                real[i] /= n.toFloat()
                imag[i] /= n.toFloat()
            }
        }
    }
}
