package com.mg.jfr

import com.mg.jfr.api.isExecutionSample
import com.mg.jfr.api.javaThreadId
import jdk.jfr.consumer.RecordedEvent
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import java.time.Instant

class JfrParserTest {
    @Test
    fun shouldParseTimestamps() {
        // given
        val input = SampleJfr.path
        var firstEvent: RecordedEvent? = null
        var lastEvent: RecordedEvent? = null
        val parser = JfrParser(
            jfrFile = input,
            listener = object : JfrEventListener {
                override fun onEvent(event: RecordedEvent, bytes: ByteArray) {
                    if (firstEvent == null) {
                        firstEvent = event
                    }
                    lastEvent = event
                }
            })
        // when
        parser.parse()
        // then
        SoftAssertions.assertSoftly {
            it.assertThat(firstEvent!!.startTime).isEqualTo(Instant.parse("2023-12-11T18:31:13.185198Z"))
            it.assertThat(lastEvent!!.startTime).isEqualTo(Instant.parse("2023-12-11T18:36:51.887484111Z"))
        }
    }

    @Test
    fun shouldParseThreadIds() {
        // given
        val input = SampleJfr.path
        var executionSampleEventsCount = 0
        val threadIdCount = mutableMapOf<Long, Int>()
        val parser = JfrParser(
            jfrFile = input,
            object : JfrEventListener {
                override fun onEvent(event: RecordedEvent, bytes: ByteArray) {
                    if (event.isExecutionSample()) {
                        executionSampleEventsCount++
                        val javaThreadId = event.javaThreadId()
                        if (javaThreadId != null) {
                            threadIdCount.compute(javaThreadId) { _, count -> (count ?: 0) + 1 }
                        }
                    }
                }
            })
        // when
        parser.parse()
        // then
        val allExecutionSamplesHaveThreadId = executionSampleEventsCount == threadIdCount.values.sum()
        SoftAssertions.assertSoftly {
            it.assertThat(allExecutionSamplesHaveThreadId).isTrue()
            it.assertThat(threadIdCount[SampleJfr.sampleThreadId]).isEqualTo(SampleJfr.sampleThreadEventsCount)
        }

    }

}
