package tech.jamalam.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

class NeoForgeMeta(private val client: HttpClient) {
    suspend fun getLoaderVersions(gameVersion: String): List<String> {
        return client.get(
            "https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge"
        ).body<NeoForgeVersionResponse>().versions.filter {
            it.startsWith(
                gameVersion.trimStart(
                    '1',
                    '.'
                )
            )
        }.reversed()
    }
}

@Serializable
data class NeoForgeVersionResponse(val versions: List<String>)
