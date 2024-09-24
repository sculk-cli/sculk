package io.github.sculk_cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import kotlinx.coroutines.runBlocking
import io.github.sculk_cli.curseforge.*
import io.github.sculk_cli.Context
import io.github.sculk_cli.pack.ModLoader
import java.nio.file.Paths

class ExportCurseforge :
    CliktCommand(name = "curseforge", help = "Export a Curseforge modpack (.zip)") {
    override fun run() = runBlocking {
        val ctx = Context.Companion.getOrCreate(terminal)
        val curseforgeManifest = createCurseforgeManifest(ctx)
        val overrides = ctx.pack
            .getFiles()
            .map { it.path to ctx.pack.getBasePath().resolve(it.path).toFile().readBytes() }

        createCurseforgePack(
            path = Paths.get("")
                .resolve("${ctx.pack.getManifest().name}-${ctx.pack.getManifest().version}.zip"),
            manifest = curseforgeManifest,
            overrides = overrides.toMap(),
        )

        terminal.info("Exported ${ctx.pack.getManifest().name} to ${ctx.pack.getManifest().name}-${ctx.pack.getManifest().version}.zip")
    }

    private fun createCurseforgeManifest(ctx: Context): CurseforgePackManifest {
        val files = mutableListOf<CurseforgePackFile>()

        for ((path, fileManifest) in ctx.pack.getManifests().entries) {
            if (fileManifest.sources.curseforge != null) {
                files += CurseforgePackFile(
                    projectId = fileManifest.sources.curseforge!!.projectId,
                    fileId = fileManifest.sources.curseforge!!.fileId,
                    required = true
                )
            } else {
                terminal.warning("File $path will not be included as it does not have a Curseforge source")
            }
        }

        val modLoaderId = when (ctx.pack.getManifest().loader.type) {
            ModLoader.Fabric -> "fabric"
            ModLoader.Forge -> "forge"
            ModLoader.Neoforge -> "neoforge"
            ModLoader.Quilt -> "quilt"
        }

        return CurseforgePackManifest(
            minecraft = CurseforgePackMinecraft(
                version = ctx.pack.getManifest().minecraft,
                modLoaders = listOf(
                    CurseforgePackModLoader(
                        id = "$modLoaderId-${ctx.pack.getManifest().loader.version}",
                        primary = true
                    )
                )
            ),
            name = ctx.pack.getManifest().name,
            version = ctx.pack.getManifest().version,
            author = ctx.pack.getManifest().author ?: "Unknown",
            manifestType = CURSEFORGE_PACK_MINECRAFT_TYPE,
            manifestVersion = CURSEFORGE_PACK_MANIFEST_VERSION,
            files = files,
            overrides = "overrides",
        )
    }
}
