package io.github.sculk_cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import io.github.sculk_cli.Context
import io.github.sculk_cli.PrettyListPrompt
import io.github.sculk_cli.pack.ModLoader
import io.github.sculk_cli.pack.SerialPackManifest
import io.github.sculk_cli.pack.SerialPackManifestModLoader
import io.github.sculk_cli.pack.migration.FormatVersion
import io.github.sculk_cli.util.mkdirsAndWriteJson
import io.github.sculk_cli.util.prettyPrompt
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
        val ctx = Context.Companion.getOrCreate(terminal)
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
	        formatVersion = FormatVersion.Companion.CURRENT.toString(),
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
