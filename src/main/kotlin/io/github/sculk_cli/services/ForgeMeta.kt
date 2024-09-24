package io.github.sculk_cli.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

typealias ForgeMavenMetadata = Map<String, List<String>>

class ForgeMeta(private val client: HttpClient) {
    suspend fun getLoaderVersions(gameVersion: String): List<String> {
        return client.get(
            "https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json"
        ).body<ForgeMavenMetadata>()[gameVersion] ?: return emptyList()
    }
}
