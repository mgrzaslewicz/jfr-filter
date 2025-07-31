package com.mg.jfr.api

import jdk.jfr.consumer.RecordedEvent
import java.io.File
import java.nio.file.Path
import java.time.Instant
import java.util.function.Predicate

class RestFilter(
    private val timeSlots: List<TimeSlot>,
    private val inputJfrFile: Path,
    private val outputJfrFile: Path,
) {

    data class TimeSlot(val start: Instant, val end: Instant)

    fun filter() {
        var isFirstEvent = true
        var matchingTimeSlotEvents = 0
        var matchingEvents = 0

        fun RecordedEvent.isEventDuringTimeSlots(): Boolean {
            if (isFirstEvent) {
                println("Event start:     $startTime")
                println("First time slot: ${timeSlots.first()}")
                println("Last  time slot: ${timeSlots.last()}")
                isFirstEvent = false
            }
            val matchingSlot =
                timeSlots.firstOrNull { timeSlot -> startTime >= timeSlot.start && this.endTime <= timeSlot.end }
            if (matchingSlot != null) {
                matchingTimeSlotEvents++
            }
            return matchingSlot != null
        }

        val predicate = Predicate<RecordedEvent> { event ->
            (!event.isExecutionSample() || event.isEventDuringTimeSlots()).also {
                matchingEvents++
            }
        }
        val jfrFilter = MultiJfrFilter(
            input = inputJfrFile,
            outputs = mapOf(outputJfrFile to predicate)
        )
        jfrFilter.filter()
        println("Matching time slot events: $matchingTimeSlotEvents")
        println("All events: $matchingEvents")
    }
}

private fun Long.toNanoInstant() = Instant.ofEpochSecond(this / 1_000_000, this % 1_000_000 * 1_000)

fun main(args: Array<String>) {
    val inputTimestampsCsv = File(args[0])
    val inputJfrFile = Path.of(args[1])
    val outputJfrFile = Path.of(args[2])
    val csvLines = inputTimestampsCsv.readLines()
    val sampleDurations = csvLines.subList(1, csvLines.size).map { line ->
        val parts = line.split(",")
        RestFilter.TimeSlot(Instant.parse(parts[0]), Instant.parse(parts[1]))
    }
    RestFilter(sampleDurations, inputJfrFile, outputJfrFile).filter()
}
