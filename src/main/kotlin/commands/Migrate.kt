package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import tech.jamalam.Context
import tech.jamalam.pack.migration.FormatVersion
import tech.jamalam.pack.migration.Migrator
import tech.jamalam.pack.migration.migrators
import java.nio.file.Paths

class Migrate : CliktCommand(name = "migrate", help = "Migrate a pack to the latest format version") {
    private val basePath = Paths.get("")

    override fun run() {
        val ctx = Context.getOrCreate(terminal)
        val manifestPath = basePath.resolve("manifest.sculk.json")
        if (!manifestPath.toFile().exists()) {
            error("Attempted to open a pack at $basePath, but no manifest was found")
        }

        var rootManifestJson =
            ctx.json.parseToJsonElement(manifestPath.toFile().readText()).jsonObject
        val formatVersion = rootManifestJson["formatVersion"]?.jsonPrimitive?.content?.let {
            FormatVersion.fromString(it)
        } ?: FormatVersion(0, 0)

        if (formatVersion == FormatVersion.CURRENT) {
            terminal.info("Format version $formatVersion is up to date")
            return
        }

        var fileManifests = rootManifestJson["manifests"]!!.jsonArray.associateBy(
            { it.jsonObject["path"]!!.jsonPrimitive.content },
            {
                val path = it.jsonObject["path"]!!.jsonPrimitive.content
                val text = basePath.resolve(path).toFile().readText()
                ctx.json.parseToJsonElement(text).jsonObject
            }
        )

        for (migrator in migrators) {
            val (newRootManifest, newFileManifests) = runMigrator(
                migrator,
                rootManifestJson,
                fileManifests
            )

            rootManifestJson = newRootManifest
            fileManifests = newFileManifests
            terminal.info("Migrated pack to format version ${migrator.getOutputVersion()}")
        }

        manifestPath.toFile().writeText(ctx.json.encodeToString(rootManifestJson))

        for ((path, fileManifest) in fileManifests) {
            val file = basePath.resolve(path).toFile()
            file.writeText(ctx.json.encodeToString(fileManifest))
        }
    }

    private fun runMigrator(
        migrator: Migrator,
        rootManifest: JsonObject,
        fileManifests: Map<String, JsonObject>
    ): Pair<JsonObject, Map<String, JsonObject>> {
        val currentVersion =
            FormatVersion.fromString(rootManifest["formatVersion"]?.jsonPrimitive?.content ?: "0.0")

        if (currentVersion >= migrator.getOutputVersion()) {
            return Pair(rootManifest, fileManifests)
        }

        var newRootManifest = migrator.migrateRootManifest(rootManifest)
        val newFileManifests = fileManifests.mapValues { (path, fileManifest) ->
            migrator.migrateFileManifest(path, fileManifest)
        }
        newRootManifest = migrator.manipulateRootPostMigration(newRootManifest)
        return Pair(newRootManifest, newFileManifests)
    }
}
