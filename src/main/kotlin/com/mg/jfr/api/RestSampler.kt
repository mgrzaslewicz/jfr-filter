package com.mg.jfr.api

import java.io.FileWriter
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

/**
 * A utility class that makes 100 REST calls to a specified endpoint and records the timing information.
 * The timing data (start and end times in nanoseconds) is appended to a samples.csv file.
 */
class RestSampler(
    private val url: String = "http://localhost:8080/api/v1/har-exploration",
    private val numberOfCalls: Int = 5000,
    private val outputFile: Path = Path.of("samples.csv"),
) {
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    /**
     * Makes the specified number of REST calls to the endpoint and records timing information.
     * Each call's start and end times (in nanoseconds) are appended to the output CSV file.
     */
    fun sample() {
        val file = outputFile.toFile()
        file.delete()

        FileWriter(file, true).use { writer ->
            writer.write("start,end")
            writer.appendLine()

            repeat(numberOfCalls) {
                val request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(url))
                    .build()

                val startTime = Instant.now()
                try {
                    val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                    val endTime = Instant.now()

                    writer.write("$startTime,$endTime\n")

                    println("Request ${it + 1}/$numberOfCalls completed with status: ${response.statusCode()}")
                } catch (e: Exception) {
                    val endTime = Instant.now()
                    writer.write("$startTime,$endTime\n")
                    println("Request ${it + 1}/$numberOfCalls failed: ${e.message}")
                }
            }
        }

        println("Sampling completed. Results saved to $outputFile")
    }
}

fun main() {
    RestSampler().sample()
}
