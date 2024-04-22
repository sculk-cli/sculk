package tech.jamalam.util

import tech.jamalam.modrinth.ModrinthPackFileEnv
import tech.jamalam.modrinth.models.ModrinthEnvSupport
import tech.jamalam.modrinth.models.ModrinthModLoader
import tech.jamalam.pack.ModLoader
import tech.jamalam.pack.Side

fun modrinthEnvTypePairToSide(clientSide: ModrinthEnvSupport, serverSide: ModrinthEnvSupport) =
    when (clientSide to serverSide) {
        ModrinthEnvSupport.Unsupported to ModrinthEnvSupport.Required -> Side.ServerOnly
        ModrinthEnvSupport.Unsupported to ModrinthEnvSupport.Optional -> Side.ClientOnly
        ModrinthEnvSupport.Required to ModrinthEnvSupport.Unsupported -> Side.ClientOnly
        ModrinthEnvSupport.Optional to ModrinthEnvSupport.Unsupported -> Side.ServerOnly
        else -> Side.Both
    }

fun ModrinthPackFileEnv.toSide() = modrinthEnvTypePairToSide(clientSupport, serverSupport)

fun Side.toModrinthEnvServerSupport() = when (this) {
    Side.ServerOnly -> ModrinthEnvSupport.Required
    Side.ClientOnly -> ModrinthEnvSupport.Unsupported
    Side.Both -> ModrinthEnvSupport.Required
}

fun Side.toModrinthEnvClientSupport() = when (this) {
    Side.ServerOnly -> ModrinthEnvSupport.Unsupported
    Side.ClientOnly -> ModrinthEnvSupport.Required
    Side.Both -> ModrinthEnvSupport.Required
}

fun ModrinthModLoader.toModLoader(): ModLoader = when (this) {
    ModrinthModLoader.NeoForge -> ModLoader.Neoforge
    ModrinthModLoader.Fabric -> ModLoader.Fabric
    ModrinthModLoader.Forge -> ModLoader.Forge
    ModrinthModLoader.Quilt -> ModLoader.Quilt
    else -> error("Unknown Modrinth mod loader: $this")
}

fun ModLoader.toModrinth(): ModrinthModLoader = when (this) {
    ModLoader.Neoforge -> ModrinthModLoader.NeoForge
    ModLoader.Fabric -> ModrinthModLoader.Fabric
    ModLoader.Forge -> ModrinthModLoader.Forge
    ModLoader.Quilt -> ModrinthModLoader.Quilt
}
