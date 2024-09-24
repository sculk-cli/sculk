package io.github.sculk_cli.modrinth.models

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
public enum class ModrinthLoader {
    @SerialName("bukkit")
    Bukkit,

    @SerialName("bungeecord")
    Bungeecord,

    @SerialName("canvas")
    Canvas,

    @SerialName("datapack")
    Datapack,

    @SerialName("fabric")
    Fabric,

    @SerialName("folia")
    Folia,

    @SerialName("forge")
    Forge,

    @SerialName("iris")
    Iris,

    @SerialName("liteloader")
    LiteLoader,

    @SerialName("minecraft")
    Minecraft,

    @SerialName("modloader")
    RisugamiModLoader,

    @SerialName("neoforge")
    NeoForge,

    @SerialName("optifine")
    Optifine,

    @SerialName("paper")
    Paper,

    @SerialName("purpur")
    Purpur,

    @SerialName("quilt")
    Quilt,

    @SerialName("rift")
    Rift,

    @SerialName("spigot")
    Spigot,

    @SerialName("sponge")
    Sponge,

    @SerialName("vanilla")
    Vanilla,

    @SerialName("velocity")
    Velocity,

    @SerialName("waterfall")
    Waterfall,
}

@Serializable
public enum class ModrinthHashAlgorithm {
    @SerialName("sha1")
    SHA1,

    @SerialName("sha512")
    SHA512
}
