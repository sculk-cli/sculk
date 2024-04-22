package tech.jamalam.modrinth.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public enum class ModrinthEnvSupport {
    @SerialName("required")
    Required,

    @SerialName("optional")
    Optional,

    @SerialName("unsupported")
    Unsupported,

    @SerialName("unknown")
    Unknown,
}

@Serializable
public enum class ModrinthModLoader {
    @SerialName("minecraft")
    Minecraft,

    @SerialName("fabric")
    Fabric,

    @SerialName("forge")
    Forge,

    @SerialName("neoforge")
    NeoForge,

    @SerialName("quilt")
    Quilt,
}

@Serializable
public enum class ModrinthHashAlgorithm {
    @SerialName("sha1")
    SHA1,

    @SerialName("sha512")
    SHA512
}
