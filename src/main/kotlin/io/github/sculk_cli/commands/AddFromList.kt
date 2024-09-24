package io.github.sculk_cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import io.github.sculk_cli.Context
import io.github.sculk_cli.pack.save
import io.github.sculk_cli.util.addModrinthProject
import io.github.sculk_cli.util.findAndAddCurseforgeProject

class AddFromList : CliktCommand(
    name = "list",
    help = "Add projects to the manifest from a userscript export list"
) {
    private val listFile by argument().file(mustExist = true, mustBeReadable = true)
        .help("The path to the list file")

    override fun run() = runBlocking {
        val ctx = Context.Companion.getOrCreate(terminal)

        listFile.forEachLine { line ->
            runBlocking {
                val parts = line.split(":")

                when (parts.size) {
                    2 -> {
                        val (site, slug) = parts

                        when (site) {
                            "curseforge" -> findAndAddCurseforgeProject(ctx, slug, ignoreIfExists = true)
                            "modrinth" -> addModrinthProject(ctx, slug, ignoreIfExists = true)
                            else -> terminal.println("Invalid site: $site")
                        }
                    }

                    else -> terminal.println("Invalid line: $line")
                }
            }
        }

        ctx.dependencyGraph.save()
        ctx.pack.save(ctx.json)
    }
}
