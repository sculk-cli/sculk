package tech.jamalam.curseforge

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import tech.jamalam.curseforge.models.*
import tech.jamalam.util.buildUrl

public const val CURSEFORGE_OFFICIAL_API_URL: String = "api.curseforge.com"
public const val CURSE_TOOLS_API_URL: String = "api.curse.tools"
public const val CURSEFORGE_OFFICIAL_API_BASE_PATH: String = "/v1"
public const val CURSE_TOOLS_API_BASE_PATH: String = "/cf/v1"

public const val CURSEFORGE_MODS_CLASS: Int = 6
public const val CURSEFORGE_DATAPACKS_CLASS: Int = 6945
public const val CURSEFORGE_SHADERPACKS_CLASS: Int = 6552
public const val CURSEFORGE_RESOURCEPACKS_CLASS: Int = 12

private const val HEADER_API_KEY: String = "x-api-key"
private const val MINECRAFT_GAME_ID: Int = 432

// Much less complete than the Modrinth API wrapper, because the Curseforge API is a pain so I only did what was necessary
public class CurseforgeApi(
    private val client: HttpClient,
    private val apiUrl: String = CURSE_TOOLS_API_URL,
    private val basePath: String = CURSE_TOOLS_API_BASE_PATH,
    private val token: String? = null
) {
    public suspend fun search(
        gameId: Int = MINECRAFT_GAME_ID,
        classId: Int? = null,
        categoryId: Int? = null,
        categoryIds: List<Int>? = null,
        gameVersion: String? = null,
        gameVersions: List<String>? = null,
        searchFilter: String? = null,
        sortField: CurseforgeSearchSortField? = null,
        sortOrder: CurseforgeSearchSortOrder? = null,
        modLoader: CurseforgeModLoader? = null,
        modLoaders: List<CurseforgeModLoader>? = null,
        gameVersionTypeId: Int? = null,
        authorId: Int? = null,
        primaryAuthorId: Int? = null,
        slug: String? = null,
        pagination: CurseforgePaginationParameters = CurseforgePaginationParameters()
    ): List<CurseforgeMod> {
        val response = get(buildUrl {
            host(apiUrl)
            path("$basePath/mods/search")
            parameter("gameId", gameId)
            optionalParameter("classId", classId)
            optionalParameter("categoryId", categoryId)
            optionalParameter("categoryIds", categoryIds?.joinToString(",") { it.toString() })
            optionalParameter("gameVersion", gameVersion)
            optionalParameter("gameVersions", gameVersions?.joinToString(","))
            optionalParameter("searchFilter", searchFilter)
            optionalParameter("sortField", sortField?.ordinal?.plus(1))
            optionalParameter(
                "sortOrder", when (sortOrder) {
                    CurseforgeSearchSortOrder.Ascending -> "asc"
                    CurseforgeSearchSortOrder.Descending -> "desc"
                    else -> null
                }
            )
            optionalParameter("modLoader", modLoader?.ordinal?.plus(1))
            optionalParameter("modLoader",
                modLoaders?.joinToString(",") { (it.ordinal + 1).toString() })
            optionalParameter("gameVersionTypeId", gameVersionTypeId)
            optionalParameter("authorId", authorId)
            optionalParameter("primaryAuthorId", primaryAuthorId)
            optionalParameter("slug", slug)
            parameter("index", pagination.firstIndex)
            parameter("pageSize", pagination.pageSize)
        })

        return response.body<PaginatedCurseforgeResponse<List<CurseforgeMod>>>().data
    }

    public suspend fun getMod(
        modId: Int
    ): CurseforgeMod? {
        val response = get(buildUrl {
            host(apiUrl)
            path("$basePath/mods/$modId")
        })

        return if (response.status == HttpStatusCode.OK) {
            response.body<CurseforgeResponse<CurseforgeMod>>().data
        } else {
            null
        }
    }

    public suspend fun getModFiles(
        modId: Int,
        gameVersion: String? = null,
        modLoader: CurseforgeModLoader? = null,
    ): List<CurseforgeFile> {
        val response = get(buildUrl {
            host(apiUrl)
            path("$basePath/mods/$modId/files")

            if (gameVersion != null) {
                parameter("gameVersion", gameVersion)
            }

            if (modLoader != null) {
                parameter("modLoaderType", modLoader.ordinal)
            }
        })

        return response.body<PaginatedCurseforgeResponse<List<CurseforgeFile>>>().data
    }

    public suspend fun getModFile(
        modId: Int,
        fileId: Int,
    ): CurseforgeFile? {
        val response = get(buildUrl {
            host(apiUrl)
            path("$basePath/mods/$modId/files/$fileId")
        })

        return if (response.status == HttpStatusCode.OK) {
            response.body<CurseforgeResponse<CurseforgeFile>>().data
        } else {
            null
        }
    }

    public suspend fun getFingerprintMatches(
        fingerprint: Long,
        gameId: Int = MINECRAFT_GAME_ID,
    ): List<CurseforgeFile> {
        val response = post(buildUrl {
            host(apiUrl)
            path("$basePath/fingerprints/$gameId")
        }, FingerprintRequestBody(listOf(fingerprint)))

        return response.body<CurseforgeResponse<CurseforgeFingerprintMatchesResult>>().data.exactMatches.map { it.file }
    }

    private suspend fun get(url: Url): HttpResponse {
        return client.get(url) {
            if (token != null) {
                header(HEADER_API_KEY, token)
            }
        }
    }

    private suspend fun post(url: Url, body: Any): HttpResponse {
        return client.post(url) {
            if (token != null) {
                header(HEADER_API_KEY, token)
            }

            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }
}

public data class CurseforgePaginationParameters(
    public val firstIndex: Int = 0, public val pageSize: Int = 20
)

@Serializable
private data class FingerprintRequestBody(val fingerprints: List<Long>)
