package io.github.sculk_cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.animation.progress.advance
import com.github.ajalt.mordant.widgets.progress.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import io.github.sculk_cli.Context
import io.github.sculk_cli.pack.FileManifest
import io.github.sculk_cli.util.updateCurseforgeProject
import io.github.sculk_cli.util.updateModrinthProject

class Update : CliktCommand(name = "update", help = "Update projects from Curseforge and Modrinth") {
    private val project by argument().optional().help("The project to update, or all projects if not specified")

    override fun run() = runBlocking {
        coroutineScope {
            val ctx = Context.Companion.getOrCreate(terminal)

            if (project != null) {
                if (ctx.pack.getManifest(project!!) == null) error("Project not found")

                updateProject(ctx, project!!, ctx.pack.getManifest(project!!)!!)
            } else {
                val progress = progressBarContextLayout {
                    text(terminal.theme.info("Checking for updates"))
                    marquee(width = 40) { terminal.theme.warning(context) }
                    percentage()
                    progressBar()
                    completed(style = terminal.theme.success)
                }.animateInCoroutine(terminal,
                    total = ctx.pack.getManifests()
                        .filter { it.value.sources.modrinth != null || it.value.sources.curseforge != null }
                        .size
                        .toLong(),
                    context = "")

                launch { progress.execute() }

                for ((path, manifest) in ctx.pack.getManifests()
                    .filter { it.value.sources.modrinth != null || it.value.sources.curseforge != null }) {
                    progress.advance()
                    progress.update { context = path }
                    updateProject(ctx, path, manifest)
                }
            }

            ctx.pack.save(ctx.json)
        }
    }

    private suspend fun updateProject(
	    ctx: Context, manifestPath: String, manifest: FileManifest
    ) {
        var any = false
        if (manifest.sources.curseforge != null && updateCurseforgeProject(ctx, manifest)) {
            terminal.info("Updated $manifestPath (Curseforge)")
            any = true
        }

        if (manifest.sources.modrinth != null && updateModrinthProject(ctx, manifest)) {
            terminal.info("Updated $manifestPath (Modrinth)")
            any = true
        }

        if (any) {
            ctx.pack.setManifest(manifestPath, manifest)
        } else {
            terminal.info("No updates for $manifestPath")
        }
    }
}
