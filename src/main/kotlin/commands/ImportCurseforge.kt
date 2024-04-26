package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.animation.progress.advance
import com.github.ajalt.mordant.widgets.progress.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tech.jamalam.*
import tech.jamalam.curseforge.importCurseforgePack
import tech.jamalam.curseforge.models.getSide
import tech.jamalam.pack.*
import tech.jamalam.util.*
import java.io.File
import java.nio.file.Paths

class ImportCurseforge : CliktCommand(name = "curseforge") {
    private val curseforgePackPath by argument().file(mustExist = true, mustBeReadable = true)

    override fun run() = runBlocking {
        coroutineScope {
            val importedPack = importCurseforgePack(curseforgePackPath.toPath())
            terminal.info("Loaded Curseforge pack with name ${importedPack.manifest.name}; creating Sculk modpack")
            val manifests = mutableListOf<SerialPackManifestManifest>()
            val files = mutableListOf<SerialPackManifestFile>()

            val progress = progressBarContextLayout {
                text(terminal.theme.info("Adding files to manifest"))
                marquee(width = 60) { terminal.theme.warning(context) }
                percentage()
                progressBar()
                completed(style = terminal.theme.success)
            }.animateInCoroutine(
                terminal,
                total = importedPack.manifest.files.size.toLong(),
                context = ""
            )

            launch { progress.execute() }

            for (file in importedPack.manifest.files) {
                progress.advance(1)
                progress.update { context = "project: ${file.projectId}, file: ${file.fileId}" }

                val mod = ctx.curseforgeApi.getMod(file.projectId)

                if (mod == null) {
                    terminal.warning("Skipping file ${file.projectId}:${file.fileId} as the project does not exist")
                    continue
                }

                val cfFile = ctx.curseforgeApi.getModFile(file.projectId, file.fileId)

                if (cfFile == null) {
                    terminal.warning("Skipping file ${file.projectId}:${file.fileId} as the file does not exist")
                    continue
                }

                if (cfFile.downloadUrl == null) {
                    terminal.warning("Skipping file ${file.projectId}:${file.fileId} as the file does not have a download URL")
                    continue
                }

                val tempFile = downloadFileTemp(parseUrl(cfFile.downloadUrl!!)).readBytes()

                val fileManifest = FileManifest(
                    filename = cfFile.fileName,
                    side = cfFile.getSide().toSide(),
                    hashes = FileManifestHashes(
                        sha1 = tempFile.digestSha1(),
                        sha512 = tempFile.digestSha512()
                    ),
                    fileSize = tempFile.size,
                    sources = FileManifestSources(
                        curseforge = FileManifestCurseforgeSource(
                            projectId = mod.id,
                            fileId = file.fileId,
                            fileUrl = cfFile.downloadUrl!!
                        ),
                        modrinth = null,
                        url = null
                    )
                )

                // TODO: will need unhardcoding when we add support for stuff other than mods
                val manifestFile =
                    File("mods").resolve("${mod.slug}.sculk.json")
                manifestFile.mkdirsAndWriteJson(ctx.json, fileManifest.toSerial())

                manifests += SerialPackManifestManifest(
                    path = manifestFile.toString(),
                    sha256 = manifestFile.readBytes().digestSha256()
                )
            }

            for ((path, bytes) in importedPack.overrides) {
                val overrideFile = Paths.get("").resolve(path).toFile()
                overrideFile.canonicalFile.parentFile.mkdirs()
                overrideFile.writeBytes(bytes)
                files += SerialPackManifestFile(
                    path = path,
                    side = Side.Both,
                    sha256 = bytes.digestSha256()
                )
            }

            val modLoaderAndVersion =
                importedPack.manifest.minecraft.modLoaders.first { it.primary }.id.split("-")
            val modLoader = SerialPackManifestModLoader(
                type = when (modLoaderAndVersion[0]) {
                    "forge" -> ModLoader.Forge
                    "fabric" -> ModLoader.Fabric
                    "neoforge" -> ModLoader.Neoforge
                    "quilt" -> ModLoader.Quilt
                    else -> error("Unknown mod loader ${modLoaderAndVersion[0]}")
                },
                version = modLoaderAndVersion[1]
            )

            val pack = SerialPackManifest(
                name = importedPack.manifest.name,
                author = importedPack.manifest.author,
                summary = null,
                version = importedPack.manifest.version,
                minecraft = importedPack.manifest.minecraft.version,
                loader = modLoader,
                manifests = manifests,
                files = files,
            )

            Paths.get("").resolve("manifest.sculk.json").toFile().mkdirsAndWriteJson(ctx.json, pack)
            terminal.info("Created Sculk modpack manifest")
        }
    }
}

