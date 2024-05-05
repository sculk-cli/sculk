package tech.jamalam.util

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import tech.jamalam.Context
import java.io.File

fun parseUrl(url: String): Url {
    return URLBuilder(url).build()
}

suspend fun downloadFileTemp(url: Url): File {
    val tempFile = File.createTempFile("sculk", null)
    tempFile.deleteOnExit()
    tempFile.writeBytes(tryReq(url))

    return tempFile
}

suspend fun tryReq(url: Url, maxAttempts: Int = 3): ByteArray {
    var attempts = 0
    var lastException: Exception? = null

    while (attempts < maxAttempts) {
        try {
            val response = Context.getOrCreate().client.get(url)
            return response.body()
        } catch (e: Exception) {
            attempts += 1
            lastException = e
        }
    }

    error("Failed to complete request to $url after $maxAttempts attempts (last caught exception: $lastException")
}
