package tech.jamalam.util

import tech.jamalam.modrinth.models.ModrinthEnvSupport
import tech.jamalam.modrinth.models.ModrinthModLoader
import tech.jamalam.pack.ModLoader
import tech.jamalam.pack.Side

fun modrinthEnvTypePairToSide(clientSide: ModrinthEnvSupport, serverSide: ModrinthEnvSupport) = when (clientSide to serverSide) {
    ModrinthEnvSupport.Unsupported to ModrinthEnvSupport.Required -> Side.ServerOnly
    ModrinthEnvSupport.Unsupported to ModrinthEnvSupport.Optional -> Side.ClientOnly
    ModrinthEnvSupport.Required to ModrinthEnvSupport.Unsupported -> Side.ClientOnly
    ModrinthEnvSupport.Optional to ModrinthEnvSupport.Unsupported -> Side.ServerOnly
    else -> Side.Both
}

fun sideToModrinthEnvTypePair(side: Side) = when (side) {
    Side.ServerOnly -> ModrinthEnvSupport.Unsupported to ModrinthEnvSupport.Required
    Side.ClientOnly -> ModrinthEnvSupport.Required to ModrinthEnvSupport.Unsupported
    Side.Both -> ModrinthEnvSupport.Required to ModrinthEnvSupport.Required
}

fun ModLoader.toModrinth(): ModrinthModLoader = when (this) {
    ModLoader.Neoforge -> ModrinthModLoader.NeoForge
    ModLoader.Fabric -> ModrinthModLoader.Fabric
    ModLoader.Forge -> ModrinthModLoader.Forge
    ModLoader.Quilt -> ModrinthModLoader.Quilt
}
