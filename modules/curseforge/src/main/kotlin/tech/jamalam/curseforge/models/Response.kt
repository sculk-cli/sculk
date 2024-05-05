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

@Serializable
internal data class CurseforgeFingerprintMatchesResult(
    val isCacheBuilt: Boolean,
    val exactMatches: List<CurseforgeFingerprintMatch>,
    val exactFingerprints: List<Long>,
    val partialMatches: List<CurseforgeFingerprintMatch>,
    val partialMatchFingerprints: Map<String, List<Long>>,
    val additionalProperties: List<Int>?,
    val installedFingerprints: List<Long>,
    val unmatchedFingerprints: List<Long>?,
)

@Serializable
internal data class CurseforgeFingerprintMatch(
    val id: Int,
    val file: CurseforgeFile,
    val latestFiles: List<CurseforgeFile>,
)
