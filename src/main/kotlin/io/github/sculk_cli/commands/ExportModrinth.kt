package io.github.sculk_cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import kotlinx.coroutines.runBlocking
import io.github.sculk_cli.Context
import io.github.sculk_cli.pack.ModLoader
import io.github.sculk_cli.pack.PackManifest
import io.github.sculk_cli.util.parseUrl
import io.github.sculk_cli.util.toModrinthEnvClientSupport
import io.github.sculk_cli.util.toModrinthEnvServerSupport
import io.github.sculk_cli.modrinth.*
import io.github.sculk_cli.modrinth.models.ModrinthVersionFileHashes
import java.io.File
import java.nio.file.Paths

class ExportModrinth :
    CliktCommand(name = "modrinth", help = "Export a Modrinth modpack (.mrpack)") {
    override fun run() = runBlocking {
        val ctx = Context.Companion.getOrCreate(terminal)
        val mrpackIndex = createMrpackIndex(ctx)
        val overrides = ctx.pack
            .getFiles()
            .map { it.path to ctx.pack.getBasePath().resolve(it.path).toFile().readBytes() }

        createModrinthPack(
            path = Paths.get("")
                .resolve("${ctx.pack.getManifest().name}-${ctx.pack.getManifest().version}.mrpack"),
            index = mrpackIndex,
            overrides = overrides.toMap(),
            clientOverrides = emptyMap(),
            serverOverrides = emptyMap()
        )

        terminal.info("Exported ${ctx.pack.getManifest().name} to ${ctx.pack.getManifest().name}-${ctx.pack.getManifest().version}.mrpack")
    }

    private fun createMrpackIndex(ctx: Context): ModrinthPackIndex {
        val files = mutableListOf<ModrinthPackFile>()

        for ((path, fileManifest) in ctx.pack.getManifests().entries) {
            val downloadUrls = mutableListOf<String>()

            if (fileManifest.sources.modrinth != null) {
                downloadUrls += fileManifest.sources.modrinth!!.fileUrl
            }

            if (fileManifest.sources.url != null) {
                downloadUrls += fileManifest.sources.url!!.url
            }

            val filteredDownloadUrls = downloadUrls
                .map { parseUrl(it) }
                .filter { MRPACK_ALLOWED_EXTERNAL_URLS.contains(it.host) }
                .map { it.toString() }

            if (filteredDownloadUrls.isEmpty()) {
                terminal.warning("File $path will not be included as there are no valid sources for it")
                continue
            }

            files += ModrinthPackFile(
                path = File(path).resolveSibling(fileManifest.filename).toString(),
                hashes = ModrinthVersionFileHashes(
                    sha1 = fileManifest.hashes.sha1,
                    sha512 = fileManifest.hashes.sha512
                ),
                env = ModrinthPackFileEnv(
                    clientSupport = fileManifest.side.toModrinthEnvClientSupport(),
                    serverSupport = fileManifest.side.toModrinthEnvServerSupport()
                ),
                downloadUrls = filteredDownloadUrls,
                fileSizeInBytes = fileManifest.fileSize
            )
        }

        val dependencies = ModrinthPackDependencies(
            minecraftVersion = ctx.pack.getManifest().minecraft,
            forgeVersion = ctx.pack.getManifest().getLoaderVersionOrNull(ModLoader.Forge),
            neoforgeVersion = ctx.pack.getManifest().getLoaderVersionOrNull(ModLoader.Neoforge),
            fabricLoaderVersion = ctx.pack.getManifest().getLoaderVersionOrNull(ModLoader.Fabric),
            quiltLoaderVersion = ctx.pack.getManifest().getLoaderVersionOrNull(ModLoader.Quilt)
        )

        return ModrinthPackIndex(
            formatVersion = MRPACK_FORMAT_VERSION,
            game = MRPACK_MINECRAFT_GAME,
            versionId = ctx.pack.getManifest().version,
            name = ctx.pack.getManifest().name,
            summary = ctx.pack.getManifest().summary,
            files = files,
            dependencies = dependencies
        )
    }
}

private fun PackManifest.getLoaderVersionOrNull(loader: ModLoader): String? {
    return if (this.loader.type == loader) {
        this.loader.version
    } else {
        null
    }
}
