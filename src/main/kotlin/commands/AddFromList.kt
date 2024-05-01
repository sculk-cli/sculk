package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import tech.jamalam.ctx
import tech.jamalam.pack.InMemoryPack
import tech.jamalam.pack.loadDependencyGraph
import tech.jamalam.pack.save
import tech.jamalam.util.addCurseforgeMod
import tech.jamalam.util.addModrinthMod

class AddFromList : CliktCommand(name = "list", help = "Add projects to the manifest from a userscript export list") {
    private val listFile by argument().file(mustExist = true, mustBeReadable = true)

    override fun run() = runBlocking {
        val pack = InMemoryPack(ctx.json, terminal = terminal)
        val dependencyGraph = loadDependencyGraph()

        listFile.forEachLine { line ->
            runBlocking {
                val parts = line.split(":")

                when (parts.size) {
                    2 -> {
                        val (site, slug) = parts

                        when (site) {
                            "curseforge" -> addCurseforgeMod(pack, dependencyGraph, slug, terminal)
                            "modrinth" -> addModrinthMod(pack, dependencyGraph, slug, terminal)
                            else -> terminal.println("Invalid site: $site")
                        }
                    }

                    else -> terminal.println("Invalid line: $line")
                }
            }
        }

        dependencyGraph.save()
        pack.save(ctx.json)
    }
}
