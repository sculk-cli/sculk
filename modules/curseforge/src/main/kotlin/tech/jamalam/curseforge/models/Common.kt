package tech.jamalam.curseforge.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal open class EnumAsIntSerializer<T : Enum<*>>(
    serialName: String, val serialize: (v: T) -> Int, val deserialize: (v: Int) -> T
) : KSerializer<T> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(serialName, PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeInt(serialize(value))
    }

    override fun deserialize(decoder: Decoder): T {
        val v = decoder.decodeInt()
        return deserialize(v)
    }
}

@Serializable(with = CurseforgeModLoaderSerializer::class)
public enum class CurseforgeModLoader {
    Any,
    Forge,
    Cauldron,
    LiteLoader,
    Fabric,
    Quilt,
    Neoforge
}

private object CurseforgeModLoaderSerializer :
    EnumAsIntSerializer<CurseforgeModLoader>("CurseforgeModLoader",
        { it.ordinal + 1 },
        { CurseforgeModLoader.entries[it - 1] })
