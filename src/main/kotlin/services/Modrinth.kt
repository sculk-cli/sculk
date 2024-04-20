package tech.jamalam.services

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import tech.jamalam.manifest.ModLoader
import java.net.URLEncoder

class Modrinth(private val client: HttpClient, private val json: Json) {
    suspend fun search(query: String): List<ModrinthProject> {
        val query = URLEncoder.encode(query, "UTF-8")
        val response = client.get(
            "https://api.modrinth.com/v2/search?query=$query"
        )
        return json.decodeFromString(
            ModrinthSearchResponse.serializer(), response.bodyAsText()
        ).hits
    }

    suspend fun findVersions(
        projectId: String, modLoader: ModLoader, gameVersion: String
    ): List<ModrinthVersion> {
        val validLoaders = listOf(
            "minecraft", when (modLoader) {
                ModLoader.Fabric -> "fabric"
                ModLoader.Forge -> "forge"
                ModLoader.Neoforge -> "neoforge"
                ModLoader.Quilt -> "quilt"
            }
        )

        val loaders =
            validLoaders.joinToString(",") { "%22$it%22" }
        val gameVersions = "%22$gameVersion%22"
        val response = client.get(
            "https://api.modrinth.com/v2/project/$projectId/version?loaders=[$loaders]&game_versions=[$gameVersions]",
        )

        return json.decodeFromString(
            ListSerializer(ModrinthVersion.serializer()), response.bodyAsText()
        )
    }

    suspend fun reverseLookupVersion(
        sha1Hash: String
    ): ModrinthVersion? {
        val response = client.get(
            "https://api.modrinth.com/v2/version_file/$sha1Hash",
        )

        return json.decodeFromString(
            ModrinthVersion.serializer(), response.bodyAsText()
        )
    }
}

@Serializable
data class ModrinthSearchResponse(
    val hits: List<ModrinthProject>,
)

@Serializable
data class ModrinthProject(
    @SerialName("project_id")
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
)

@Serializable
data class ModrinthVersion(
    val name: String,
    @SerialName("project_id")
    val projectId: String,
    val id: String,
    @SerialName("date_published") val publishedDate: String,
    val loaders: List<String>,
    val files: List<ModrinthFile>,
)

@Serializable
data class ModrinthFile(
    val url: String,
    val filename: String,
    val primary: Boolean,
    val hashes: ModrinthFileHashes,
)

@Serializable
data class ModrinthFileHashes(
    val sha1: String,
    val sha512: String,
)
