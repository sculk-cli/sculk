package tech.jamalam.modrinth.models

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ModrinthProjectDependenciesResponse(
    public val projects: List<ModrinthProject>,
    public val versions: List<ModrinthVersion>,
)

@Serializable
public data class ModrinthProject(
    public val id: String,
    @SerialName("project_type")
    public val type: ModrinthProjectType,
    public val slug: String,
    public val title: String,
    public val description: String,
    public val body: String,
    @SerialName("icon_url") public val iconUrl: String? = null,
    public val color: Int? = null,
    public val categories: List<String>,
    @SerialName("additional_categories") public val additionalCategories: List<String>,
    @SerialName("client_side") public val clientSideSupport: ModrinthEnvSupport,
    @SerialName("server_side") public val serverSideSupport: ModrinthEnvSupport,
    @SerialName("issues_url") public val issuesUrl: String? = null,
    @SerialName("source_url") public val sourceUrl: String? = null,
    @SerialName("wiki_url") public val wikiUrl: String? = null,
    @SerialName("discord_url") public val discordUrl: String? = null,
    @SerialName("donation_urls") public val donationUrls: List<ModrinthProjectDonationUrl>,
    public val license: ModrinthLicense,
    public val downloads: Int,
    public val followers: Int,
    public val status: ModrinthProjectStatus,
    @SerialName("requested_status") public val requestedStatus: ModrinthProjectRequestedStatus? = null,
    @SerialName("moderator_message") public val moderatorMessage: String? = null,
    @SerialName("thread_id") public val threadId: String? = null,
    @SerialName("monetization_status") public val monetizationStatus: ModrinthMonetizationStatus? = null,
    @SerialName("team") public val teamId: String,
    @SerialName("organization") public val organizationId: String? = null,
    @SerialName("published") public val publishedTime: Instant,
    @SerialName("updated") public val updatedTime: Instant,
    @SerialName("approved") public val approvedTime: Instant? = null,
    @SerialName("queued") public val queuedTime: Instant? = null,
    @SerialName("versions") public val versionIds: List<String>,
    public val loaders: List<ModrinthLoader>,
    @SerialName("game_versions") public val gameVersions: List<String>,
    @SerialName("gallery") public val galleryImages: List<ModrinthGalleryImage>? = null,
    @SerialName("body_url") private val bodyUrl: Nothing? = null,
)

@Serializable
public enum class ModrinthProjectStatus {
    @SerialName("approved")
    Approved,

    @SerialName("archived")
    Archived,

    @SerialName("rejected")
    Rejected,

    @SerialName("draft")
    Draft,

    @SerialName("unlisted")
    Unlisted,

    @SerialName("processing")
    Processing,

    @SerialName("withheld")
    Withheld,

    @SerialName("scheduled")
    Scheduled,

    @SerialName("private")
    Private,

    @SerialName("unknown")
    Unknown
}

@Serializable
public enum class ModrinthProjectRequestedStatus {
    @SerialName("approved")
    Approved,

    @SerialName("archived")
    Archived,

    @SerialName("unlisted")
    Unlisted,

    @SerialName("private")
    Private,

    @SerialName("draft")
    Draft
}

@Serializable
public data class ModrinthProjectDonationUrl(
    public val id: String, public val platform: String, public val url: String
)

@Serializable
public enum class ModrinthProjectType {
    @SerialName("mod")
    Mod,

    @SerialName("modpack")
    Modpack,

    @SerialName("resourcepack")
    Resourcepack,

    @SerialName("shader")
    Shader
}

@Serializable
public enum class ModrinthMonetizationStatus {
    @SerialName("monetized")
    Monetized,

    @SerialName("demonetized")
    Demonetized,

    @SerialName("force-demonetized")
    ForceDemonetized,
}

@Serializable
public data class ModrinthLicense(
    public val id: String, public val name: String, public val url: String?
)

@Serializable
public data class ModrinthGalleryImage(
    public val url: String,
    public val featured: Boolean,
    public val title: String?,
    public val description: String?,
    @SerialName("created") public val createdTime: Instant,
    public val ordering: Int
)
