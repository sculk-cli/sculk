@file:Suppress("ClassName")

package io.github.sculk_cli.pack.migration

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import io.github.sculk_cli.curseforge.calculateCurseforgeMurmur2Hash
import io.github.sculk_cli.Context
import io.github.sculk_cli.util.digestSha256
import io.github.sculk_cli.util.downloadFileTemp
import io.github.sculk_cli.util.mkdirsAndWriteJson
import io.github.sculk_cli.util.parseUrl
import java.io.File

val migrators = listOf(
    Migrator1_0(),
    Migrator1_1(),
)

class FormatVersion(private val major: Int, private val minor: Int) {
    override fun toString(): String {
        return "$major.$minor"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FormatVersion) return false

        if (major != other.major) return false
        if (minor != other.minor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        return result
    }

    operator fun compareTo(outputVersion: FormatVersion): Int {
        return when {
            major > outputVersion.major -> 1
            major < outputVersion.major -> -1
            minor > outputVersion.minor -> 1
            minor < outputVersion.minor -> -1
            else -> 0
        }
    }

    companion object {
        val CURRENT = FormatVersion(1, 1)

        fun fromString(version: String): FormatVersion {
            val parts = version.split('.')
            return FormatVersion(parts[0].toInt(), parts[1].toInt())
        }
    }
}

abstract class Migrator {
    private val hashes: MutableMap<String, String> = mutableMapOf()

    open fun migrateRootManifest(json: JsonObject): JsonObject {
        return JsonObject(json.toMutableMap().apply {
            put("formatVersion", JsonPrimitive(getOutputVersion().toString()))
        })
    }

    fun migrateFileManifest(path: String, json: JsonObject): JsonObject {
        val migrated = migrateFileManifest2(path, json)
        val tempFile = File.createTempFile("migrated", ".json")
        tempFile.mkdirsAndWriteJson(Context.Companion.getOrCreate().json, migrated)
        hashes[path] = tempFile.readBytes().digestSha256()
        return migrated
    }

    abstract fun migrateFileManifest2(path: String, json: JsonObject): JsonObject

    open fun manipulateRootPostMigration(json: JsonObject): JsonObject {
        Context.Companion.getOrCreate().terminal.info(hashes)
        return JsonObject(json.toMutableMap().apply {
            val fileManifests =
                json["manifests"]?.jsonArray!!.map { it.jsonObject }.map { it.toMutableMap() }

            for (fileManifest in fileManifests) {
                val path = fileManifest["path"]!!.jsonPrimitive.content
                fileManifest["sha256"] = JsonPrimitive(hashes[path])
            }

            set("manifests", JsonArray(fileManifests.map { JsonObject(it) }))
        })
    }

    abstract fun getOutputVersion(): FormatVersion
}

// Adds `formatVersion` to root manifest
class Migrator1_0 : Migrator() {
    override fun migrateFileManifest2(path: String, json: JsonObject): JsonObject {
        return json
    }

    override fun getOutputVersion(): FormatVersion {
        return FormatVersion(1, 0)
    }
}

// Adds a `murmur2` hash to file manifests
class Migrator1_1 : Migrator() {
    override fun migrateFileManifest2(path: String, json: JsonObject): JsonObject {
        return JsonObject(json.toMutableMap().apply {
            val sources = json["sources"]?.jsonObject!!.toMap()

            val downloadUrl = if (sources.containsKey("url")) {
                sources["url"]!!.jsonObject["url"]!!.jsonPrimitive.content
            } else if (sources.containsKey("modrinth")) {
                sources["modrinth"]!!.jsonObject["fileUrl"]!!.jsonPrimitive.content
            } else if (sources.containsKey("curseforge")) {
                sources["curseforge"]!!.jsonObject["fileUrl"]!!.jsonPrimitive.content
            } else {
                return json
            }

            val hashes = json["hashes"]?.jsonObject!!.toMutableMap().apply {
                val file = runBlocking { downloadFileTemp(parseUrl(downloadUrl)) }
                put("murmur2", JsonPrimitive(calculateCurseforgeMurmur2Hash(file.readBytes())))
            }

            set("hashes", JsonObject(hashes))
        })
    }

    override fun getOutputVersion(): FormatVersion {
        return FormatVersion(1, 1)
    }
}
