package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import tech.jamalam.Context
import tech.jamalam.PrettyListPrompt
import tech.jamalam.pack.ModLoader
import tech.jamalam.pack.SerialPackManifest
import tech.jamalam.pack.SerialPackManifestModLoader
import tech.jamalam.pack.migration.FormatVersion
import tech.jamalam.util.mkdirsAndWriteJson
import tech.jamalam.util.prettyPrompt
import java.io.File
import java.nio.file.Paths

class Init : CliktCommand(name = "init", help = "Initialize a new Sculk modpack") {
    private val path by argument().file().help("The path to the modpack folder")
        .default(Paths.get("").toFile().canonicalFile)

    private val name by option().prettyPrompt<String>("Enter modpack name")
        .help("The name of the modpack")
    private val loader by option().prettyPrompt<ModLoader>("Select mod loader")
        .help("The mod loader to use")

    override fun run() = runBlocking {
        val ctx = Context.getOrCreate(terminal)
        if (!path.exists()) {
            path.mkdirs()
        }

        if (!path.isDirectory) {
            error("Path is not a directory")
        }

        if (path.listFiles()?.isNotEmpty() == true) {
            error("Directory is not empty")
        }

        val minecraftVersion = PrettyListPrompt(
            "Select Minecraft version",
            ctx.pistonMeta.getMcVersions(),
            terminal
        ).ask()

        val loaderVersion = when (loader) {
            ModLoader.Fabric -> {
                val versions =
                    ctx.fabricMeta.getLoaderVersions(minecraftVersion).map { it.version }

                if (versions.isEmpty()) {
                    error("No Fabric versions found for $minecraftVersion")
                }

                PrettyListPrompt(
                    "Select Fabric version", versions, terminal
                ).ask()
            }

            ModLoader.Forge -> {
                val versions = ctx.forgeMeta.getLoaderVersions(minecraftVersion)

                if (versions.isEmpty()) {
                    error("No Forge versions found for $minecraftVersion")
                }

                PrettyListPrompt(
                    "Select Forge version", versions, terminal
                ).ask()
            }

            ModLoader.Neoforge -> {
                val versions = ctx.neoForgeMeta.getLoaderVersions(minecraftVersion)

                if (versions.isEmpty()) {
                    error("No NeoForge versions found for $minecraftVersion")
                }

                PrettyListPrompt(
                    "Select NeoForge version", versions, terminal
                ).ask()
            }

            ModLoader.Quilt -> {
                val versions =
                    ctx.quiltMeta.getLoaderVersions(minecraftVersion).map { it.version }

                if (versions.isEmpty()) {
                    error("No Quilt versions found for $minecraftVersion")
                }

                PrettyListPrompt(
                    "Select Fabric version", versions, terminal
                ).ask()
            }
        }

        val manifest = SerialPackManifest(
            formatVersion = FormatVersion.CURRENT.toString(),
            name = name,
            version = "1.0.0",
            summary = null,
            author = null,
            minecraft = minecraftVersion,
            loader = SerialPackManifestModLoader(loader, loaderVersion),
            manifests = emptyList(),
            files = emptyList()
        )

        val manifestFile = File("$path/manifest.sculk.json")
        manifestFile.mkdirsAndWriteJson(ctx.json, manifest)
    }
}
