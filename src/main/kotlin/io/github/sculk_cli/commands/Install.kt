package io.github.sculk_cli.commands

import io.github.sculk_cli.util.normalizePath
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
import com.github.ajalt.mordant.terminal.info
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
import kotlinx.coroutines.joinAll
import java.io.File

class Install :
    CliktCommand(name = "install") {
    private val packLocation by argument().help("The URL or path to the modpack")
    private val installLocation by argument().help("The path to install the modpack to")
        .default(".")
    private val side by option().enum<InstallSide>().help("The side to install for")
        .default(InstallSide.SERVER)

    override fun run() = runBlocking {
        coroutineScope {
            val ctx = Context.getOrCreate(terminal)
            val startTime = System.currentTimeMillis()
            terminal.info("Getting pack manifest from $packLocation/manifest.sculk.json")
            val manifest = ctx.json.decodeFromString(
                SerialPackManifest.serializer(), readFileAsText("manifest.sculk.json")
            )
            val installDir = File(installLocation)
            val installManifestFile = installDir.resolve("install.sculk.json")
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
                        installDir.resolve(file.path).resolveSibling(fileManifest.filename)

                    installedItems += fileFile.relativeTo(installDir).path.normalizePath()

                    val downloadLink = if (fileManifest.sources.url != null) {
                        fileManifest.sources.url.url
                    } else if (fileManifest.sources.modrinth != null) {
                        fileManifest.sources.modrinth.fileUrl
                    } else if (fileManifest.sources.curseforge != null) {
                        fileManifest.sources.curseforge.fileUrl
                    } else {
                        error("No valid source found for ${file.path}")
                    }

                    val hashes = if (fileManifest.sources.url != null) {
                        fileManifest.sources.url.hashes
                    } else if (fileManifest.sources.modrinth != null) {
                        fileManifest.sources.modrinth.hashes
                    } else if (fileManifest.sources.curseforge != null) {
                        fileManifest.sources.curseforge.hashes
                    } else {
                        error("No valid hashes found for ${file.path}")
                    }

                    if (fileFile.exists()) {
                        if (fileFile.readBytes().digestSha512() == hashes.sha512) {
                            terminal.info("Skipping ${file.path} because it's already downloaded")
                            return@launch
                        }
                    }

                    fileFile.parentFile.mkdirs()
                    val request = ctx.client.get(downloadLink) {
                        timeout {
                            // Some mods are large.
                            requestTimeoutMillis = null
                        }
                    }
                    fileFile.writeBytes(request.readRawBytes())

                    if (fileFile.readBytes().digestSha512() != hashes.sha512) {
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

                    val fileFile = installDir.resolve(file.path)
                    installedItems += fileFile.relativeTo(installDir).path.normalizePath()
                    fileFile.parentFile.mkdirs()
                    fileFile.writeBytes(fileBytes)
                    terminal.info("Downloaded ${file.path}")
                }
            }
            
            jobs.joinAll()

            for (previouslyInstalledItem in installManifest.getItemsRelativeTo(installDir)) {
                if (previouslyInstalledItem !in installedItems) {
                    terminal.info("Removing $previouslyInstalledItem as it is no longer part of the pack")
                    installDir.resolve(previouslyInstalledItem).delete()
                }
            }

            installManifest.sculkInstalledItems = installedItems
            installManifest.formatVersion = InstallManifest.CURRENT_FORMAT_VERSION
            installManifestFile.writeText(ctx.json.encodeToString(InstallManifest.serializer(), installManifest))
            terminal.info("Installed in ${System.currentTimeMillis() - startTime}ms")
        }
    }

    private suspend fun readFileAsText(path: String): String {
        return if (packLocation.startsWith("http")) {
            Context.getOrCreate(terminal).client.get("$packLocation/$path").bodyAsText()
        } else {
            File(packLocation).resolve(path).readText()
        }
    }
    
    private suspend fun readFile(path: String): ByteArray {
        return if (packLocation.startsWith("http")) {
            Context.getOrCreate(terminal).client.get("$packLocation/$path").body()
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
        // Absent in manifests written before paths were stored relative to the install directory
        var formatVersion: Int = 0,
    ) {
        fun getItemsRelativeTo(installDir: File): List<String> {
            if (formatVersion >= CURRENT_FORMAT_VERSION) {
                return sculkInstalledItems.map { it.normalizePath() }
            }

            // Format 0 stored paths relative to the working directory with the install location prepended
            val canonicalInstallDir = installDir.canonicalFile
            return sculkInstalledItems.mapNotNull {
                val file = File(it).canonicalFile
                if (file.startsWith(canonicalInstallDir)) {
                    file.relativeTo(canonicalInstallDir).path.normalizePath()
                } else {
                    null
                }
            }
        }

        companion object {
            const val CURRENT_FORMAT_VERSION = 1
        }
    }

    override fun help(context: com.github.ajalt.clikt.core.Context): String = "Install a Sculk modpack from a URL or local directory"
}
