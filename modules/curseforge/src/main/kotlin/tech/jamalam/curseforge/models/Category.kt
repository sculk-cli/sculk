package tech.jamalam.curseforge.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
public data class CurseforgeCategory(
    public val id: Int,
    public val gameId: Int,
    public val name: String,
    public val slug: String,
    public val url: String,
    public val iconUrl: String,
    public val dateModified: Instant,
    public val isClass: Boolean?,
    public val classId: Int?,
    public val parentCategoryId: Int?,
    public val displayIndex: Int?,
)
