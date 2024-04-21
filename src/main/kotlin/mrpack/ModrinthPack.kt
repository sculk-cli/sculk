package tech.jamalam.mrpack

import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.animation.progress.advance
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.progress.*
import io.ktor.http.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.jamalam.*
import tech.jamalam.modrinth.models.ModrinthEnvSupport
import tech.jamalam.modrinth.models.ModrinthVersionFileHashes
import tech.jamalam.pack.*
import tech.jamalam.util.modrinthEnvTypePairToSide
import tech.jamalam.util.sideToModrinthEnvTypePair
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

private val ALLOWED_EXTERNAL_URLS = listOf(
    URLBuilder("cdn.modrinth.com").build(),
    URLBuilder("github.com").build(),
    URLBuilder("raw.githubusercontent.com").build(),
    URLBuilder("gitlab.com").build()
)

suspend fun exportModrinthPack(terminal: Terminal, pack: InMemoryPack) = coroutineScope {
    val files = mutableListOf<ModrinthPackFile>()
    val manifest = pack.getManifest()

    for ((path, fileManifest) in pack.getFileManifests().entries) {
        val downloads = mutableListOf<String>()

        if (fileManifest.sources.modrinth != null) {
            downloads += fileManifest.sources.modrinth!!.fileUrl
        }

        if (fileManifest.sources.url != null) {
            downloads += fileManifest.sources.url!!.url
        }

        val filteredDownloads = downloads.map { URLBuilder(it).build() }
            //TODO
//            .filter { url -> ALLOWED_EXTERNAL_URLS.firstOrNull { allowedUrl -> allowedUrl.host == url.host } != null }
            .map { it.toString() }

        if (filteredDownloads.isEmpty()) {
            terminal.warning("File $path will not be included as there are no valid sources for it")
            continue
        }

        files += ModrinthPackFile(
            path = File(path).resolveSibling(fileManifest.filename).toString(),
            hashes = ModrinthVersionFileHashes(
                sha1 = fileManifest.hashes.sha1, sha512 = fileManifest.hashes.sha512
            ),
            env = ModrinthPackFileEnv(
                client = sideToModrinthEnvTypePair(fileManifest.side).first,
                server = sideToModrinthEnvTypePair(fileManifest.side).second
            ),
            downloads = filteredDownloads,
            fileSize = fileManifest.fileSize
        )
    }

    val dependencies = ModrinthPackDependencies(
        minecraft = manifest.minecraft,
        forge = if (manifest.loader.type == ModLoader.Forge) manifest.loader.version else null,
        neoforge = if (manifest.loader.type == ModLoader.Neoforge) manifest.loader.version else null,
        fabricLoader = if (manifest.loader.type == ModLoader.Fabric) manifest.loader.version else null,
        quiltLoader = if (manifest.loader.type == ModLoader.Quilt) manifest.loader.version else null
    )

    val index = ModrinthPackIndex(
        formatVersion = 1,
        game = "minecraft",
        versionId = manifest.version,
        name = manifest.name,
        summary = manifest.summary,
        files = files,
        dependencies = dependencies
    )

    val os = ZipOutputStream(
        FileOutputStream(
            pack.getBasePath().resolve("${manifest.name}-${manifest.version}.mrpack").toFile()
        )
    )
    var entry = ZipEntry("modrinth.index.json")
    os.putNextEntry(entry)
    var data =
        ctx.json.encodeToString(ModrinthPackIndex.serializer(), index).toByteArray(Charsets.UTF_8)
    os.write(data, 0, data.size)
    os.closeEntry()

    for (file in pack.getDirectFiles()) {
        entry = ZipEntry("overrides/${file}")
        os.putNextEntry(entry)
        data = pack.getBasePath().resolve(file).toFile().readBytes()
        os.write(data, 0, data.size)
        os.closeEntry()
    }

    os.close()

    terminal.info("Exported ${manifest.name}-${manifest.version}.mrpack")
}

