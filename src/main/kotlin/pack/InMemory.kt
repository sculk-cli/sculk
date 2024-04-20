package tech.jamalam.pack

import kotlinx.serialization.json.Json
import tech.jamalam.digestSha256
import java.nio.file.Path
import java.nio.file.Paths

class InMemoryPack(json: Json, private val basePath: Path = Paths.get("")) {
    private val packManifest: PackManifest
    private val fileManifests = mutableMapOf<String, FileManifest>()
    private val directFiles = mutableListOf<String>()

    init {
        basePath.toFile().mkdirs()

        val manifestPath = basePath.resolve("manifest.sculk.json")
        if (!manifestPath.toFile().exists()) {
            error("Attempted to open a pack at $basePath, but no manifest was found")
        }

        val manifest = manifestPath.toFile().readText()
        val serialManifest = json.decodeFromString(SerialPackManifest.serializer(), manifest)
        packManifest = serialManifest.load()

        for (file in packManifest.files) {
            if (file.path.endsWith(".sculk.json")) {
                val fileManifestPath = basePath.resolve(file.path)
                if (!fileManifestPath.toFile().exists()) {
                    error("Attempted to open a file manifest at ${file.path}, but no manifest was found")
                }

                val fileManifest = fileManifestPath.toFile().readText()

                if (fileManifest.toByteArray().digestSha256() != file.sha256) {
                    error("File hashes do not match for manifest at ${file.path}")
                }

                val serialFileManifest =
                    json.decodeFromString(SerialFileManifest.serializer(), fileManifest)
                fileManifests[file.path] = serialFileManifest.load()
            } else {
                directFiles.add(file.path)
            }
        }
    }

    fun getBasePath(): Path {
        return basePath
    }

    fun getManifest(): PackManifest {
        return packManifest
    }

    fun getDirectFiles(): List<String> {
        return directFiles
    }

    fun getFileManifests(): Map<String, FileManifest> {
        return fileManifests
    }

    fun getFileManifest(path: String): FileManifest {
        return fileManifests[path] ?: error("No file manifest found at $path")
    }

    fun addFileManifest(path: String, fileManifest: FileManifest) {
        fileManifests[path] = fileManifest
    }

    fun addDirectFile(path: String) {
        directFiles.add(path)
    }

    fun save(json: Json) {
        val manifestPath = basePath.resolve("manifest.sculk.json")

        for ((path, fileManifest) in fileManifests) {
            val fileManifestFile = basePath.resolve(path).toFile()
            fileManifestFile.parentFile.mkdirs()
            fileManifestFile.writeText(
                "${json.encodeToString(
                    SerialFileManifest.serializer(),
                    fileManifest.toSerial()
                )}\n"
            )

            if (packManifest.files.none { it.path == path }) {
                packManifest.files += PackManifestFile(
                    path = path, sha256 = fileManifestFile.readBytes().digestSha256()
                )
            } else {
                packManifest.files.find { it.path == path }!!.sha256 =
                    fileManifestFile.readBytes().digestSha256()
            }
        }

        for (path in directFiles) {
            if (packManifest.files.none { it.path == path }) {
                packManifest.files += PackManifestFile(
                    path = path, sha256 = basePath.resolve(path).toFile().readBytes().digestSha256()
                )
            } else {
                packManifest.files.find { it.path == path }!!.sha256 =
                    basePath.resolve(path).toFile().readBytes().digestSha256()
            }
        }

        manifestPath.toFile().writeText(
            "${json.encodeToString(
                SerialPackManifest.serializer(),
                packManifest.toSerial()
            )}\n"
        )
    }
}

data class PackManifest(
    var name: String,
    var summary: String?,
    var version: String,
    var minecraft: String,
    var loader: PackManifestModLoader,
    var files: List<PackManifestFile>
)

data class PackManifestModLoader(
    var type: ModLoader,
    var version: String,
)

data class PackManifestFile(
    var path: String,
    var sha256: String,
)

data class FileManifest(
    var filename: String,
    var hashes: FileManifestHashes,
    var fileSize: Int,
    var sources: FileManifestSources
)

data class FileManifestHashes(
    var sha1: String, var sha512: String
)

data class FileManifestSources(
    var curseforge: FileManifestCurseforgeSource?,
    var modrinth: FileManifestModrinthSource?,
    var url: FileManifestUrlSource?,
)

data class FileManifestCurseforgeSource(
    var todo: String,
)

data class FileManifestModrinthSource(
    var projectId: String, var fileUrl: String,
)

data class FileManifestUrlSource(
    var url: String
)

fun PackManifest.toSerial(): SerialPackManifest {
    return SerialPackManifest(
        name = name,
        summary = summary,
        version = version,
        minecraft = minecraft,
        loader = SerialPackManifestModLoader(
            type = loader.type,
            version = loader.version,
        ),
        files = files.map {
            SerialPackManifestFile(
                path = it.path,
                sha256 = it.sha256,
            )
        }
    )
}

fun FileManifest.toSerial(): SerialFileManifest {
    return SerialFileManifest(
        filename = filename,
        hashes = SerialFileManifestHashes(
            sha1 = hashes.sha1,
            sha512 = hashes.sha512,
        ),
        fileSize = fileSize,
        sources = SerialFileManifestSources(
            curseforge = sources.curseforge?.let {
                SerialFileManifestCurseforgeSource(
                    todo = it.todo,
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
