package tech.jamalam.pack

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

// A file that is included in the modpack, but does not have a manifest file (e.g. a config file)
data class DirectFile(val path: String)


