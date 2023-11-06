package com.mg.jfr

import java.io.DataInputStream


class Leb128Long(val value: Long, val bytes: ByteArray) {
    override fun toString(): String {
        return "Leb128Long(value=$value, bytes=${bytes.joinToString(",")})"
    }

    operator fun compareTo(other: Int): Int {
        return value.compareTo(other)
    }

}

fun DataInputStream.readLongLeb128(): Leb128Long {
    val bytes = mutableListOf<Byte>()
    var value: Long = 0
    var readValue = 0
    var i = 0
    do {
        readValue = this.read()
        bytes.add(readValue.toByte())
        value = value or ((readValue and 0x7F).toLong() shl 7 * i)
        i++
    } while (readValue and 0x80 != 0 // In fact a fully LEB128 encoded 64bit number could take up to 10 bytes
        // (in order to store 64 bit original value using 7bit slots we need at most 10 of them).
        // However, eg. JMC parser will stop at 9 bytes, assuming that the compressed number is
        // a Java unsigned long (therefore having only 63 bits and they all fit in 9 bytes).
        && i < 9
    )
    return Leb128Long(value, bytes.toByteArray())
}
