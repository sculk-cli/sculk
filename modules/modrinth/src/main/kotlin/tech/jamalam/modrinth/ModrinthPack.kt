package tech.jamalam.modrinth

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tech.jamalam.modrinth.models.ModrinthEnvSupport
import tech.jamalam.modrinth.models.ModrinthModLoader
import tech.jamalam.modrinth.models.ModrinthVersionFileHashes
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

public val MRPACK_ALLOWED_EXTERNAL_URLS: List<String> = listOf(
    "cdn.modrinth.com",
    "github.com",
    "raw.githubusercontent.com",
    "gitlab.com"
)

public const val MRPACK_FORMAT_VERSION: Int = 1
public const val MRPACK_MINECRAFT_GAME: String = "minecraft"

private const val MODRINTH_INDEX_JSON_PATH: String = "modrinth.index.json"

@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
    explicitNulls = false
    coerceInputValues = true
}

public fun createModrinthPack(
    path: Path,
    index: ModrinthPackIndex,
    overrides: Map<String, ByteArray>,
    clientOverrides: Map<String, ByteArray>,
    serverOverrides: Map<String, ByteArray>,
) {
    val outputStream = ZipOutputStream(FileOutputStream(path.toFile()))
    val indexBytes = json.encodeToString(ModrinthPackIndex.serializer(), index)
    outputStream.writeEntry(MODRINTH_INDEX_JSON_PATH, indexBytes.encodeToByteArray())

    for ((overridePath, bytes) in overrides) {
        outputStream.writeEntry("overrides/$overridePath", bytes)
    }

    for ((overridePath, bytes) in clientOverrides) {
        outputStream.writeEntry("client-overrides/$overridePath", bytes)
    }

    for ((overridePath, bytes) in serverOverrides) {
        outputStream.writeEntry("server-overrides/$overridePath", bytes)
    }

    outputStream.close()
}

public fun importModrinthPack(path: Path): ImportedModrinthPack {
    val zipFile = ZipFile(path.toFile())
    val indexEntry = zipFile.getEntry(MODRINTH_INDEX_JSON_PATH)
    val index = json.decodeFromString(
        ModrinthPackIndex.serializer(),
        zipFile.getInputStream(indexEntry).readBytes().decodeToString()
    )

    val overrides = zipFile.entries().asSequence()
        .filter { it.name.startsWith("overrides/") }
        .map { it.name.removePrefix("overrides/") to zipFile.getInputStream(it).readBytes() }
        .toMap()

    val clientOverrides = zipFile.entries().asSequence()
        .filter { it.name.startsWith("client-overrides/") }
        .map { it.name.removePrefix("client-overrides/") to zipFile.getInputStream(it).readBytes() }
        .toMap()

    val serverOverrides = zipFile.entries().asSequence()
        .filter { it.name.startsWith("server-overrides/") }
        .map { it.name.removePrefix("server-overrides/") to zipFile.getInputStream(it).readBytes() }
        .toMap()

    return ImportedModrinthPack(index, overrides, clientOverrides, serverOverrides)
}

public data class ImportedModrinthPack(
    val index: ModrinthPackIndex,
    val overrides: Map<String, ByteArray>,
    val clientOverrides: Map<String, ByteArray>,
    val serverOverrides: Map<String, ByteArray>,
)

@Serializable
public data class ModrinthPackIndex(
    val formatVersion: Int,
    val game: String,
    val versionId: String,
    val name: String,
    val summary: String? = null,
    val files: List<ModrinthPackFile>,
    val dependencies: ModrinthPackDependencies,
)

@Serializable
public data class ModrinthPackFile(
    val path: String,
    val hashes: ModrinthVersionFileHashes,
    val env: ModrinthPackFileEnv? = null,
    @SerialName("downloads") val downloadUrls: List<String>,
    @SerialName("fileSize") val fileSizeInBytes: Int,
)

@Serializable
public data class ModrinthPackFileEnv(
    @SerialName("client") val clientSupport: ModrinthEnvSupport,
    @SerialName("server") val serverSupport: ModrinthEnvSupport,
)

@Serializable
public data class ModrinthPackDependencies(
    @SerialName("minecraft") val minecraftVersion: String,
    @SerialName("forge") val forgeVersion: String? = null,
    @SerialName("neoforge") val neoforgeVersion: String? = null,
    @SerialName("fabric-loader") val fabricLoaderVersion: String? = null,
    @SerialName("quilt-loader") val quiltLoaderVersion: String? = null,
)

public fun ModrinthPackDependencies.getLoaderVersionPair(): Pair<ModrinthModLoader, String> {
    return when {
        forgeVersion != null -> ModrinthModLoader.Forge to forgeVersion
        neoforgeVersion != null -> ModrinthModLoader.NeoForge to neoforgeVersion
        fabricLoaderVersion != null -> ModrinthModLoader.Fabric to fabricLoaderVersion
        quiltLoaderVersion != null -> ModrinthModLoader.Quilt to quiltLoaderVersion
        else -> error("No valid mod loader found")
    }
}

private fun ZipOutputStream.writeEntry(path: String, data: ByteArray) {
    val entry = ZipEntry(path)
    putNextEntry(entry)
    write(data)
    closeEntry()
}
