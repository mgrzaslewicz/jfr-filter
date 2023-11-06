package com.mg.jfr

import com.mg.jfr.*
import jdk.jfr.consumer.RecordingFile
import mu.KLogging
import java.io.DataInputStream
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.inputStream

class JfrParser(private val jfrFile: Path, private val listener: JfrEventListener) {
    private companion object : KLogging()
    private class EventHeader(val eventSizeIncludingHeader: Leb128Long, val eventType: Leb128Long) {
        val headerBytes: ByteArray = eventSizeIncludingHeader.bytes + eventType.bytes
        fun payloadSize(): Int = (eventSizeIncludingHeader.value - headerBytes.size).toInt()
    }

    private val countingStream by lazy { CountingInputStream(jfrFile.inputStream().buffered()) }
    private val dataStream by lazy { DataInputStream(countingStream) }
    private val jdkJfrRecording by lazy { RecordingFile(jfrFile) }

    private fun streamPosition(): Long = countingStream.count

    fun parse() {
        jdkJfrRecording.use {
            dataStream.use {
                var chunkCounter = 1
                while (dataStream.available() > 0) {
                    logger.debug { "Parsing $jfrFile chunk $chunkCounter ..." }
                    val header: ChunkHeader = ChunkHeader.read(dataStream, absoluteChunkStart = streamPosition())
                    listener.onChunkStart(header)
                    parseChunk(header)
                    listener.onChunkEnd()
                    chunkCounter++
                }
            }
        }
    }

    private fun parseChunk(chunkHeader: ChunkHeader) {
        while (streamPosition() < chunkHeader.absoluteChunkEnd) {
            val eventSize = dataStream.readLongLeb128()
            if (eventSize > 0) {
                val eventType = dataStream.readLongLeb128()
                parseEvent(EventHeader(eventSize, eventType))
            } else {
                throw IllegalStateException("Unexpected event size: $eventSize at position ${streamPosition()}")
            }
        }
    }

    @Throws(IOException::class)
    private fun parseEvent(eventHeader: EventHeader) {
        val eventPayload = getBytes(eventHeader.payloadSize())
        val eventBytes = eventHeader.headerBytes + eventPayload
        when (eventHeader.eventType.value) {
            0L -> listener.onMetadata(eventBytes)
            1L -> listener.onConstantPoolEvent(eventBytes)
            else -> listener.onEvent(jdkJfrRecording.readEvent(), eventBytes)
        }
    }

    @Throws(IOException::class)
    private fun getBytes(eventPayloadSize: Int) = ByteArray(eventPayloadSize).apply {
        dataStream.read(this, 0, eventPayloadSize)
    }

}
