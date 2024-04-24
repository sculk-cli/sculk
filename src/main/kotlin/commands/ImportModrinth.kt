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
import tech.jamalam.ctx
import tech.jamalam.digestSha256
import tech.jamalam.mkdirsAndWriteJson
import tech.jamalam.modrinth.getLoaderVersionPair
import tech.jamalam.modrinth.importModrinthPack
import tech.jamalam.pack.*
import tech.jamalam.parseUrl
import tech.jamalam.util.toModLoader
import tech.jamalam.util.toSide
import java.io.File
import java.nio.file.Paths

class ImportModrinth : CliktCommand(name = "modrinth") {
    private val mrpack by argument().file(mustExist = true, mustBeReadable = true)

    override fun run() = runBlocking {
        coroutineScope {
            val importedPack = importModrinthPack(mrpack.toPath())
            terminal.info("Loaded Modrinth pack with name ${importedPack.index.name}; creating Sculk modpack")
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
                total = importedPack.index.files.sumOf { it.downloadUrls.size }.toLong(),
                context = ""
            )

            launch { progress.execute() }

            for (file in importedPack.index.files) {
                if (file.downloadUrls.isEmpty()) {
                    terminal.warning("File ${file.path} will not be included as there are download URLs for it")
                    continue
                }

                val fileManifest = FileManifest(
                    filename = File(file.path).name,
                    side = file.env?.toSide() ?: Side.Both,
                    hashes = FileManifestHashes(
                        sha1 = file.hashes.sha1,
                        sha512 = file.hashes.sha512
                    ),
                    fileSize = file.fileSizeInBytes,
                    sources = FileManifestSources(
                        curseforge = null,
                        modrinth = null,
                        url = null
                    )
                )

                var slug: String? = null

                for (downloadUrl in file.downloadUrls) {
                    progress.advance(1)
                    progress.update { context = downloadUrl }

                    if (parseUrl(downloadUrl).host == "cdn.modrinth.com") {
                        val version = ctx.modrinthApi.getVersionFromHash(file.hashes.sha1)
                            ?: run {
                                terminal.warning("File ${file.path} includes a Modrinth file that does not seem to exist")
                                null
                            } ?: continue

                        fileManifest.sources.modrinth = FileManifestModrinthSource(
                            projectId = version.projectId,
                            fileUrl = downloadUrl
                        )

                        slug = ctx.modrinthApi.getProject(version.projectId)!!.slug
                    } else if (fileManifest.sources.url == null) {
                        fileManifest.sources.url = FileManifestUrlSource(downloadUrl)
                    } else {
                        terminal.warning("File ${file.path} has multiple valid URL sources; only the first will be included")
                    }
                }

                val actualSlug =
                    slug ?: File(file.path).nameWithoutExtension.split(Regex.fromLiteral("-\\d"))
                        .firstOrNull() ?: File(file.path).nameWithoutExtension

                val manifestFile =
                    File(file.path).resolveSibling("$actualSlug.sculk.json")
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

            for ((path, bytes) in importedPack.clientOverrides) {
                if (files.any { it.path == path }) {
                    files.removeIf { it.path == path }
                }

                val overrideFile = Paths.get("").resolve(path).toFile()
                overrideFile.canonicalFile.parentFile.mkdirs()
                overrideFile.writeBytes(bytes)
                files += SerialPackManifestFile(
                    path = path,
                    side = Side.ClientOnly,
                    sha256 = bytes.digestSha256()
                )
            }

            for ((path, bytes) in importedPack.serverOverrides) {
                if (files.any { it.path == path }) {
                    files.removeIf { it.path == path }
                }

                val overrideFile = Paths.get("").resolve(path).toFile()
                overrideFile.canonicalFile.parentFile.mkdirs()
                overrideFile.writeBytes(bytes)
                files += SerialPackManifestFile(
                    path = path,
                    side = Side.ServerOnly,
                    sha256 = bytes.digestSha256()
                )
            }

            val modLoaderAndVersion = importedPack.index.dependencies.getLoaderVersionPair()
            val modLoader = SerialPackManifestModLoader(
                type = modLoaderAndVersion.first.toModLoader(),
                version = modLoaderAndVersion.second
            )

            val pack = SerialPackManifest(
                name = importedPack.index.name,
                author = null,
                summary = importedPack.index.summary,
                version = importedPack.index.versionId,
                minecraft = importedPack.index.dependencies.minecraftVersion,
                loader = modLoader,
                manifests = manifests,
                files = files,
            )

            Paths.get("").resolve("manifest.sculk.json").toFile().mkdirsAndWriteJson(ctx.json, pack)
            terminal.info("Created Sculk modpack manifest")
        }
    }
}
