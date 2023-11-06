package com.mg.jfr.api

import jdk.jfr.consumer.RecordedEvent
import jdk.jfr.consumer.RecordedThread

fun RecordedEvent.javaThreadId(): Long? {
    return if (this.hasField("sampledThread")) {
        this.getValue<RecordedThread>("sampledThread")?.javaThreadId
    } else {
        null
    }
}

fun RecordedEvent.isExecutionSample(): Boolean {
    return this.eventType.id == 101L
}
