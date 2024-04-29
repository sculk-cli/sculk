package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import kotlinx.coroutines.runBlocking
import tech.jamalam.ctx
import tech.jamalam.curseforge.*
import tech.jamalam.pack.InMemoryPack
import tech.jamalam.pack.ModLoader
import java.nio.file.Paths

class ExportCurseforge : CliktCommand(name = "curseforge", help = "Export a Curseforge modpack (.zip)") {
    override fun run() = runBlocking {
        val pack = InMemoryPack(ctx.json)
        val curseforgeManifest = createCurseforgeManifest(pack)
        val overrides = pack
            .getFiles()
            .map { it.path to pack.getBasePath().resolve(it.path).toFile().readBytes() }

        createCurseforgePack(
            path = Paths.get("")
                .resolve("${pack.getManifest().name}-${pack.getManifest().version}.zip"),
            manifest = curseforgeManifest,
            overrides = overrides.toMap(),
        )

        terminal.info("Exported ${pack.getManifest().name} to ${pack.getManifest().name}-${pack.getManifest().version}.zip")
    }

    private fun createCurseforgeManifest(pack: InMemoryPack): CurseforgePackManifest {
        val files = mutableListOf<CurseforgePackFile>()

        for ((path, fileManifest) in pack.getManifests().entries) {
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

        val modLoaderId = when (pack.getManifest().loader.type) {
            ModLoader.Fabric -> "fabric"
            ModLoader.Forge -> "forge"
            ModLoader.Neoforge -> "neoforge"
            ModLoader.Quilt -> "quilt"
        }

        return CurseforgePackManifest(
            minecraft = CurseforgePackMinecraft(
                version = pack.getManifest().minecraft,
                modLoaders = listOf(
                    CurseforgePackModLoader(
                        id = "$modLoaderId-${pack.getManifest().loader.version}",
                        primary = true
                    )
                )
            ),
            name = pack.getManifest().name,
            version = pack.getManifest().version,
            author = pack.getManifest().author ?: "Unknown",
            manifestType = CURSEFORGE_PACK_MINECRAFT_TYPE,
            manifestVersion = CURSEFORGE_PACK_MANIFEST_VERSION,
            files = files,
            overrides = "overrides",
        )
    }
}
