package io.github.sculk_cli.util

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.github.sculk_cli.Context
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

suspend fun tryReq(url: Url): ByteArray {
    var attempts = 0
    var lastException: Exception? = null

    while (attempts < Context.getOrCreate().requestRetries) {
        try {
            val response = Context.Companion.getOrCreate().client.get(url)
            return response.body()
        } catch (e: Exception) {
            attempts += 1
            lastException = e
        }
    }

    error("Failed to complete request to $url after ${Context.getOrCreate().requestRetries} attempts (last caught exception: $lastException")
}
