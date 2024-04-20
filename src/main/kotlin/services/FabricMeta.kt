package tech.jamalam.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class FabricMeta(private val client: HttpClient) {
    suspend fun getLoaderVersions(gameVersion: String): List<FabricLoaderVersion> {
        return client.get(
            "https://meta.fabricmc.net/v2/versions/loader/$gameVersion"
        ).body<List<FabricVersionsResponse>>().map { it.loader }
    }
}

@Serializable
data class FabricVersionsResponse(val loader: FabricLoaderVersion)

@Serializable
data class FabricLoaderVersion(val version: String)
