package tech.jamalam.services

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class PistonMeta(private val client: HttpClient) {
    private val json = Json { ignoreUnknownKeys = true }
    private var meta: PistonMetaManifest? = null

    suspend fun getMcVersions(): List<String> {
        if (meta == null) {
            fetchMeta()
        }

        return meta!!.versions.map { it.id }
    }

    private suspend fun fetchMeta() {
        val response = client.get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
        meta = json.decodeFromString(PistonMetaManifest.serializer(), response.bodyAsText())
    }
}

@Serializable
data class PistonMetaManifest(
    val versions: List<PistonMetaVersion>
)

@Serializable
data class PistonMetaVersion(
    val id: String
)
