package tech.jamalam.pack

import kotlinx.serialization.Serializable

@Serializable
data class SerialPackManifest(
    val name: String,
    val summary: String?,
    val author: String?,
    val version: String,
    val minecraft: String,
    val loader: SerialPackManifestModLoader,
    val manifests: List<SerialPackManifestManifest>,
    val files: List<SerialPackManifestFile>
)

@Serializable
data class SerialPackManifestModLoader(
    val type: ModLoader,
    val version: String,
)

@Serializable
data class SerialPackManifestManifest(
    val path: String,
    val sha256: String,
)

@Serializable
data class SerialPackManifestFile(
    val path: String,
    val side: Side,
    val sha256: String
)

@Serializable
data class SerialFileManifest(
    val filename: String,
    val side: Side,
    val hashes: SerialFileManifestHashes,
    val fileSize: Int,
    val sources: SerialFileManifestSources
)

@Serializable
data class SerialFileManifestHashes(
    val sha1: String, val sha512: String
)

@Serializable
data class SerialFileManifestSources(
    val curseforge: SerialFileManifestCurseforgeSource?,
    val modrinth: SerialFileManifestModrinthSource?,
    val url: SerialFileManifestUrlSource?,
)

@Serializable
data class SerialFileManifestCurseforgeSource(
    val projectId: Int, val fileUrl: String, val fileId: Int,
)

@Serializable
data class SerialFileManifestModrinthSource(
    val projectId: String, val fileUrl: String,
)

@Serializable
data class SerialFileManifestUrlSource(
    var url: String
)

fun SerialPackManifest.load(): PackManifest {
    return PackManifest(
        name = name,
        summary = summary,
        author = author,
        version = version,
        minecraft = minecraft,
        loader = PackManifestModLoader(
            type = loader.type,
            version = loader.version,
        ),
        manifests = manifests.map {
            PackManifestManifest(
                path = it.path,
                sha256 = it.sha256,
            )
        },
        files = files.map {
            PackManifestFile(
                path = it.path,
                side = it.side,
                sha256 = it.sha256,
            )
        }
    )
}

fun SerialFileManifest.load(): FileManifest {
    return FileManifest(
        filename = filename,
        side = side,
        hashes = FileManifestHashes(
            sha1 = hashes.sha1,
            sha512 = hashes.sha512,
        ),
        fileSize = fileSize,
        sources = FileManifestSources(
            curseforge = sources.curseforge?.let {
                FileManifestCurseforgeSource(
                    projectId = it.projectId,
                    fileUrl = it.fileUrl,
                    fileId = it.fileId,
                )
            },
            modrinth = sources.modrinth?.let {
                FileManifestModrinthSource(
                    projectId = it.projectId,
                    fileUrl = it.fileUrl,
                )
            },
            url = sources.url?.let {
                FileManifestUrlSource(
                    url = it.url,
                )
            },
        )
    )
}
