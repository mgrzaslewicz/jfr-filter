package com.mg.jfr

import jdk.jfr.consumer.RecordedEvent
import mu.KLogging
import java.io.DataOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.util.function.Predicate

class FilteringWriter(
    private val eventFilter: Predicate<RecordedEvent>,
    private val outputFile: File,
) : JfrEventListener, AutoCloseable {
    private companion object : KLogging()

    private var lastHeader: ChunkHeader? = null
    private var lastChunkStartPosition = 0L
    private var lastConstantPoolOffset: Long = 0L
    private var lastMetadataOffset: Long = 0L

    private val countingOutput by lazy { CountingOutputStream(outputFile.outputStream().buffered()) }
    private val output by lazy { DataOutputStream(countingOutput) }

    override fun onChunkStart(header: ChunkHeader) {
        lastHeader = header
        lastChunkStartPosition = countingOutput.position
        header.write(output)
    }

    override fun onConstantPoolEvent(bytes: ByteArray) {
        lastConstantPoolOffset = countingOutput.offsetSinceLastReset
        output.write(bytes)
    }

    override fun onMetadata(bytes: ByteArray) {
        lastMetadataOffset = countingOutput.offsetSinceLastReset
        output.write(bytes)
    }

    override fun onEvent(event: RecordedEvent, bytes: ByteArray) {
        if (eventFilter.test(event)) {
            output.write(bytes)
        }
    }

    override fun onChunkEnd() {
        updateChunk()
        countingOutput.resetOffset()
    }

    private fun updateChunk() {
        output.flush() // save the output, otherwise below update will be lost
        val chunkSize = countingOutput.position - lastChunkStartPosition
        val updatedHeader = lastHeader!!
            .copy(
                chunkSize = chunkSize,
                constantPoolOffset = lastConstantPoolOffset,
                metadataOffset = lastMetadataOffset,
            )
        RandomAccessFile(outputFile, "rw").use {
            it.seek(lastChunkStartPosition)
            updatedHeader.apply {
                logger.debug { "Updating chunk from $lastHeader to $this" }
                write(it)
            }
        }
    }

    override fun close() {
        output.close()
    }

}
