package tech.jamalam.modrinth.models

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ModrinthVersion(
    public val name: String,
    @SerialName("version_number")
    public val versionNumber: String,
    public val changelog: String,
    public val dependencies: List<ModrinthVersionDependency>,
    @SerialName("game_versions")
    public val gameVersions: List<String>,
    @SerialName("version_type")
    public val type: ModrinthVersionType,
    public val loaders: List<ModrinthLoader>,
    public val featured: Boolean,
    public val status: ModrinthVersionStatus,
    @SerialName("requested_status")
    public val requestedStatus: ModrinthVersionRequestedStatus? = null,
    public val id: String,
    @SerialName("project_id")
    public val projectId: String,
    @SerialName("author_id")
    public val authorId: String,
    @SerialName("date_published")
    public val publishedTime: Instant,
    public val downloads: Int,
    public val files: List<ModrinthVersionFile>,
    @SerialName("changelog_url")
    private val changelogUrl: Nothing? = null,
)

@Serializable
public data class ModrinthVersionDependency(
    @SerialName("version_id")
    public val versionId: String? = null,
    @SerialName("project_id")
    public val projectId: String,
    @SerialName("file_name")
    public val fileName: String? = null,
    @SerialName("dependency_type")
    public val type: ModrinthVersionDependencyType
)

@Serializable
public enum class ModrinthVersionDependencyType {
    @SerialName("required")
    Required,

    @SerialName("optional")
    Optional,

    @SerialName("incompatible")
    Incompatible,

    @SerialName("embedded")
    Embedded
}

@Serializable
public enum class ModrinthVersionType {
    @SerialName("release")
    Release,

    @SerialName("beta")
    Beta,

    @SerialName("alpha")
    Alpha
}

@Serializable
public enum class ModrinthVersionStatus {
    @SerialName("listed")
    Listed,

    @SerialName("archived")
    Archived,

    @SerialName("draft")
    Draft,

    @SerialName("unlisted")
    Unlisted,

    @SerialName("scheduled")
    Scheduled,

    @SerialName("unknown")
    Unknown
}

@Serializable
public enum class ModrinthVersionRequestedStatus {
    @SerialName("listed")
    Listed,

    @SerialName("archived")
    Archived,

    @SerialName("draft")
    Draft,

    @SerialName("unlisted")
    Unlisted
}

@Serializable
public data class ModrinthVersionFile(
    val hashes: ModrinthVersionFileHashes,
    @SerialName("url")
    val downloadUrl: String,
    val filename: String,
    val primary: Boolean,
    @SerialName("size")
    val sizeInBytes: Long,
    @SerialName("file_type")
    val type: ModrinthVersionFileType?
)

@Serializable
public data class ModrinthVersionFileHashes(
    val sha1: String,
    val sha512: String
)

@Serializable
public enum class ModrinthVersionFileType {
    @SerialName("required-resource-pack")
    RequiredResourcePack,

    @SerialName("optional-resource-pack")
    OptionalResourcePack
}
