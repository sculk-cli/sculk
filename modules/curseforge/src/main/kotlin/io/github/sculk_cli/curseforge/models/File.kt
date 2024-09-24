package io.github.sculk_cli.curseforge.models

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class CurseforgeFile(
    public val id: Int,
    public val gameId: Int,
    public val modId: Int,
    public val isAvailable: Boolean,
    public val displayName: String,
    public val fileName: String,
    public val releaseType: CurseforgeFileReleaseType,
    public val fileStatus: CurseforgeFileStatus,
    public val hashes: List<CurseforgeFileHash>,
    public val fileDate: Instant,
    @SerialName("fileLength")
    public val fileLengthInBytes: Long,
    public val downloadCount: Long,
    private val fileSizeOnDisk: Long?,
    public val downloadUrl: String?,
    public val gameVersions: List<String>,
    public val sortableGameVersions: List<CurseforgeSortableGameVersion>,
    public val dependencies: List<CurseforgeFileDependency>,
    public val exposeAsAlternative: Boolean?,
    public val parentProjectFileId: Int?,
    public val alternateFileId: Int?,
    public val isServerPack: Boolean?,
    public val serverPackFileId: Int?,
    public val isEarlyAccessContent: Boolean?,
    public val earlyAccessEndDate: Instant?,
    public val fileFingerprint: Long?,
    public val modules: List<CurseforgeFileModule>
)

public fun CurseforgeFile.getActualGameVersions(): List<String> =
    this.gameVersions.filter { it.contains("1.") }

public fun CurseforgeFile.getSide(): CurseforgeSide = this.gameVersions
    .filter { it == "Client" || it == "Server" }
    .map {
        if (it == "Client") {
            CurseforgeSide.Client
        } else {
            CurseforgeSide.Server
        }
    }
    .let {
        if (it.size == 1) {
            it[0]
        } else {
            CurseforgeSide.Both
        }
    }

public enum class CurseforgeSide {
    Client,
    Server,
    Both
}

@Serializable
public data class CurseforgeFileIndex(
    public val gameVersion: String,
    public val fileId: Int,
    public val filename: String,
    public val releaseType: CurseforgeFileReleaseType,
    public val gameVersionTypeId: Int?,
    public val modLoader: CurseforgeModLoader?,
)

@Serializable(with = CurseforgeFileReleaseTypeSerializer::class)
public enum class CurseforgeFileReleaseType {
    Release,
    Beta,
    Alpha
}

private object CurseforgeFileReleaseTypeSerializer :
    EnumAsIntSerializer<CurseforgeFileReleaseType>("CurseforgeFileReleaseType",
        { it.ordinal + 1 },
        { CurseforgeFileReleaseType.entries[it - 1] })

@Serializable(with = CurseforgeFileStatusSerializer::class)
public enum class CurseforgeFileStatus {
    Processing,
    ChangesRequired,
    UnderReview,
    Approved,
    Rejected,
    MalwareDetected,
    Deleted,
    Archived,
    Testing,
    Released,
    ReadyForReview,
    Deprecated,
    Baking,
    AwaitingPublishing,
    FailedPublishing
}

private object CurseforgeFileStatusSerializer :
    EnumAsIntSerializer<CurseforgeFileStatus>("CurseforgeFileStatus",
        { it.ordinal + 1 },
        { CurseforgeFileStatus.entries[it - 1] })

@Serializable
public data class CurseforgeFileHash(
    public val value: String,
    public val algo: CurseforgeHashAlgo,
)

@Serializable(with = CurseforgeHashAlgoSerializer::class)
public enum class CurseforgeHashAlgo {
    Sha1,
    Md5
}

private object CurseforgeHashAlgoSerializer :
    EnumAsIntSerializer<CurseforgeHashAlgo>("CurseforgeHashAlgo",
        { it.ordinal + 1 },
        { CurseforgeHashAlgo.entries[it - 1] })

@Serializable
public data class CurseforgeSortableGameVersion(
    public val gameVersionName: String,
    public val gameVersionPadded: String,
    public val gameVersion: String,
    public val gameVersionReleaseDate: Instant,
    public val gameVersionTypeId: Int?,
)

@Serializable
public data class CurseforgeFileDependency(
    public val modId: Int,
    public val relationType: CurseforgeFileRelationType,
)

@Serializable(with = CurseforgeFileRelationTypeSerializer::class)
public enum class CurseforgeFileRelationType {
    EmbeddedLibrary,
    OptionalDependency,
    RequiredDependency,
    Tool,
    Incompatible,
    Include
}

private object CurseforgeFileRelationTypeSerializer :
    EnumAsIntSerializer<CurseforgeFileRelationType>("CurseforgeFileRelationType",
        { it.ordinal + 1 },
        { CurseforgeFileRelationType.entries[it - 1] })

@Serializable
public data class CurseforgeFileModule(
    public val name: String,
    public val fingerprint: Long,
)
