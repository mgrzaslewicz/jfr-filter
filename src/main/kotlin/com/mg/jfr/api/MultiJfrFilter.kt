package com.mg.jfr.api

import com.mg.jfr.ChunkHeader
import com.mg.jfr.FilteringWriter
import com.mg.jfr.JfrEventListener
import com.mg.jfr.JfrParser
import jdk.jfr.consumer.RecordedEvent
import java.nio.file.Path
import java.util.function.Predicate

/**
 * Creates multiple JFR files from a single JFR file.
 * Reading JFR is memory heavy so read it once and write multiple times.
 */
class MultiJfrFilter(
    private val input: Path,
    /** Each output file will contain only events that match the corresponding predicate. */
    private val outputs: Map<Path, Predicate<RecordedEvent>>,
) : JfrFilter {
    private class CompositeJfrEventListener(
        private val listeners: List<JfrEventListener>
    ) : JfrEventListener {
        override fun onChunkStart(header: ChunkHeader) {
            listeners.forEach { it.onChunkStart(header) }
        }

        override fun onConstantPoolEvent(bytes: ByteArray) {
            listeners.forEach { it.onConstantPoolEvent(bytes) }
        }

        override fun onMetadata(bytes: ByteArray) {
            listeners.forEach { it.onMetadata(bytes) }
        }

        override fun onEvent(event: RecordedEvent, bytes: ByteArray) {
            listeners.forEach { it.onEvent(event, bytes) }
        }

        override fun onChunkEnd() {
            listeners.forEach { it.onChunkEnd() }
        }
    }

    override fun filter() {
        val writers = outputs.map { (output, eventFilter) ->
            FilteringWriter(eventFilter, output.toFile())
        }
        val compositeListener = CompositeJfrEventListener(writers)
        val parser = JfrParser(input, compositeListener)
        try {
            parser.parse()
        } finally {
            writers.forEach { it.close() }
        }
    }

}
