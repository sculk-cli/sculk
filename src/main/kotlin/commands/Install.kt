package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.animation.progress.advance
import com.github.ajalt.mordant.widgets.progress.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import tech.jamalam.Context
import tech.jamalam.pack.SerialFileManifest
import tech.jamalam.pack.SerialPackManifest
import tech.jamalam.pack.Side
import tech.jamalam.util.digestSha256
import tech.jamalam.util.digestSha512
import java.io.File

class Install :
    CliktCommand(name = "install", help = "Install a Sculk modpack from a URL or local directory") {
    private val packLocation by argument().help("The URL or path to the modpack")
    private val installLocation by argument().help("The path to install the modpack to")
        .default(".")
    private val side by option().enum<InstallSide>().help("The side to install for")
        .default(InstallSide.SERVER)

    override fun run() = runBlocking {
        coroutineScope {
            val ctx = Context.getOrCreate(terminal)
            val manifest = ctx.json.decodeFromString(
                SerialPackManifest.serializer(), readFile("manifest.sculk.json")
            )
            val installManifestFile = File(installLocation).resolve("install.sculk.json")
            val installManifest = if (installManifestFile.exists()) {
                ctx.json.decodeFromString(
                    InstallManifest.serializer(), installManifestFile.readText()
                )
            } else {
                InstallManifest(mutableListOf())
            }
            val installedItems = mutableListOf<String>()

            terminal.info("Installing pack ${manifest.name} to $installLocation")

            val progress = progressBarContextLayout {
                text(terminal.theme.info("Downloading files"))
                marquee(width = 60) { terminal.theme.warning(context) }
                percentage()
                progressBar()
                completed(style = terminal.theme.success)
            }.animateInCoroutine(
                terminal,
                total = manifest.manifests.size.toLong() + manifest.files.size.toLong(),
                context = ""
            )

            launch { progress.execute() }

            for (file in manifest.manifests) {
                progress.advance(1)
                progress.update { context = file.path }
                val manifestText = readFile(file.path)

                if (manifestText.toByteArray().digestSha256() != file.sha256) {
                    error("File ${file.path} was corrupted or hash was incorrect")
                }

                val fileManifest = ctx.json.decodeFromString(
                    SerialFileManifest.serializer(), manifestText
                )

                if (fileManifest.side != Side.Both) {
                    if ((fileManifest.side == Side.ServerOnly && side == InstallSide.CLIENT) || (fileManifest.side == Side.ClientOnly && side == InstallSide.SERVER)) {
                        terminal.info("Ignoring ${file.path} because it's not for the selected side ($side)")
                        continue
                    }
                }

                val fileFile =
                    File(installLocation).resolve(file.path).resolveSibling(fileManifest.filename)

                installedItems += fileFile.path.toString()

                if (fileFile.exists()) {
                    if (fileFile.readBytes().digestSha512() == fileManifest.hashes.sha512) {
                        terminal.info("Skipping ${file.path} because it's already downloaded")
                        continue
                    }
                }

                val downloadLink = if (fileManifest.sources.url != null) {
                    fileManifest.sources.url.url
                } else if (fileManifest.sources.modrinth != null) {
                    fileManifest.sources.modrinth.fileUrl
                } else if (fileManifest.sources.curseforge != null) {
                    fileManifest.sources.curseforge.fileUrl
                } else {
                    error("No valid source found for ${file.path}")
                }

                fileFile.parentFile.mkdirs()
                val request = ctx.client.get(downloadLink)
                fileFile.writeBytes(request.readBytes())

                if (fileFile.readBytes().digestSha512() != fileManifest.hashes.sha512) {
                    error("Downloaded file for ${file.path} was corrupted or hash was incorrect")
                }

                terminal.info("Downloaded ${file.path}")
            }

            for (file in manifest.files) {
                progress.advance(1)
                progress.update { context = file.path }
                val fileText = readFile(file.path)

                if (fileText.toByteArray().digestSha256() != file.sha256) {
                    error("File ${file.path} was corrupted or hash was incorrect")
                }

                if (file.side != Side.Both) {
                    if ((file.side == Side.ServerOnly && side == InstallSide.CLIENT) || (file.side == Side.ClientOnly && side == InstallSide.SERVER)) {
                        terminal.info("Ignoring ${file.path} because it's not for the selected side ($side)")
                        continue
                    }
                }

                val fileFile = File(installLocation).resolve(file.path)
                installedItems += fileFile.path.toString()
                fileFile.parentFile.mkdirs()
                fileFile.writeText(fileText)
                terminal.info("Downloaded ${file.path}")
            }

            for (previouslyInstalledItem in installManifest.sculkInstalledItems) {
                if (previouslyInstalledItem !in installedItems) {
                    terminal.info("Removing $previouslyInstalledItem as it is no longer part of the pack")
                    File(installLocation).resolve(previouslyInstalledItem).delete()
                }
            }

            installManifest.sculkInstalledItems = installedItems
            installManifestFile.writeText(ctx.json.encodeToString(InstallManifest.serializer(), installManifest))
        }
    }

    private suspend fun readFile(path: String): String {
        return if (packLocation.startsWith("http")) {
            Context.getOrCreate(terminal).client.get("$packLocation/$path").bodyAsText()
        } else {
            File(packLocation).resolve(path).readText()
        }
    }

    enum class InstallSide {
        CLIENT, SERVER
    }

    @Serializable
    data class InstallManifest(
        var sculkInstalledItems: MutableList<String>,
    )
}
