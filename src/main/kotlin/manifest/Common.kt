package tech.jamalam.manifest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ModLoader {
    @SerialName("fabric")
    Fabric,

    @SerialName("forge")
    Forge,

    @SerialName("neoforge")
    Neoforge,

    @SerialName("quilt")
    Quilt;
}
