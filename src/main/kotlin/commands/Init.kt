package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import tech.jamalam.PrettyListPrompt
import tech.jamalam.ctx
import tech.jamalam.manifest.ModLoader
import tech.jamalam.manifest.SerialPackManifest
import tech.jamalam.manifest.SerialPackManifestModLoader
import tech.jamalam.mkdirsAndWriteJson
import tech.jamalam.prettyPrompt
import java.io.File

class Init : CliktCommand(name = "init") {
    private val path by option().prettyPrompt<File>(
        "Enter modpack path",
        default = File(".")
    )
    private val name by option().prettyPrompt<String>("Enter modpack name")
    private val loader by option().prettyPrompt<ModLoader>("Select mod loader")
    private val minecraftVersion by option().prettyPrompt<String>(
        "Select Minecraft version",
        choices = runBlocking { ctx.pistonMeta.getMcVersions() })

    override fun run() {
        if (!path.exists()) {
            path.mkdirs()
        }

        if (!path.isDirectory) {
            echo("Path is not a directory", err = true)
            return
        }

        if (path.listFiles()?.isNotEmpty() == true) {
            echo("Directory is not empty", err = true)
            return
        }

        val loaderVersion = runBlocking {
            when (loader) {
                ModLoader.Fabric -> {
                    val versions =
                        ctx.fabricMeta.getLoaderVersions(minecraftVersion).map { it.version }

                    if (versions.isEmpty()) {
                        error("No Fabric versions found for $minecraftVersion")
                    }

                    PrettyListPrompt(
                        "Select Fabric version",
                        versions,
                        terminal
                    ).ask()
                }

                ModLoader.Forge -> {
                    val versions = ctx.forgeMeta.getLoaderVersions(minecraftVersion)

                    if (versions.isEmpty()) {
                        error("No Forge versions found for $minecraftVersion")
                    }

                    PrettyListPrompt(
                        "Select Forge version",
                        versions,
                        terminal
                    ).ask()
                }

                ModLoader.Neoforge -> {
                    val versions = ctx.neoForgeMeta.getLoaderVersions(minecraftVersion)

                    if (versions.isEmpty()) {
                        error("No NeoForge versions found for $minecraftVersion")
                    }

                    PrettyListPrompt(
                        "Select NeoForge version",
                        versions,
                        terminal
                    ).ask()
                }

                ModLoader.Quilt -> {
                    val versions =
                        ctx.quiltMeta.getLoaderVersions(minecraftVersion).map { it.version }

                    if (versions.isEmpty()) {
                        error("No Quilt versions found for $minecraftVersion")
                    }

                    PrettyListPrompt(
                        "Select Fabric version",
                        versions,
                        terminal
                    ).ask()
                }
            }
        }

        val manifest = SerialPackManifest(
            name = name,
            version = "1.0.0",
            summary = null,
            minecraft = minecraftVersion,
            loader = SerialPackManifestModLoader(loader, loaderVersion),
            files = emptyList()
        )

        val manifestFile = File("$path/manifest.sculk.json")
        manifestFile.mkdirsAndWriteJson(ctx.json, manifest)
    }
}
