package tech.jamalam.curseforge.models

import kotlinx.serialization.Serializable

@Serializable
public data class CurseforgeModAuthor(
    val id: Int,
    val name: String,
    val url: String,
)
