package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import tech.jamalam.Context
import tech.jamalam.pack.save
import tech.jamalam.util.addModrinthProject
import tech.jamalam.util.findAndAddCurseforgeProject

class AddFromList : CliktCommand(
    name = "list",
    help = "Add projects to the manifest from a userscript export list"
) {
    private val listFile by argument().file(mustExist = true, mustBeReadable = true)

    override fun run() = runBlocking {
        val ctx = Context.getOrCreate(terminal)

        listFile.forEachLine { line ->
            runBlocking {
                val parts = line.split(":")

                when (parts.size) {
                    2 -> {
                        val (site, slug) = parts

                        when (site) {
                            "curseforge" -> findAndAddCurseforgeProject(ctx, slug)
                            "modrinth" -> addModrinthProject(ctx, slug)
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