suspend fun importModrinthPack(terminal: Terminal, importPath: Path, mrpackPath: Path) =
    coroutineScope {
        terminal.info("Importing $mrpackPath")
        val zip = ZipFile(mrpackPath.toFile())
        val entry = zip.getEntry("modrinth.index.json")
        val index =
            ctx.json.decodeFromString<ModrinthPackIndex>(
                String(
                    zip.getInputStream(entry).readAllBytes()
                )
            )
        val files = mutableListOf<SerialPackManifestFile>()

        val progress = progressBarContextLayout {
            text(terminal.theme.info("Adding files to manifest"))
            marquee(width = 60) { terminal.theme.warning(context) }
            percentage()
            progressBar()
            completed(style = terminal.theme.success)
        }.animateInCoroutine(terminal, total = index.files.size.toLong(), context = "")

        launch { progress.execute() }

        for (file in index.files) {
            val dl = file.downloads.first { it.contains("cdn.modrinth.com") }
            progress.advance(1)
            progress.update { context = dl.split("/").last() }
            val tempFile = downloadFileTemp(parseUrl(dl))
            val tempFileBytes = tempFile.readBytes()
            val version = ctx.modrinthApi.getVersionFromHash(file.hashes.sha1)
                ?: error("Can only currently import Modrinth versions from mrpacks")

            val fileManifest = SerialFileManifest(
                filename = File(file.path).name,
                side = modrinthEnvTypePairToSide(
                    file.env?.client ?: ModrinthEnvSupport.Required,
                    file.env?.server ?: ModrinthEnvSupport.Required
                ),
                hashes = SerialFileManifestHashes(
                    sha1 = file.hashes.sha1,
                    sha512 = file.hashes.sha512
                ),
                fileSize = tempFileBytes.size,
                sources = SerialFileManifestSources(
                    curseforge = null,
                    url = null,
                    modrinth = SerialFileManifestModrinthSource(
                        projectId = version.projectId,
                        fileUrl = dl
                    )
                )
            )

            val filePath = File(file.path)
            val manifestFile =
                filePath.resolveSibling("${filePath.nameWithoutExtension}.sculk.json")
            manifestFile.mkdirsAndWriteJson(ctx.json, fileManifest)

            files += SerialPackManifestFile(
                path = manifestFile.toString(),
                sha256 = manifestFile.readBytes().digestSha256()
            )
        }

        val modLoader = if (index.dependencies.fabricLoader != null) {
            SerialPackManifestModLoader(
                type = ModLoader.Fabric, version = index.dependencies.fabricLoader
            )
        } else if (index.dependencies.neoforge != null) {
            SerialPackManifestModLoader(
                type = ModLoader.Neoforge, version = index.dependencies.neoforge
            )
        } else if (index.dependencies.forge != null) {
            SerialPackManifestModLoader(type = ModLoader.Forge, version = index.dependencies.forge)
        } else if (index.dependencies.quiltLoader != null) {
            SerialPackManifestModLoader(
                type = ModLoader.Quilt, version = index.dependencies.quiltLoader
            )
        } else {
            error("No valid mod loader found")
        }

        val pack = SerialPackManifest(
            name = index.name,
            summary = index.summary,
            author = null,
            version = index.versionId,
            minecraft = index.dependencies.minecraft,
            loader = modLoader,
            files = files
        )

        importPath.toFile().canonicalFile.resolve("manifest.sculk.json")
            .mkdirsAndWriteJson(ctx.json, pack)
    }

@Serializable
data class ModrinthPackIndex(
    val formatVersion: Int,
    val game: String,
    val versionId: String,
    val name: String,
    val summary: String?,
    val files: List<ModrinthPackFile>,
    val dependencies: ModrinthPackDependencies,
)

@Serializable
data class ModrinthPackFile(
    val path: String,
    val hashes: ModrinthVersionFileHashes,
    val env: ModrinthPackFileEnv?,
    val downloads: List<String>,
    val fileSize: Int
)

@Serializable
data class ModrinthPackFileEnv(
    val client: ModrinthEnvSupport, val server: ModrinthEnvSupport
)

@Serializable
data class ModrinthPackDependencies(
    val minecraft: String,
    val forge: String?,
    val neoforge: String?,
    @SerialName("fabric-loader") val fabricLoader: String?,
    @SerialName("quilt-loader") val quiltLoader: String?
)

