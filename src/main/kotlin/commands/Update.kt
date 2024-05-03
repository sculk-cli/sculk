package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.animation.progress.advance
import com.github.ajalt.mordant.widgets.progress.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tech.jamalam.ctx
import tech.jamalam.pack.FileManifest
import tech.jamalam.pack.InMemoryPack
import tech.jamalam.util.updateCurseforgeProject
import tech.jamalam.util.updateModrinthProject

class Update : CliktCommand(name = "update") {
    private val project by argument().optional()

    override fun run() = runBlocking {
        coroutineScope {
            val pack = InMemoryPack(ctx.json, terminal = terminal)

            if (project != null) {
                if (pack.getManifest(project!!) == null) error("Project not found")

                updateProject(pack, project!!, pack.getManifest(project!!)!!)
            } else {
                val progress = progressBarContextLayout {
                    text(terminal.theme.info("Checking for updates"))
                    marquee(width = 40) { terminal.theme.warning(context) }
                    percentage()
                    progressBar()
                    completed(style = terminal.theme.success)
                }.animateInCoroutine(terminal,
                    total = pack.getManifests()
                        .filter { it.value.sources.modrinth != null || it.value.sources.curseforge != null }
                        .size
                        .toLong(),
                    context = "")

                launch { progress.execute() }

                for ((path, manifest) in pack.getManifests()
                    .filter { it.value.sources.modrinth != null || it.value.sources.curseforge != null }) {
                    progress.advance()
                    progress.update { context = path }
                    updateProject(pack, path, manifest)
                }
            }

            pack.save(ctx.json)
        }
    }

    private suspend fun updateProject(
        pack: InMemoryPack, manifestPath: String, manifest: FileManifest
    ) {
        var any = false
        if (manifest.sources.curseforge != null && updateCurseforgeProject(pack, manifest)) {
            terminal.info("Updated $manifestPath (Curseforge)")
            any = true
        }

        if (manifest.sources.modrinth != null && updateModrinthProject(pack, manifest)) {
            terminal.info("Updated $manifestPath (Modrinth)")
            any = true
        }

        if (any) {
            pack.setManifest(manifestPath, manifest)
        } else {
            terminal.info("No updates for $manifestPath")
        }
    }
}
