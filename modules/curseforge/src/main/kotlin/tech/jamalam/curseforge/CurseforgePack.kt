package tech.jamalam.curseforge

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

public const val CURSEFORGE_PACK_MINECRAFT_TYPE: String = "minecraftModpack"
public const val CURSEFORGE_PACK_MANIFEST_VERSION: Int = 1

private const val CURSEFORGE_MANIFEST_PATH: String = "manifest.json"

@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
    explicitNulls = false
    coerceInputValues = true
}

public fun createCurseforgePack(
    path: Path,
    manifest: CurseforgePackManifest,
    overrides: Map<String, ByteArray>,
) {
    val outputStream = ZipOutputStream(FileOutputStream(path.toFile()))
    val indexBytes = json.encodeToString(manifest)
    outputStream.writeEntry(CURSEFORGE_MANIFEST_PATH, indexBytes.encodeToByteArray())

    for ((overridePath, bytes) in overrides) {
        outputStream.writeEntry("overrides/$overridePath", bytes)
    }

    outputStream.close()
}

public fun importCurseforgePack(path: Path): ImportedCurseforgePack {
    val zipFile = ZipFile(path.toFile())
    val indexEntry = zipFile.getEntry(CURSEFORGE_MANIFEST_PATH)
    val manifest = json.decodeFromString(
        CurseforgePackManifest.serializer(),
        zipFile.getInputStream(indexEntry).readBytes().decodeToString()
    )

    val overrides = zipFile.entries().asSequence()
        .filter { it.name.startsWith("overrides/") }
        .map { it.name.removePrefix("overrides/") to zipFile.getInputStream(it).readBytes() }
        .toMap()

    return ImportedCurseforgePack(manifest, overrides)
}

public data class ImportedCurseforgePack(
    val manifest: CurseforgePackManifest,
    val overrides: Map<String, ByteArray>,
)

@Serializable
public data class CurseforgePackManifest(
    public val minecraft: CurseforgePackMinecraft,
    public val manifestType: String,
    public val manifestVersion: Int,
    public val name: String,
    public val version: String,
    public val author: String,
    public val files: List<CurseforgePackFile>,
    public val overrides: String,
)

@Serializable
public data class CurseforgePackMinecraft(
    public val version: String,
    public val modLoaders: List<CurseforgePackModLoader>,
)

@Serializable
public data class CurseforgePackModLoader(
    public val id: String,
    public val primary: Boolean,
)

@Serializable
public data class CurseforgePackFile(
    public val projectId: Int,
    public val fileId: Int,
    public val required: Boolean,
)

private fun ZipOutputStream.writeEntry(path: String, data: ByteArray) {
    val entry = ZipEntry(path)
    putNextEntry(entry)
    write(data)
    closeEntry()
}
