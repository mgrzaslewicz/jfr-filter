package com.mg.jfr

import java.io.FilterInputStream
import java.io.InputStream

class CountingInputStream(private val decorated: InputStream) : FilterInputStream(decorated) {
    var count = 0L
        private set

    override fun read(): Int {
        val read = decorated.read()
        if (read != -1) {
            count++
        }
        return read
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val read = super.read(b, off, len)
        if (read != -1) {
            count += read
        }
        return read
    }

}
