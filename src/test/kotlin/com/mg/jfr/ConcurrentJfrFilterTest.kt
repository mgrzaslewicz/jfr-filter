package com.mg.jfr

import com.mg.jfr.api.ConcurrentJfrFilter
import com.mg.jfr.api.MultiJfrFilter
import com.mg.jfr.api.javaThreadId
import jdk.jfr.consumer.RecordedEvent
import mu.KLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.function.Predicate
import kotlin.system.measureTimeMillis

class ConcurrentJfrFilterTest {
    private companion object : KLogging()

    @Test
    @Disabled("It's not in fact true. I was just testing performance of concurrent vs multi implementation. Concurrent is many times slower and it's not surprising having many very quick tasks submitted.")
    fun shouldBeFasterThanMulti() {
        // given
        val sampleInput = SampleJfr.getPath()
        val predicate1Before = Predicate<RecordedEvent> {
            val javaThreadId = it.javaThreadId()
            javaThreadId == 628L
        }
        val predicate2Before = Predicate<RecordedEvent> {
            val javaThreadId = it.javaThreadId()
            javaThreadId == 33L
        }
        val filteredFile1 = sampleInput.resolveSibling("shouldFilterByThreadId-1-" + sampleInput.fileName.toString())
        val filteredFile2 = sampleInput.resolveSibling("shouldFilterByThreadId-2-" + sampleInput.fileName.toString())

        val multiFilter = MultiJfrFilter(
            input = sampleInput,
            outputs = mapOf(filteredFile1 to predicate1Before, filteredFile2 to predicate2Before),
        )
        val concurrentFilter = ConcurrentJfrFilter(
            input = sampleInput,
            outputs = mapOf(filteredFile1 to predicate1Before, filteredFile2 to predicate2Before),
        )
        // when
        logger.debug("Filtering JFR using MultiJfrFilter implementation ...")
        val multiDuration = measureTimeMillis {
            multiFilter.filter()
        }
        logger.debug("Filtering JFR using ConcurrentJfrFilter implementation ...")
        val concurrentDuration = measureTimeMillis {
            concurrentFilter.filter()
        }
        // then
        assertThat(concurrentDuration).isLessThan(multiDuration)
    }
}
