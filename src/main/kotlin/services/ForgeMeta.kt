package tech.jamalam.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.json.Json

class ForgeMeta(private val client: HttpClient) {
    suspend fun getLoaderVersions(gameVersion: String): List<String> {
        return client.get(
            "https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json"
        ).body<Map<String, List<String>>>()[gameVersion] ?: return emptyList()
    }
}
