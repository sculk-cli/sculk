package io.github.sculk_cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.animation.progress.advance
import com.github.ajalt.mordant.terminal.info
import com.github.ajalt.mordant.widgets.progress.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import io.github.sculk_cli.Context
import io.github.sculk_cli.pack.Side
import io.github.sculk_cli.util.digestSha512
import kotlinx.coroutines.joinAll
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path
import kotlin.io.readBytes

class ExportZip :
    CliktCommand(name = "zip") {
    private val side by option().enum<InstallSide>().help("The side to export for")
        .default(InstallSide.SERVER)

    override fun run() = runBlocking {
        coroutineScope {
            val ctx = Context.getOrCreate(terminal)
            val startTime = System.currentTimeMillis()
            val outFile = File("${ctx.pack.getManifest().name}-${ctx.pack.getManifest().version}.zip")
            val out = ZipOutputStream(FileOutputStream(outFile))

            val progress = progressBarContextLayout {
                text(terminal.theme.info("Downloading files"))
                marquee(width = 60) { terminal.theme.warning(context) }
                percentage()
                progressBar()
                completed(style = terminal.theme.success)
            }.animateInCoroutine(
                terminal,
                total = ctx.pack.getManifests().size.toLong() + ctx.pack.getFiles().size.toLong(),
                context = ""
            )

            launch { progress.execute() }
            val jobs = mutableListOf<Job>()

            for ((path, fileManifest) in ctx.pack.getManifests()) {
                jobs += launch {
                    progress.advance(1)
                    progress.update { context = path }
                    if (fileManifest.side != Side.Both) {
                        // 'Server only' should still be installed on the client generally
                        if (fileManifest.side == Side.ClientOnly && side == InstallSide.SERVER) {
                            terminal.info("Ignoring $path because it's not for the selected side ($side)")
                            return@launch
                        }
                    }

                    val downloadLink = if (fileManifest.sources.url != null) {
                        fileManifest.sources.url!!.url
                    } else if (fileManifest.sources.modrinth != null) {
                        fileManifest.sources.modrinth!!.fileUrl
                    } else if (fileManifest.sources.curseforge != null) {
                        fileManifest.sources.curseforge!!.fileUrl
                    } else {
                        error("No valid source found for $path")
                    }

                    val hashes = if (fileManifest.sources.url != null) {
                        fileManifest.sources.url!!.hashes
                    } else if (fileManifest.sources.modrinth != null) {
                        fileManifest.sources.modrinth!!.hashes
                    } else if (fileManifest.sources.curseforge != null) {
                        fileManifest.sources.curseforge!!.hashes
                    } else {
                        error("No valid hashes found for $path")
                    }

                    val request = ctx.client.get(downloadLink) {
                        timeout {
                            // Some mods are large.
                            requestTimeoutMillis = null
                        }
                    }
                    val bytes = request.readRawBytes()
                    if (bytes.digestSha512() != hashes.sha512) {
                        error("Downloaded file for $path was corrupted or hash was incorrect")
                    }

                    out.putNextEntry(ZipEntry(Path(path).resolveSibling(fileManifest.filename).toString()))
                    out.write(bytes, 0, bytes.size)
                    out.closeEntry()
                    terminal.info("Zipped $path")
                }
            }

            for (file in ctx.pack.getFiles()) {
                jobs += launch {
                    progress.advance(1)
                    progress.update { context = file.path }
                    if (file.side != Side.Both) {
                        if ((file.side == Side.ServerOnly && side == InstallSide.CLIENT) || (file.side == Side.ClientOnly && side == InstallSide.SERVER)) {
                            terminal.info("Ignoring ${file.path} because it's not for the selected side ($side)")
                            return@launch
                        }
                    }

                    out.putNextEntry(ZipEntry(file.path))
                    val bytes = ctx.pack.getBasePath().resolve(file.path).toFile().readBytes()
                    out.write(bytes, 0, bytes.size)
                    out.closeEntry()
                    terminal.info("Zipped ${file.path}")
                }
            }
            
            jobs.joinAll()
            out.close()
            terminal.info("Exported ${ctx.pack.getManifest().name} to ${outFile.name} in ${System.currentTimeMillis() - startTime}ms")
        }
    }

    enum class InstallSide {
        CLIENT, SERVER
    }

    override fun help(context: com.github.ajalt.clikt.core.Context): String = "Export a ZIP file with all mods downloaded"
}
