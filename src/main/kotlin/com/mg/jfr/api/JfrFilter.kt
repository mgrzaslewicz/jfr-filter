package com.mg.jfr.api

import com.mg.jfr.FilteringWriter
import com.mg.jfr.JfrParser
import jdk.jfr.consumer.RecordedEvent
import java.nio.file.Path
import java.util.function.Predicate

interface JfrFilter {
    fun filter()
}

