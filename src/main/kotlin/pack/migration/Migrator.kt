@file:Suppress("ClassName")

package tech.jamalam.pack.migration

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import tech.jamalam.Context
import tech.jamalam.curseforge.calculateCurseforgeMurmur2Hash
import tech.jamalam.util.digestSha256
import tech.jamalam.util.downloadFileTemp
import tech.jamalam.util.parseUrl

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

interface Migrator {
    fun migrateRootManifest(json: JsonObject): JsonObject {
        return JsonObject(json.toMutableMap().apply {
            put("formatVersion", JsonPrimitive(getOutputVersion().toString()))
        })
    }

    fun migrateFileManifest(path: String, json: JsonObject): JsonObject {
        return json
    }

    fun manipulateRootPostMigration(json: JsonObject): JsonObject {
        return json
    }

    fun getOutputVersion(): FormatVersion
}

// Adds `formatVersion` to root manifest
class Migrator1_0 : Migrator {
    override fun getOutputVersion(): FormatVersion {
        return FormatVersion(1, 0)
    }
}

// Adds a `murmur2` hash to file manifests
class Migrator1_1 : Migrator {
    private val hashes: MutableMap<String, String> = mutableMapOf()

    override fun migrateFileManifest(path: String, json: JsonObject): JsonObject {
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

            this@Migrator1_1.hashes[path] =
                Context.getOrCreate().json.encodeToString(json).toByteArray().digestSha256()
        })
    }

    override fun manipulateRootPostMigration(json: JsonObject): JsonObject {
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

    override fun getOutputVersion(): FormatVersion {
        return FormatVersion(1, 1)
    }
}
