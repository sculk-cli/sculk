package io.github.sculk_cli.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

class PistonMeta(private val client: HttpClient) {
    private var meta: PistonMetaManifest? = null

    suspend fun getMcVersions(): List<String> {
        if (meta == null) {
            fetchMeta()
        }

        return meta!!.versions.map { it.id }
    }

    private suspend fun fetchMeta() {
        meta = client.get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
            .body<PistonMetaManifest>()
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
