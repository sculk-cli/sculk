package io.github.sculk_cli.modrinth.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.enums.EnumEntries

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

@Serializable(with = ModrinthLoaderSerializer::class)
public enum class ModrinthLoader(public val serialName: String) {
    Babric("babric"),

    BtaBabric("bta-babric"),

    Bukkit("bukkit"),

    Bungeecord("bungeecord"),

    Canvas("canvas"),

    Datapack("datapack"),

    Fabric("fabric"),

    Folia("folia"),

    Forge("forge"),

    Geyser("geyser"),

    Iris("iris"),

    JavaAgent("java-agent"),

    LegacyFabric("legacy-fabric"),

    LiteLoader("liteloader"),

    Minecraft("minecraft"),

    RisugamiModLoader("modloader"),

    NeoForge("neoforge"),

    NilLoader("nilloader"),

    Optifine("optifine"),

    Paper("paper"),

    Purpur("purpur"),

    Quilt("quilt"),

    Rift("rift"),

    Spigot("spigot"),

    Sponge("sponge"),

    Vanilla("vanilla"),

    Velocity("velocity"),

    Waterfall("waterfall"),

    Unknown("unknown");
}

@Serializable
public enum class ModrinthHashAlgorithm {
    @SerialName("sha1")
    SHA1,

    @SerialName("sha512")
    SHA512
}

internal abstract class SafeEnumSerializer<T : Enum<T>>(
    serialName: String,
    val toString: (T) -> String,
    values: EnumEntries<T>,
    val default: T
) : KSerializer<T> {
    val fromString = values.associateBy(toString)

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): T {
        return fromString[decoder.decodeString()] ?: default
    }

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(toString(value))
    }
}

private object ModrinthLoaderSerializer : SafeEnumSerializer<ModrinthLoader>(
    "ModrinthLoader",
    ModrinthLoader::serialName,
    ModrinthLoader.entries,
    ModrinthLoader.Unknown
)