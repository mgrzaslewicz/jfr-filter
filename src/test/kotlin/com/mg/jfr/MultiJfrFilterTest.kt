package com.mg.jfr

import com.mg.jfr.api.MultiJfrFilter
import com.mg.jfr.api.javaThreadId
import jdk.jfr.consumer.RecordedEvent
import mu.KLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.security.MessageDigest
import java.util.function.Predicate
import kotlin.io.path.fileSize

class MultiJfrFilterTest {
    private companion object : KLogging()

    private fun Path.md5Sum(): String {
        val md = MessageDigest.getInstance("MD5")
        return this.toFile().inputStream().use { fis ->
            val buffer = ByteArray(8192)
            generateSequence {
                when (val bytesRead = fis.read(buffer)) {
                    -1 -> null
                    else -> bytesRead
                }
            }.forEach { bytesRead -> md.update(buffer, 0, bytesRead) }
            md.digest().joinToString("") { "%02x".format(it) }
        }
    }

    @Test
    fun shouldRewriteWithoutChanges() {
        // given
        val expectedInput = SampleJfr.path
        val actualOutput1 =
            expectedInput.resolveSibling("shouldRewriteWithoutChanges-1-" + expectedInput.fileName.toString())
        val actualOutput2 =
            expectedInput.resolveSibling("shouldRewriteWithoutChanges-2-" + expectedInput.fileName.toString())
        val jfrFilter = MultiJfrFilter(
            input = expectedInput,
            outputs = mapOf(actualOutput1 to Predicate { true }, actualOutput2 to Predicate { true }),
        )
        // when
        jfrFilter.filter()
        // then
        assertThat(actualOutput1).hasSize(expectedInput.fileSize())
        assertThat(actualOutput1.md5Sum()).isEqualTo(SampleJfr.md5Sum)
        assertThat(actualOutput2).hasSize(expectedInput.fileSize())
        assertThat(actualOutput2.md5Sum()).isEqualTo(SampleJfr.md5Sum)
    }

    @Test
    fun shouldFilterByThreadId() {
        // given
        val sampleInput = SampleJfr.path
        val expectedThreadCounter1 = mutableMapOf<Long?, Long>()
        val expectedThreadCounter2 = mutableMapOf<Long?, Long>()
        val filter1 = Predicate<RecordedEvent> {
            val javaThreadId = it.javaThreadId()
            expectedThreadCounter1.compute(javaThreadId) { _, count -> (count ?: 0) + 1 }
            javaThreadId == SampleJfr.sampleThreadId
        }
        val filter2 = Predicate<RecordedEvent> {
            val javaThreadId = it.javaThreadId()
            expectedThreadCounter2.compute(javaThreadId) { _, count -> (count ?: 0) + 1 }
            javaThreadId == SampleJfr.sampleThreadId
        }
        val filteredFile1 = sampleInput.resolveSibling("shouldFilterByThreadId-1-" + sampleInput.fileName.toString())
        val filteredFile2 = sampleInput.resolveSibling("shouldFilterByThreadId-2-" + sampleInput.fileName.toString())
        // when
        logger.debug("Filtering JFR ...")
        val jrfFilter = MultiJfrFilter(
            input = sampleInput,
            outputs = mapOf(filteredFile1 to filter1, filteredFile2 to filter2),
        )
        jrfFilter.filter()
        // then
        val actualThreadCounter1 = mutableMapOf<Long?, Long>()
        JfrParser(jfrFile = filteredFile1, listener = object : JfrEventListener {
            override fun onEvent(event: RecordedEvent, bytes: ByteArray) {
                val javaThreadId = event.javaThreadId()
                actualThreadCounter1.compute(javaThreadId) { _, count -> (count ?: 0) + 1 }
            }
        }).parse()
        val actualThreadCounter2 = mutableMapOf<Long?, Long>()
        JfrParser(jfrFile = filteredFile2, listener = object : JfrEventListener {
            override fun onEvent(event: RecordedEvent, bytes: ByteArray) {
                val javaThreadId = event.javaThreadId()
                actualThreadCounter2.compute(javaThreadId) { _, count -> (count ?: 0) + 1 }
            }
        }).parse()
        assertThat(actualThreadCounter1[SampleJfr.sampleThreadId]).isEqualTo(expectedThreadCounter1[SampleJfr.sampleThreadId])
        actualThreadCounter1.remove(SampleJfr.sampleThreadId)
        assertThat(actualThreadCounter1).isEmpty()

        assertThat(actualThreadCounter2[SampleJfr.sampleThreadId]).isEqualTo(expectedThreadCounter2[SampleJfr.sampleThreadId])
        actualThreadCounter2.remove(SampleJfr.sampleThreadId)
        assertThat(actualThreadCounter2).isEmpty()
    }

}
