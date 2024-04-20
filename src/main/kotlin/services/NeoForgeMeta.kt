package tech.jamalam.services

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class NeoForgeMeta(private val client: HttpClient, private val json: Json) {
    suspend fun getLoaderVersions(gameVersion: String): List<String> {
        val response = client.get(
            "https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge"
        )

        return json.decodeFromString(
            NeoForgeVersionResponse.serializer(), response.bodyAsText()
        ).versions.filter { it.startsWith(gameVersion.trimStart('1', '.')) }.reversed()
    }
}

@Serializable
data class NeoForgeVersionResponse(val versions: List<String>)
