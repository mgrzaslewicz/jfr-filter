package com.mg.jfr

import java.io.File
import java.nio.file.Path
import java.time.Instant
import java.util.*
import java.util.zip.ZipFile

object SampleJfr {

    val path: Path by lazy {
        val zippedInput = File(javaClass.getResource("/profiler-result.zip")!!.toURI())
        val destinationDir = File("target/profiler-result-${Instant.now()}")
        destinationDir.mkdirs()
        unzip(zippedInput, destinationDir)
        destinationDir.resolve("profiler-result.jfr").toPath()
    }

    val md5Sum = "041ab4e24a2a311632a2610abae41bb9"

    val sampleThreadId = 161L
    val sampleThreadEventsCount = 33863L

    private fun unzip(file: File, destinationDir: File) {
        val zip = ZipFile(file)
        zip.stream().forEach { entry ->
            val unpackedEntry = destinationDir.resolve(entry.name)
            zip.getInputStream(entry).use { packedStream ->
                unpackedEntry.outputStream().use { unpackedStream ->
                    packedStream.copyTo(unpackedStream)
                }
            }
        }
    }
}
