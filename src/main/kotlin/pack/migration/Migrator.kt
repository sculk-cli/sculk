@file:Suppress("ClassName")

package tech.jamalam.pack.migration

import kotlinx.serialization.json.*

private val migrators = listOf(
    Migrator1_0(),
)

fun migrateFile(fileType: MigrationFileType, json: JsonObject): Pair<Boolean, JsonObject> {
    var doneAny = false
    var migratedJson = json
    var currentVersion = json["formatVersion"]?.jsonPrimitive?.content?.let { FormatVersion.fromString(it) } ?: FormatVersion(0, 0)

    for (migrator in migrators) {
        if (currentVersion == migrator.getOutputVersion()) {
            break
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

    companion object {
        val CURRENT = FormatVersion(1, 0)

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
