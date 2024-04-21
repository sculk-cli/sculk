package tech.jamalam.modrinth.models

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ModrinthUser(
    public val id: String,
    public val userName: String,
    @SerialName("name")
    public val displayName: String?,
    public val email: String?,
    public val bio: String,
    @SerialName("payout_data")
    public val payoutData: ModrinthUserPayoutData,
    @SerialName("avatar_url")
    public val avatarUrl: String,
    @SerialName("created")
    public val createdTime: Instant,
    public val role: ModrinthUserRole,
    public val badges: Int,
    @SerialName("auth_providers")
    public val authProviders: List<String>?,
    @SerialName("email_verified")
    public val emailVerified: Boolean?,
    @SerialName("has_password")
    public val hasPassword: Boolean?,
    @SerialName("has_totp")
    public val hasTotp: Boolean?,
    @SerialName("github_id")
    private val githubId: Nothing?,
)

@Serializable
public enum class ModrinthUserPayoutData(
    public val balance: Int,
    @SerialName("payout_wallet")
    public val payoutWallet: ModrinthPayoutWallet,
    @SerialName("payout_wallet_type")
    public val payoutWalletType: ModrinthPayoutWalletType,
    @SerialName("payout_address")
    public val payoutAddress: String
)

@Serializable
public enum class ModrinthPayoutWallet {
    @SerialName("paypal")
    Paypal,

    @SerialName("venmo")
    Venmo,
}

@Serializable
public enum class ModrinthPayoutWalletType {
    @SerialName("email")
    Email,

    @SerialName("phone")
    Phone,

    @SerialName("user_handle")
    UserHandle
}

@Serializable
public enum class ModrinthUserRole {
    @SerialName("developer")
    Developer,

    @SerialName("moderator")
    Moderator,

    @SerialName("admin")
    Admin
}
