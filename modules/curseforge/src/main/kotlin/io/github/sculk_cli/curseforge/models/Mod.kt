package io.github.sculk_cli.curseforge.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
public data class CurseforgeMod(
    public val id: Int,
    private val gameId: Int,
    public val name: String,
    public val slug: String,
    public val links: CurseforgeModLinks,
    public val summary: String,
    public val status: CurseforgeModStatus,
    public val downloadCount: Long,
    public val isFeatured: Boolean,
    public val primaryCategoryId: Int,
    public val categories: List<CurseforgeCategory>,
    public val classId: Int?,
    public val authors: List<CurseforgeModAuthor>,
    public val logo: CurseforgeModAsset?,
    public val screenshots: List<CurseforgeModAsset>,
    public val mainFileId: Int,
    public val latestFiles: List<CurseforgeFile>,
    public val latestFilesIndexes: List<CurseforgeFileIndex>,
    public val latestEarlyAccessFilesIndexes: List<CurseforgeFileIndex>,
    public val dateCreated: Instant,
    public val dateModified: Instant,
    public val dateReleased: Instant,
    public val allowModDistribution: Boolean?,
    public val gamePopularityRank: Int,
    public val isAvailable: Boolean,
    public val thumbsUpCount: Int,
    public val rating: Float?,
)

@Serializable
public data class CurseforgeModLinks(
    public val websiteUrl: String?,
    public val wikiUrl: String?,
    public val issuesUrl: String?,
    public val sourceUrl: String?,
)

@Serializable(with = CurseforgeModStatusSerializer::class)
public enum class CurseforgeModStatus {
    New,
    ChangesRequired,
    UnderSoftReview,
    Approved,
    Rejected,
    ChangesMade,
    Inactive,
    Abandoned,
    Deleted,
    UnderReview
}

private object CurseforgeModStatusSerializer :
    EnumAsIntSerializer<CurseforgeModStatus>("CurseforgeModStatus",
        { it.ordinal + 1 },
        { CurseforgeModStatus.entries[it - 1] })

@Serializable
public data class CurseforgeModAsset(
    public val id: Int,
    public val modId: Int,
    public val title: String,
    public val description: String,
    public val thumbnailUrl: String,
    public val url: String,
)
