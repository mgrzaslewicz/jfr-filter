package com.mg.jfr

import com.mg.jfr.api.JfrFilter
import com.mg.jfr.api.javaThreadId
import jdk.jfr.consumer.RecordedEvent
import mu.KLogging
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.security.MessageDigest
import java.util.function.Predicate
import kotlin.io.path.fileSize

class JfrFilterTest {
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
        val jfrFilter = JfrFilter(expectedInput)
        // when
        val actualOutput = jfrFilter.filter(
            eventFilter = { true },
            output = expectedInput.resolveSibling("shouldRewriteWithoutChanges-" + expectedInput.fileName.toString())
        )
        // then
        SoftAssertions.assertSoftly {
            it.assertThat(actualOutput).hasSize(expectedInput.fileSize())
            it.assertThat(actualOutput.md5Sum()).isEqualTo(SampleJfr.md5Sum)
        }
    }

    @Test
    fun shouldFilterByThreadId() {
        // given
        val sampleInput = SampleJfr.path
        val expectedThreadCounter = mutableMapOf<Long?, Long>()
        val filter = Predicate<RecordedEvent> {
            val javaThreadId = it.javaThreadId()
            expectedThreadCounter.compute(javaThreadId) { _, count -> (count ?: 0) + 1 }
            javaThreadId == SampleJfr.sampleThreadId
        }
        // when
        logger.debug("Filtering JFR ...")
        val jrfFilter = JfrFilter(sampleInput)
        val filteredFile = jrfFilter.filter(
            eventFilter = filter,
            output = sampleInput.resolveSibling("shouldFilterByThreadId-" + sampleInput.fileName.toString())
        )
        // then
        val actualThreadCounter = mutableMapOf<Long?, Long>()
        JfrParser(jfrFile = filteredFile, listener = object : JfrEventListener {
            override fun onEvent(event: RecordedEvent, bytes: ByteArray) {
                val javaThreadId = event.javaThreadId()
                actualThreadCounter.compute(javaThreadId) { _, count -> (count ?: 0) + 1 }
            }
        }).parse()
        assertThat(actualThreadCounter[SampleJfr.sampleThreadId]).isEqualTo(expectedThreadCounter[SampleJfr.sampleThreadId])
        assertThat(actualThreadCounter[SampleJfr.sampleThreadId]).isEqualTo(SampleJfr.sampleThreadEventsCount)
        actualThreadCounter.remove(SampleJfr.sampleThreadId)
        assertThat(actualThreadCounter).isEmpty()
    }

}
