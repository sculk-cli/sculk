package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import kotlinx.coroutines.runBlocking
import tech.jamalam.ctx
import tech.jamalam.modrinth.*
import tech.jamalam.modrinth.models.ModrinthVersionFileHashes
import tech.jamalam.pack.InMemoryPack
import tech.jamalam.pack.ModLoader
import tech.jamalam.pack.PackManifest
import tech.jamalam.util.parseUrl
import tech.jamalam.util.toModrinthEnvClientSupport
import tech.jamalam.util.toModrinthEnvServerSupport
import java.io.File
import java.nio.file.Paths

class ExportModrinth : CliktCommand(name = "modrinth") {
    override fun run() = runBlocking {
        val pack = InMemoryPack(ctx.json)
        val mrpackIndex = createMrpackIndex(pack)
        val overrides = pack
            .getFiles()
            .map { it.path to pack.getBasePath().resolve(it.path).toFile().readBytes() }

        createModrinthPack(
            path = Paths.get("")
                .resolve("${pack.getManifest().name}-${pack.getManifest().version}.mrpack"),
            index = mrpackIndex,
            overrides = overrides.toMap(),
            clientOverrides = emptyMap(),
            serverOverrides = emptyMap()
        )

        terminal.info("Exported ${pack.getManifest().name} to ${pack.getManifest().name}-${pack.getManifest().version}.mrpack")
    }

    private fun createMrpackIndex(pack: InMemoryPack): ModrinthPackIndex {
        val files = mutableListOf<ModrinthPackFile>()

        for ((path, fileManifest) in pack.getManifests().entries) {
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
            minecraftVersion = pack.getManifest().minecraft,
            forgeVersion = pack.getManifest().getLoaderVersionOrNull(ModLoader.Forge),
            neoforgeVersion = pack.getManifest().getLoaderVersionOrNull(ModLoader.Neoforge),
            fabricLoaderVersion = pack.getManifest().getLoaderVersionOrNull(ModLoader.Fabric),
            quiltLoaderVersion = pack.getManifest().getLoaderVersionOrNull(ModLoader.Quilt)
        )

        return ModrinthPackIndex(
            formatVersion = MRPACK_FORMAT_VERSION,
            game = MRPACK_MINECRAFT_GAME,
            versionId = pack.getManifest().version,
            name = pack.getManifest().name,
            summary = pack.getManifest().summary,
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
