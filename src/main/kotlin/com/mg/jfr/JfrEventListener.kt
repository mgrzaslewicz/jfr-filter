package com.mg.jfr

import jdk.jfr.consumer.RecordedEvent

interface JfrEventListener {
    fun onChunkStart(header: ChunkHeader) {}
    fun onConstantPoolEvent(bytes: ByteArray) {}
    fun onMetadata(bytes: ByteArray) {}
    fun onEvent(event: RecordedEvent, bytes: ByteArray) {}
    fun onChunkEnd() {}
}
