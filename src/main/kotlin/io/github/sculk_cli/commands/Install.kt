package io.github.sculk_cli.commands

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
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import io.github.sculk_cli.Context
import io.github.sculk_cli.pack.SerialFileManifest
import io.github.sculk_cli.pack.SerialPackManifest
import io.github.sculk_cli.pack.Side
import io.github.sculk_cli.util.digestSha256
import io.github.sculk_cli.util.digestSha512
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
            val ctx = Context.Companion.getOrCreate(terminal)
            val startTime = System.currentTimeMillis()
            terminal.info("Getting pack manifest from $packLocation/manifest.sculk.json")
            val manifest = ctx.json.decodeFromString(
                SerialPackManifest.serializer(), readFileAsText("manifest.sculk.json")
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
            
            val jobs = mutableListOf<Job>()

            for (file in manifest.manifests) {
                jobs += launch {
                    progress.advance(1)
                    progress.update { context = file.path }
                    val manifestText = readFileAsText(file.path)

                    if (manifestText.toByteArray().digestSha256() != file.sha256) {
                        error("File ${file.path} was corrupted or hash was incorrect")
                    }

                    val fileManifest = ctx.json.decodeFromString(
                        SerialFileManifest.serializer(), manifestText
                    )

                    if (fileManifest.side != Side.Both) {
                        // 'Server only' should still be installed on the client generally
                        if (fileManifest.side == Side.ClientOnly && side == InstallSide.SERVER) {
                            terminal.info("Ignoring ${file.path} because it's not for the selected side ($side)")
                            return@launch
                        }
                    }

                    val fileFile =
                        File(installLocation).resolve(file.path).resolveSibling(fileManifest.filename)

                    installedItems += fileFile.path.toString()

                    if (fileFile.exists()) {
                        if (fileFile.readBytes().digestSha512() == fileManifest.hashes.sha512) {
                            terminal.info("Skipping ${file.path} because it's already downloaded")
                            return@launch
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
                    val request = ctx.client.get(downloadLink) {
                        timeout {
                            // Some mods are large.
                            requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
                        }
                    }
                    fileFile.writeBytes(request.readBytes())

                    if (fileFile.readBytes().digestSha512() != fileManifest.hashes.sha512) {
                        error("Downloaded file for ${file.path} was corrupted or hash was incorrect")
                    }

                    terminal.info("Downloaded ${file.path}")
                }
            }

            for (file in manifest.files) {
                jobs += launch {
                    progress.advance(1)
                    progress.update { context = file.path }
                    val fileBytes = readFile(file.path)

                    if (fileBytes.digestSha256() != file.sha256) {
                        error("File ${file.path} was corrupted or hash was incorrect")
                    }

                    if (file.side != Side.Both) {
                        if ((file.side == Side.ServerOnly && side == InstallSide.CLIENT) || (file.side == Side.ClientOnly && side == InstallSide.SERVER)) {
                            terminal.info("Ignoring ${file.path} because it's not for the selected side ($side)")
                            return@launch
                        }
                    }

                    val fileFile = File(installLocation).resolve(file.path)
                    installedItems += fileFile.path.toString()
                    fileFile.parentFile.mkdirs()
                    fileFile.writeBytes(fileBytes)
                    terminal.info("Downloaded ${file.path}")
                }
            }
            
            jobs.forEach { it.join() }

            for (previouslyInstalledItem in installManifest.sculkInstalledItems) {
                if (previouslyInstalledItem !in installedItems) {
                    terminal.info("Removing $previouslyInstalledItem as it is no longer part of the pack")
                    File(installLocation).resolve(previouslyInstalledItem).delete()
                }
            }

            installManifest.sculkInstalledItems = installedItems
            installManifestFile.writeText(ctx.json.encodeToString(InstallManifest.serializer(), installManifest))
            terminal.info("Installed in ${System.currentTimeMillis() - startTime}ms")
        }
    }

    private suspend fun readFileAsText(path: String): String {
        return if (packLocation.startsWith("http")) {
            Context.Companion.getOrCreate(terminal).client.get("$packLocation/$path").bodyAsText()
        } else {
            File(packLocation).resolve(path).readText()
        }
    }
    
    private suspend fun readFile(path: String): ByteArray {
        return if (packLocation.startsWith("http")) {
            Context.Companion.getOrCreate(terminal).client.get("$packLocation/$path").body()
        } else {
            File(packLocation).resolve(path).readBytes()
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
