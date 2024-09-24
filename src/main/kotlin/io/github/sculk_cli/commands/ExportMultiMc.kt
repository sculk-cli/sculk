package io.github.sculk_cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import io.github.sculk_cli.multimc.MultiMcPackComponent
import io.github.sculk_cli.multimc.createMultiMcCompatiblePack
import io.github.sculk_cli.Context
import io.github.sculk_cli.pack.ModLoader
import io.github.sculk_cli.pack.Side
import io.github.sculk_cli.util.downloadFileTemp
import io.github.sculk_cli.util.parseUrl
import java.nio.file.Paths

class ExportMultiMc :
    CliktCommand(name = "multimc", help = "Export a MultiMC compatible instance (.zip)") {
    private val packUrl by option().help("The download URL/path for the modpack, to use for automatic updating. If not specified, the pack will not automatically update")
    private val sculkJarLocation by option().help("The location of the Sculk jar, to use for automatic updating. If not specified, Sculk will attempt to locate it automatically.")
    
    override fun run() = runBlocking {
        val ctx = Context.Companion.getOrCreate(terminal)
        val components = mutableListOf<MultiMcPackComponent>()

        components.add(MultiMcPackComponent.Minecraft(ctx.pack.getManifest().minecraft))
        when (ctx.pack.getManifest().loader.type) {
            ModLoader.Fabric -> components.add(MultiMcPackComponent.FabricLoader(ctx.pack.getManifest().loader.version))
            ModLoader.Forge -> components.add(MultiMcPackComponent.MinecraftForge(ctx.pack.getManifest().loader.version))
            ModLoader.Neoforge -> components.add(MultiMcPackComponent.Neoforge(ctx.pack.getManifest().loader.version))
            ModLoader.Quilt -> components.add(MultiMcPackComponent.QuiltLoader(ctx.pack.getManifest().loader.version))
        }

        val files = mutableMapOf<String, ByteArray>()

        if (packUrl == null) {
            terminal.info("No pack URL specified; downloading files...")
            for ((path, manifest) in ctx.pack.getManifests()) {
                if (manifest.side == Side.ServerOnly) continue

                val actualFilePath =
                    ctx.pack.getBasePath().resolve(path).resolveSibling(manifest.filename)
                val relativeFilePath = ctx.pack.getBasePath().relativize(actualFilePath).toString()

                val url = if (manifest.sources.url != null) {
                    manifest.sources.url!!.url
                } else if (manifest.sources.modrinth != null) {
                    manifest.sources.modrinth!!.fileUrl
                } else if (manifest.sources.curseforge != null) {
                    manifest.sources.curseforge!!.fileUrl
                } else {
                    error("No valid source for file $path")
                }

                val file = downloadFileTemp(parseUrl(url))
                files[relativeFilePath] = file.readBytes()
            }

            for (file in ctx.pack.getFiles()) {
                if (file.side == Side.ServerOnly) continue
                val actualFilePath = ctx.pack.getBasePath().resolve(file.path)
                files[file.path] = actualFilePath.toFile().readBytes()
            }
        }

        createMultiMcCompatiblePack(
            path = Paths.get("")
                .resolve("${ctx.pack.getManifest().name}-${ctx.pack.getManifest().version}-multimc.zip"),
            name = ctx.pack.getManifest().name,
            components = components,
            files = files,
            packUrl = packUrl,
            sculkJarLocation = sculkJarLocation
        )

        terminal.info("Exported ${ctx.pack.getManifest().name} to ${ctx.pack.getManifest().name}-${ctx.pack.getManifest().version}-multimc.zip")
    }
}
