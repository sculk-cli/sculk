package io.github.sculk_cli.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

class QuiltMeta(private val client: HttpClient) {
    suspend fun getLoaderVersions(gameVersion: String): List<QuiltLoaderVersion> {
        return client.get(
            "https://meta.quiltmc.org/v3/versions/loader/$gameVersion"
        ).body<List<QuiltLoaderVersionsResponse>>().map { it.loader }
    }
}

@Serializable
data class QuiltLoaderVersionsResponse(val loader: QuiltLoaderVersion)

@Serializable
data class QuiltLoaderVersion(val version: String)
