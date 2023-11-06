package com.mg.jfr.api

import com.mg.jfr.FilteringWriter
import com.mg.jfr.JfrParser
import jdk.jfr.consumer.RecordedEvent
import java.nio.file.Path
import java.util.function.Predicate

class JfrFilter(private val input: Path) {
    fun filter(
        eventFilter: Predicate<RecordedEvent>,
        output: Path = input.resolveSibling("filtered-" + input.fileName.toString())
    ): Path {
        FilteringWriter(eventFilter, output.toFile()).use {
            val parser = JfrParser(input, it)
            parser.parse()
            return output
        }
    }

}
