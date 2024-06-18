package tech.jamalam.modrinth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import tech.jamalam.modrinth.models.*
import tech.jamalam.util.buildUrl

public const val MODRINTH_API_URL: String = "api.modrinth.com"
public const val MODRINTH_STAGING_API_URL: String = "staging-api.modrinth.com"

public class ModrinthApi(
    private val client: HttpClient,
    private val apiUrl: String = MODRINTH_API_URL,
) {

    public suspend fun search(
        query: String,
        loaders: List<ModrinthLoader> = listOf(),
        gameVersions: List<String> = listOf(),
        index: ModrinthSearchIndex = ModrinthSearchIndex.Relevance,
        offset: Int = 0,
        limit: Int = 20,
    ): ModrinthSearchResponse {
        val facets = mutableListOf<String>()

        if (loaders.isNotEmpty()) {
            facets += "[${loaders.joinToString(",") { "\"categories:${it.name.lowercase()}\"" }}]"
        }

        if (gameVersions.isNotEmpty()) {
            facets += "[${gameVersions.joinToString(",") { "\"versions:$it\"" }}]"
        }

        val url = buildUrl {
            host(apiUrl)
            path("/v2/search")
            parameter("query", query)

            if (facets.isNotEmpty()) {
                encodedParameter(
                    "facets",
                    "[${facets.joinToString(",").encodeURLParameter().replace("%2C", ",")}]"
                )
            }

            parameter("index", index.name.lowercase())
            parameter("offset", offset.toString())
            parameter("limit", limit.toString())
        }

        return client.get(url).body()
    }

    public suspend fun getProject(idOrSlug: String): ModrinthProject? {
        val url = buildUrl {
            host(apiUrl)
            path("/v2/project/$idOrSlug")
        }

        val response = client.get(url)

        return if (response.status != HttpStatusCode.OK) {
            null
        } else {
            response.body()
        }
    }

    public suspend fun getProjects(idOrSlugs: List<String>): List<ModrinthProject> {
        val url = buildUrl {
            host(apiUrl)
            path("/v2/projects")
            parameter("ids", "[${idOrSlugs.joinToString(",") { "\"$it\"" }.replace("%2C", ",")}]")
        }

        return client.get(url).body()
    }

    public suspend fun getRandomProjects(count: Int): List<ModrinthProject> {
        val url = buildUrl {
            host(apiUrl)
            path("/v2/projects_random")
            parameter("count", count.toString())
        }

        return client.get(url).body()
    }

    public suspend fun checkProjectExists(idOrSlug: String): Boolean {
        val url = buildUrl {
            host(apiUrl)
            path("/v2/project/$idOrSlug/check")
        }

        val response = client.get(url)

        return response.status == HttpStatusCode.OK
    }

    public suspend fun getProjectDependencies(idOrSlug: String): ModrinthProjectDependenciesResponse {
        val url = buildUrl {
            host(apiUrl)
            path("/v2/project/$idOrSlug/dependencies")
        }

        return client.get(url).body()
    }

    public suspend fun getProjectVersions(
        idOrSlug: String,
        loaders: List<ModrinthLoader> = listOf(),
        gameVersions: List<String> = listOf(),
        featured: Boolean? = null
    ): List<ModrinthVersion> {
        val url = buildUrl {
            host(apiUrl)
            path("/v2/project/$idOrSlug/version")

            if (loaders.isNotEmpty()) {
                encodedParameter(
                    "loaders",
                    "[${
                        loaders.joinToString(",") { "\"${it.name.lowercase()}\"" }
                            .encodeURLParameter()
                            .replace("%2C", ",")
                    }]"
                )
            }

            if (gameVersions.isNotEmpty()) {
                encodedParameter(
                    "game_versions",
                    "[${
                        gameVersions.joinToString(",") { "\"$it\"" }.encodeURLParameter()
                            .replace("%2C", ",")
                    }]"
                )
            }

            if (featured != null) {
                parameter("featured", featured.toString())
            }
        }

        return client.get(url).body()
    }

    public suspend fun getVersion(id: String): ModrinthVersion? {
        val url = buildUrl {
            host(apiUrl)
            path("/v2/version/$id")
        }

        val response = client.get(url)

        return if (response.status != HttpStatusCode.OK) {
            null
        } else {
            response.body()
        }
    }

    public suspend fun getVersions(ids: List<String>): List<ModrinthVersion> {
        val url = buildUrl {
            host(apiUrl)
            path("/v2/versions")
            encodedParameter(
                "ids",
                "[${ids.joinToString(",") { "\"$it\"" }.encodeURLParameter().replace("%2C", ",")}]"
            )
        }

        return client.get(url).body()
    }

    public suspend fun getVersionFromVersionNumber(
        projectIdOrSlug: String, versionIdOrNumber: String
    ): ModrinthVersion? {
        val url = buildUrl {
            host(apiUrl)
            path("/v2/project/$projectIdOrSlug/version/$versionIdOrNumber")
        }

        val response = client.get(url)

        return if (response.status != HttpStatusCode.OK) {
            null
        } else {
            response.body()
        }
    }

    public suspend fun getVersionFromHash(
        hash: String, algorithm: ModrinthHashAlgorithm = ModrinthHashAlgorithm.SHA1
    ): ModrinthVersion? {
        val url = buildUrl {
            host(apiUrl)
            path("/v2/version_file/$hash")
            parameter("algorithm", algorithm.name.lowercase())
        }

        val response = client.get(url)

        return if (response.status != HttpStatusCode.OK) {
            null
        } else {
            response.body()
        }
    }

    public suspend fun getVersionsFromHash(
        hash: String, algorithm: ModrinthHashAlgorithm = ModrinthHashAlgorithm.SHA1
    ): List<ModrinthVersion> {
        val url = buildUrl {
            host(apiUrl)
            path("/v2/version_file/$hash")
            parameter("algorithm", algorithm.name.lowercase())
            parameter("multiple", "true")
        }

        return client.get(url).body()
    }

    public suspend fun getUser(idOrUsername: String): ModrinthUser? {
        val url = buildUrl {
            host(apiUrl)
            path("/v2/user/$idOrUsername")
        }

        val response = client.get(url)

        return if (response.status != HttpStatusCode.OK) {
            null
        } else {
            response.body()
        }
    }

    public suspend fun getUsers(idsOrUsernames: List<String>): List<ModrinthVersion> {
        val url = buildUrl {
            host(apiUrl)
            path("/v2/users")
            encodedParameter(
                "ids",
                "[${
                    idsOrUsernames.joinToString(",") { "\"$it\"" }.encodeURLParameter()
                        .replace("%2C", ",")
                }]"
            )
        }

        return client.get(url).body()
    }

    public suspend fun getUserProjects(idOrUsername: String): List<ModrinthProject> {
        val url = buildUrl {
            host(apiUrl)
            path("/v2/user/$idOrUsername/projects")
        }

        return client.get(url).body()
    }
}


