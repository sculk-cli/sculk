package tech.jamalam.modrinth.models

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ModrinthSearchResponse(
    val hits: List<ModrinthSearchResult>,
    val offset: Int,
    val limit: Int,
    @SerialName("total_hits")
    val totalHits: Int,
)

@Serializable
public data class ModrinthSearchResult(
    @SerialName("project_id")
    public val id: String,
    @SerialName("project_type")
    public val type: ModrinthProjectType,
    public val slug: String,
    public val title: String,
    public val description: String,
    @SerialName("icon_url")
    public val iconUrl: String? = null,
    public val color: Int? = null,
    public val categories: List<String>,
    @SerialName("display_categories")
    public val displayCategories: List<String>,
    @SerialName("client_side")
    public val clientSideSupport: ModrinthEnvSupport,
    @SerialName("server_side")
    public val serverSideSupport: ModrinthEnvSupport,
    public val license: String,
    public val downloads: Int,
    @SerialName("follows")
    public val followers: Int,
    @SerialName("thread_id")
    public val threadId: String? = null,
    @SerialName("monetization_status")
    public val monetizationStatus: ModrinthMonetizationStatus? = null,
    @SerialName("author")
    public val authorUsername: String,
    @SerialName("date_created")
    public val createdTime: Instant,
    @SerialName("date_modified")
    public val modifiedTime: Instant,
    public val versions: List<String>,
    @SerialName("latest_version")
    public val latestVersionId: String,
    @SerialName("gallery")
    public val galleryImages: List<String>,
    @SerialName("featured_gallery")
    public val featuredGalleryImage: String? = null,
)

public enum class ModrinthSearchIndex {
    Relevance,
    Downloads,
    Follows,
    Newest,
    Updated
}
