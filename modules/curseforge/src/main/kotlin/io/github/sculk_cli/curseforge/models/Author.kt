package io.github.sculk_cli.curseforge.models

import kotlinx.serialization.Serializable

@Serializable
public data class CurseforgeModAuthor(
    val id: Int,
    val name: String,
    val url: String,
)
