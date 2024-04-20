package tech.jamalam.services

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class QuiltMeta(private val client: HttpClient, private val json: Json) {
    suspend fun getLoaderVersions(gameVersion: String): List<QuiltLoaderVersion> {
        val response = client.get(
            "https://meta.fabricmc.net/v3/versions/loader/$gameVersion"
        )

        return json.decodeFromString(
            ListSerializer(QuiltLoaderVersionsResponse.serializer()), response.bodyAsText()
        ).map { it.loader }
    }
}

@Serializable
data class QuiltLoaderVersionsResponse(val loader: QuiltLoaderVersion)

@Serializable
data class QuiltLoaderVersion(val version: String)
