package tech.jamalam.curseforge.models

import kotlinx.serialization.Serializable

@Serializable(with = CurseforgeSearchSortFieldSerializer::class)
public enum class CurseforgeSearchSortField {
    Featured,
    Popularity,
    LastUpdated,
    Name,
    Author,
    TotalDownloads,
    Category,
    GameVersion,
    EarlyAccess,
    FeaturedReleased,
    ReleasedDate,
    Rating
}

private object CurseforgeSearchSortFieldSerializer :
    EnumAsIntSerializer<CurseforgeSearchSortField>("CurseforgeSearchSortField",
        { it.ordinal + 1 },
        { CurseforgeSearchSortField.entries[it - 1] })

@Serializable(with = CurseforgeSearchSortOrderSerializer::class)
public enum class CurseforgeSearchSortOrder {
    Ascending,
    Descending
}

private object CurseforgeSearchSortOrderSerializer :
    EnumAsIntSerializer<CurseforgeSearchSortOrder>("CurseforgeSearchSortOrder",
        { it.ordinal + 1 },
        { CurseforgeSearchSortOrder.entries[it - 1] })
