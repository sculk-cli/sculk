package tech.jamalam.curseforge.models

import kotlinx.serialization.Serializable

@Serializable
public data class CurseforgeResponse<T>(val data: T)

@Serializable
public data class PaginatedCurseforgeResponse<T>(
    val data: T,
    val pagination: CurseforgeResponsePagination
)

@Serializable
public data class CurseforgeResponsePagination(
    val index: Int,
    val pageSize: Int,
    val resultCount: Int,
    val totalCount: Int
)
