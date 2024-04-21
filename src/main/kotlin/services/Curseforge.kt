package tech.jamalam.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import tech.jamalam.pack.ModLoader
import java.util.*

class Curseforge(private val client: HttpClient) {
    private val baseUrl: String
    private val apiKey: String

    init {
        this::class.java.getResourceAsStream("/curseforge-credentials.properties").use {
            val properties = Properties()
            properties.load(it)
            baseUrl = properties.getProperty("api_url")
            apiKey = properties.getProperty("api_key")
        }
    }

    suspend fun searchBySlug(slug: String, gameVersion: String): List<CurseforgeProject> {
        return get("mods/search?gameId=0&slug=$slug&gameVersion=$gameVersion").body<SearchResponse>().data
    }

    suspend fun search(slug: String, gameVersion: String): List<CurseforgeProject> {
        return get("mods/search?gameId=432&searchFilter=$slug&gameVersion=$gameVersion").body<SearchResponse>().data
    }

    suspend fun getValidVersions(
        id: Int,
        loader: ModLoader,
        gameVersion: String
    ): List<CurseforgeFile> {
        return get("mods/$id/files?gameVersion=$gameVersion&modLoaderType=${loader.toCurseforgeId()}").body<GetModFilesResponse>().data
    }

    private suspend inline fun get(path: String): HttpResponse {
        return client.get("$baseUrl/$path") {
            if (apiKey != "unauthenticated") {
                header("x-api-key", apiKey)
            }
        }
    }
}

fun ModLoader.toCurseforgeId(): Int {
    return when (this) {
        ModLoader.Fabric -> 4
        ModLoader.Neoforge -> 6
        ModLoader.Forge -> 1
        ModLoader.Quilt -> 5
    }
}

@Serializable
data class SearchResponse(val data: List<CurseforgeProject>)

@Serializable
data class CurseforgeProject(
    val id: Int,
    val name: String,
    val slug: String,
    val summary: String,
)

@Serializable
data class GetModFilesResponse(val data: List<CurseforgeFile>)

@Serializable
data class CurseforgeFile(
    val id: Int,
    val displayName: String,
    val downloadUrl: String,
    val fileName: String,
    val fileDate: String,
)
