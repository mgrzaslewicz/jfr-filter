package com.mg.jfr

import com.mg.jfr.api.RestSampler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Instant

class RestSamplerTest {

    @TempDir
    private lateinit var tempDir: Path

    @Test
    fun `should create CSV file with correct format`() {
        // given
        val outputFile = tempDir.resolve("test-samples.csv")

        val sampler = RestSampler(
            url = "http://localhost:9999/non-existent",
            numberOfCalls = 3,
            outputFile = outputFile
        )

        // when
        sampler.sample()

        // then
        assertThat(outputFile).exists()

        val lines = outputFile.toFile().readLines()
        val headerLinesCount = 1
        val dataLinesCount = 3
        assertThat(lines).hasSize(headerLinesCount + dataLinesCount)
        assertThat(lines[0]).isEqualTo("start,end")

        // Check that each data line has two comma-separated numbers
        lines.drop(1).forEach { line ->
            val parts = line.split(",")
            assertThat(parts).hasSize(2)
            assertThat(Instant.parse(parts[0])).isNotNull()
            assertThat(Instant.parse(parts[1])).isNotNull()
        }
    }

}
