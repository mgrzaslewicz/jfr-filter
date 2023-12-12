package com.mg.jfr.api

import com.mg.jfr.ChunkHeader
import com.mg.jfr.FilteringWriter
import com.mg.jfr.JfrEventListener
import com.mg.jfr.JfrParser
import jdk.jfr.consumer.RecordedEvent
import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors.newFixedThreadPool
import java.util.function.Predicate

/**
 * Creates multiple JFR files from a single JFR file.
 * Reading JFR is memory heavy so read it once and write multiple times.
 * This implementation uses multiple threads to write to multiple files.
 */
class ConcurrentJfrFilter(
    private val input: Path,
    /** Each output file will contain only events that match the corresponding predicate. */
    private val outputs: Map<Path, Predicate<RecordedEvent>>,
) : JfrFilter {
    private class ConcurrentJfrEventListener(
        private val listeners: List<JfrEventListener>,
        private val executor: ExecutorService,
    ) : JfrEventListener {
        override fun onChunkStart(header: ChunkHeader) {
            listeners.map { executor.submit { it.onChunkStart(header) } }.forEach { it.get() }
        }

        override fun onConstantPoolEvent(bytes: ByteArray) {
            listeners.map { executor.submit { it.onConstantPoolEvent(bytes) } }.forEach { it.get() }
        }

        override fun onMetadata(bytes: ByteArray) {
            listeners.map { executor.submit { it.onMetadata(bytes) } }.forEach { it.get() }
        }

        override fun onEvent(event: RecordedEvent, bytes: ByteArray) {
            listeners.map { executor.submit { it.onEvent(event, bytes) } }.forEach { it.get() }
        }

        override fun onChunkEnd() {
            listeners.map { executor.submit { it.onChunkEnd() } }.forEach { it.get() }
        }
    }

    override fun filter() {
        val executor = newFixedThreadPool(outputs.size)
        val writers = outputs.map { (output, eventFilter) ->
            FilteringWriter(eventFilter, output.toFile())
        }
        val compositeListener = ConcurrentJfrEventListener(writers, executor)
        val parser = JfrParser(input, compositeListener)
        try {
            parser.parse()
        } finally {
            writers.forEach { it.close() }
            executor.shutdownNow()
        }
    }

}
