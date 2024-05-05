@file:Suppress("ClassName")

package tech.jamalam.pack.migration

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import tech.jamalam.curseforge.calculateCurseforgeMurmur2Hash
import tech.jamalam.util.downloadFileTemp
import tech.jamalam.util.parseUrl

private val migrators = listOf(
    Migrator1_0(),
    Migrator1_1(),
)

fun migrateFile(fileType: MigrationFileType, json: JsonObject, currentVersion: FormatVersion): Pair<Boolean, JsonObject> {
    var doneAny = false
    var migratedJson = json
    var currentVersion = currentVersion

    for (migrator in migrators) {
        if (currentVersion == migrator.getOutputVersion()) {
            continue
        }

        migratedJson = migrator.migrate(fileType, migratedJson)
        doneAny = true
        currentVersion = migrator.getOutputVersion()
    }

    return doneAny to migratedJson
}

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

    companion object {
        val CURRENT = FormatVersion(1, 1)

        fun fromString(version: String): FormatVersion {
            val parts = version.split('.')
            return FormatVersion(parts[0].toInt(), parts[1].toInt())
        }
    }
}

enum class MigrationFileType {
    ROOT_MANIFEST,
    FILE_MANIFEST;
}

interface Migrator {
    fun migrate(fileType: MigrationFileType, json: JsonObject): JsonObject
    fun getOutputVersion(): FormatVersion
}

class Migrator1_0 : Migrator {
    override fun migrate(fileType: MigrationFileType, json: JsonObject): JsonObject {
        return JsonObject(json.toMutableMap().apply {
            if (fileType == MigrationFileType.ROOT_MANIFEST) {
                put("formatVersion", JsonPrimitive(getOutputVersion().toString()))
            }
        })
    }

    override fun getOutputVersion(): FormatVersion {
        return FormatVersion(1, 0)
    }
}

class Migrator1_1 : Migrator {
    override fun migrate(fileType: MigrationFileType, json: JsonObject): JsonObject {
        return JsonObject(json.toMutableMap().apply {
            if (fileType == MigrationFileType.ROOT_MANIFEST) {
                put("formatVersion", JsonPrimitive(getOutputVersion().toString()))
            }

            // TODO: we need to be able to update the hashes in the root manifest after doing this?
            if (fileType == MigrationFileType.FILE_MANIFEST) {
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
            }
        })
    }

    override fun getOutputVersion(): FormatVersion {
        return FormatVersion(1, 1)
    }
}
