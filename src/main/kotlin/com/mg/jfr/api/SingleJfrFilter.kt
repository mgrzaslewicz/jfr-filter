package com.mg.jfr.api

import com.mg.jfr.FilteringWriter
import com.mg.jfr.JfrParser
import jdk.jfr.consumer.RecordedEvent
import java.nio.file.Path
import java.util.function.Predicate

class SingleJfrFilter(
    private val input: Path,
    private val eventFilter: Predicate<RecordedEvent>,
    private val output: Path = input.resolveSibling("filtered-" + input.fileName.toString()),
): JfrFilter {
    override fun filter() {
        FilteringWriter(eventFilter, output.toFile()).use { writer ->
            val parser = JfrParser(input, writer)
            parser.parse()
        }
    }

}
