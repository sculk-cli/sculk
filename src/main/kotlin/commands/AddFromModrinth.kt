package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import kotlinx.coroutines.runBlocking
import tech.jamalam.ctx
import tech.jamalam.pack.InMemoryPack
import tech.jamalam.pack.loadDependencyGraph
import tech.jamalam.pack.save
import tech.jamalam.util.addModrinthMod

class AddFromModrinth : CliktCommand(name = "modrinth") {
    private val query by argument()

    override fun run() = runBlocking {
        val pack = InMemoryPack(ctx.json)
        val dependencyGraph = loadDependencyGraph()
        addModrinthMod(pack, dependencyGraph, query, terminal)
        dependencyGraph.save()
        pack.save(ctx.json)
    }
}
