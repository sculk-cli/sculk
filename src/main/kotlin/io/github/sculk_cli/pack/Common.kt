package io.github.sculk_cli.pack

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

@Serializable
enum class Side {
    @SerialName("both")
    Both,

    @SerialName("client_only")
    ClientOnly,

    @SerialName("server_only")
    ServerOnly;
}
