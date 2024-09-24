package io.github.sculk_cli.modrinth.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ModrinthTeamMember(
    @SerialName("team_id")
    public val teamId: String,
    public val user: ModrinthUser,
    public val role: String,
    public val permissions: Int,
    public val accepted: Boolean,
    @SerialName("payouts_split")
    public val payoutsSplit: String,
    public val ordering: Int,
)
