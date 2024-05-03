package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import tech.jamalam.ctx
import tech.jamalam.pack.InMemoryPack
import tech.jamalam.pack.loadDependencyGraph
import tech.jamalam.pack.save
import tech.jamalam.util.addModrinthProject
import tech.jamalam.util.addModrinthVersion

class AddFromModrinth : CliktCommand(name = "modrinth", help = "Add a project to the manifest from Modrinth") {
    private val query by argument().optional()
    private val versionId by option()

    override fun run() = runBlocking {
        val pack = InMemoryPack(ctx.json, terminal = terminal)
        val dependencyGraph = loadDependencyGraph()

        if (versionId != null) {
            val version = ctx.modrinthApi.getVersion(versionId!!) ?: error("Version not found")
            val project = ctx.modrinthApi.getProject(version.projectId) ?: error("Project not found")
            addModrinthVersion(pack, dependencyGraph, project, version, terminal, false)
        } else {
            addModrinthProject(pack, dependencyGraph, query ?: error("Query must be provided if version ID is not"), terminal)
        }

        dependencyGraph.save()
        pack.save(ctx.json)
    }
}
