package com.mg.jfr

import org.checkerframework.checker.index.qual.Positive
import java.io.DataInput
import java.io.DataOutput

/**
 * Based on [jdk.jfr.internal.consumer.ChunkHeader]
 */
data class ChunkHeader(
    val major: Short,
    val minor: Short,
    val chunkSize: Long,
    val constantPoolOffset: Long,
    val metadataOffset: Long,
    val chunkStartNanos: Long,
    val durationNanos: Long,
    val chunkStartTicks: Long,
    val ticksPerSecond: Long,
    val compressed: Boolean,

    val absoluteChunkStart: Long,
) {
    init {
        require(chunkSize > 0) { "chunkSize must be positive" }
        require(constantPoolOffset > 0) { "constantPoolOffset must be positive" }
        require(metadataOffset > 0) { "metadataOffset must be positive" }
        require(chunkStartNanos > 0) { "chunkStartNanos must be positive" }
        require(durationNanos > 0) { "durationNanos must be positive" }
        require(chunkStartTicks > 0) { "chunkStart ticks must be positive" }
        require(ticksPerSecond > 0) { "ticksPerSecond must be positive" }
    }

    val absoluteChunkEnd: Long = absoluteChunkStart + chunkSize
    companion object {
        private val FILE_MAGIC: ByteArray =
            byteArrayOf('F'.code.toByte(), 'L'.code.toByte(), 'R'.code.toByte(), '\u0000'.code.toByte())

        fun read(input: DataInput, absoluteChunkStart: Long): ChunkHeader {
            val magic = ByteArray(FILE_MAGIC.size)
            input.readFully(magic)
            if (!magic.contentEquals(FILE_MAGIC)) {
                throw IllegalArgumentException("Not a JFR file")
            }
            return ChunkHeader(
                major = input.readShort(),
                minor = input.readShort(),
                chunkSize = input.readLong(),
                constantPoolOffset = input.readLong(),
                metadataOffset = input.readLong(),
                chunkStartNanos = input.readLong(),
                durationNanos = input.readLong(),
                chunkStartTicks = input.readLong(),
                ticksPerSecond = input.readLong(),
                compressed = input.readInt() != 0,
                absoluteChunkStart = absoluteChunkStart
            )
        }
    }



    fun write(output: DataOutput) {
        output.write(FILE_MAGIC)
        output.writeShort(major.toInt())
        output.writeShort(minor.toInt())
        output.writeLong(chunkSize)
        output.writeLong(constantPoolOffset)
        output.writeLong(metadataOffset)
        output.writeLong(chunkStartNanos)
        output.writeLong(durationNanos)
        output.writeLong(chunkStartTicks)
        output.writeLong(ticksPerSecond)
        output.writeInt(if (compressed) 1 else 0)
    }

}
