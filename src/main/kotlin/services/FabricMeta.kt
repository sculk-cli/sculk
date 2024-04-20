package tech.jamalam.services

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class FabricMeta(private val client: HttpClient, private val json: Json) {
    suspend fun getLoaderVersions(gameVersion: String): List<FabricLoaderVersion> {
        val response = client.get(
            "https://meta.fabricmc.net/v2/versions/loader/$gameVersion"
        )

        return json.decodeFromString(
            ListSerializer(FabricVersionsResponse.serializer()), response.bodyAsText()
        ).map { it.loader }
    }
}

@Serializable
data class FabricVersionsResponse(val loader: FabricLoaderVersion)

@Serializable
data class FabricLoaderVersion(val version: String)
