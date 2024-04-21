package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
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
import tech.jamalam.ctx
import tech.jamalam.digestSha256
import tech.jamalam.digestSha512
import tech.jamalam.pack.SerialFileManifest
import tech.jamalam.pack.SerialPackManifest
import tech.jamalam.pack.Side
import java.io.File

class Install : CliktCommand(name = "install") {
    private val packLocation by argument()
    private val installLocation by argument().default(".")
    private val side by option().enum<InstallSide>().default(InstallSide.SERVER)

    override fun run() = runBlocking {
        coroutineScope {
            val manifest =
                ctx.json.decodeFromString(
                    SerialPackManifest.serializer(),
                    readFile("manifest.sculk.json")
                )

            val progress = progressBarContextLayout {
                text(terminal.theme.info("Downloading files"))
                marquee(width = 60) { terminal.theme.warning(context) }
                percentage()
                progressBar()
                completed(style = terminal.theme.success)
            }.animateInCoroutine(terminal, total = manifest.files.size.toLong(), context = "")

            launch { progress.execute() }

            for (file in manifest.files) {
                progress.advance(1)
                progress.update { context = file.path }
                val manifestText = readFile(file.path)

                if (manifestText.toByteArray().digestSha256() != file.sha256) {
                    error("File ${file.path} was corrupted or hash was incorrect")
                }

                if (file.path.endsWith(".sculk.json")) {
                    val fileManifest =
                        ctx.json.decodeFromString(
                            SerialFileManifest.serializer(),
                            manifestText
                        )

                    if (fileManifest.side != Side.Both) {
                        if ((fileManifest.side == Side.ServerOnly && side == InstallSide.CLIENT) || (fileManifest.side == Side.ClientOnly && side == InstallSide.SERVER)) {
                            terminal.info("Ignoring ${file.path} because it's not for the selected side ($side)")
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

                    val fileFile = File(installLocation).resolve(file.path)
                        .resolveSibling(fileManifest.filename)

                    fileFile.parentFile.mkdirs()
                    val request = ctx.client.get(downloadLink)
                    fileFile.writeBytes(request.readBytes())

                    if (fileFile.readBytes().digestSha512() != fileManifest.hashes.sha512) {
                        error("Downloaded file for ${file.path} was corrupted or hash was incorrect")
                    }
                } else {
                    val fileFile = File(installLocation).resolve(file.path)
                    fileFile.parentFile.mkdirs()
                    fileFile.writeText(manifestText)
                }

                terminal.info("Downloaded ${file.path}")
            }
        }
    }

    private suspend fun readFile(path: String): String {
        return if (packLocation.startsWith("http")) {
            ctx.client.get("$packLocation/$path").bodyAsText()
        } else {
            File(packLocation).resolve(path).readText()
        }
    }

    enum class InstallSide {
        CLIENT,
        SERVER
    }
}
