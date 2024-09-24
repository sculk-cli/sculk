package io.github.sculk_cli.pack

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import io.github.sculk_cli.Context
import io.github.sculk_cli.pack.migration.FormatVersion
import io.github.sculk_cli.util.digestSha256
import io.github.sculk_cli.util.mkdirsAndWriteJson
import io.github.sculk_cli.util.tryWithContext
import java.nio.file.Path
import java.nio.file.Paths

class InMemoryPack(ctx: Context, private val basePath: Path = Paths.get("")) {
    private val packManifest: PackManifest
    private val manifests = mutableMapOf<String, FileManifest>()
    private val files = mutableListOf<PackManifestFile>()

    init {
        basePath.toFile().mkdirs()

        val manifestPath = basePath.resolve("manifest.sculk.json")
        if (!manifestPath.toFile().exists()) {
            error("Attempted to open a pack at $basePath, but no manifest was found")
        }

        val manifest =
            tryWithContext("while reading root manifest") { manifestPath.toFile().readText() }
        val rootManifestJson = tryWithContext("while deserializing root manifest") {
            ctx.json.parseToJsonElement(manifest).jsonObject
        }
        val formatVersion = rootManifestJson["formatVersion"]?.jsonPrimitive?.content?.let {
            FormatVersion.Companion.fromString(it)
        } ?: FormatVersion(0, 0)

        if (formatVersion != FormatVersion.Companion.CURRENT) {
            error("Format version $formatVersion is out of date, please run the migrate command")
        }

        val serialManifest =
            tryWithContext("while deserializing root manifest") {
                ctx.json.decodeFromJsonElement(
                    SerialPackManifest.serializer(),
                    rootManifestJson
                )
            }
        packManifest = serialManifest.load()

        for (file in packManifest.manifests) {
            val fileManifestPath = basePath.resolve(file.path)
            if (!fileManifestPath.toFile().exists()) {
                error("Attempted to open a file manifest at ${file.path}, but no manifest was found")
            }

            val fileManifest = tryWithContext("while reading ${file.path}") {
                fileManifestPath.toFile().readText()
            }

            if (fileManifest.toByteArray().digestSha256() != file.sha256) {
                error("File hashes do not match for manifest at ${file.path}")
            }

            val serialFileManifest =
                tryWithContext("while deserializing ${file.path}") {
                    ctx.json.decodeFromString<SerialFileManifest>(
                        fileManifest
                    )
                }
            manifests[file.path] = serialFileManifest.load()
        }

        for (file in packManifest.files) {
            files += file
        }
    }

    fun getBasePath(): Path {
        return basePath
    }

    fun getManifest(): PackManifest {
        return packManifest
    }

    fun getFiles(): List<PackManifestFile> {
        return files
    }

    fun removeFile(path: String) {
        files.remove(files.first { it.path == path })
        packManifest.files.remove(packManifest.files.first { it.path == path })
    }

    fun getManifests(): Map<String, FileManifest> {
        return manifests
    }

    fun getManifest(path: String): FileManifest? {
        return manifests[path]
    }

    fun setManifest(path: String, fileManifest: FileManifest) {
        manifests[path] = fileManifest
    }

    fun removeManifest(path: String) {
        manifests.remove(path)
        packManifest.manifests.remove(packManifest.manifests.first { it.path == path })
    }

    fun save(json: Json) {
        val manifestPath = basePath.resolve("manifest.sculk.json")

        for ((path, fileManifest) in manifests) {
            val fileManifestFile = basePath.resolve(path).toFile()
            fileManifestFile.mkdirsAndWriteJson(json, fileManifest.toSerial())

            if (packManifest.manifests.none { it.path == path }) {
                packManifest.manifests += PackManifestManifest(
                    path = path, sha256 = fileManifestFile.readBytes().digestSha256()
                )
            } else {
                packManifest.manifests.find { it.path == path }!!.sha256 =
                    fileManifestFile.readBytes().digestSha256()
            }
        }

        for (file in files) {
            if (packManifest.files.none { it.path == file.path }) {
                packManifest.files += PackManifestFile(
                    path = file.path,
                    side = file.side,
                    sha256 = basePath.resolve(file.path).toFile().readBytes().digestSha256()
                )
            } else {
                packManifest.files.find { it.path == file.path }!!.sha256 =
                    basePath.resolve(file.path).toFile().readBytes().digestSha256()
            }
        }

        manifestPath.toFile().mkdirsAndWriteJson(json, packManifest.toSerial())
    }
}

data class PackManifest(
    var formatVersion: String,
    var name: String,
    var summary: String?,
    var author: String?,
    var version: String,
    var minecraft: String,
    var loader: PackManifestModLoader,
    var manifests: MutableList<PackManifestManifest>,
    var files: MutableList<PackManifestFile>
)

data class PackManifestModLoader(
    var type: ModLoader,
    var version: String,
)

data class PackManifestManifest(
    var path: String,
    var sha256: String,
)

data class PackManifestFile(
    var path: String,
    var side: Side,
    var sha256: String
)

data class FileManifest(
    var filename: String,
    var side: Side,
    var hashes: FileManifestHashes,
    var fileSize: Int,
    var sources: FileManifestSources
)

data class FileManifestHashes(
    var sha1: String, var sha512: String, var murmur2: Long
)

data class FileManifestSources(
    var curseforge: FileManifestCurseforgeSource?,
    var modrinth: FileManifestModrinthSource?,
    var url: FileManifestUrlSource?,
)

data class FileManifestCurseforgeSource(
    var projectId: Int, var fileUrl: String, var fileId: Int,
)

data class FileManifestModrinthSource(
    var projectId: String, var fileUrl: String,
)

data class FileManifestUrlSource(
    var url: String
)

fun PackManifest.toSerial(): SerialPackManifest {
    return SerialPackManifest(
        formatVersion = formatVersion,
        name = name,
        summary = summary,
        author = author,
        version = version,
        minecraft = minecraft,
        loader = SerialPackManifestModLoader(
            type = loader.type,
            version = loader.version,
        ),
        manifests = manifests.map {
            SerialPackManifestManifest(
                path = it.path,
                sha256 = it.sha256,
            )
        },
        files = files.map {
            SerialPackManifestFile(
                path = it.path,
                side = it.side,
                sha256 = it.sha256,
            )
        }
    )
}

fun FileManifest.toSerial(): SerialFileManifest {
    return SerialFileManifest(
        filename = filename,
        side = side,
        hashes = SerialFileManifestHashes(
            sha1 = hashes.sha1,
            sha512 = hashes.sha512,
            murmur2 = hashes.murmur2,
        ),
        fileSize = fileSize,
        sources = SerialFileManifestSources(
            curseforge = sources.curseforge?.let {
                SerialFileManifestCurseforgeSource(
                    projectId = it.projectId,
                    fileUrl = it.fileUrl,
                    fileId = it.fileId,
                )
            },
            modrinth = sources.modrinth?.let {
                SerialFileManifestModrinthSource(
                    projectId = it.projectId,
                    fileUrl = it.fileUrl,
                )
            },
            url = sources.url?.let {
                SerialFileManifestUrlSource(
                    url = it.url,
                )
            },
        )
    )
}
