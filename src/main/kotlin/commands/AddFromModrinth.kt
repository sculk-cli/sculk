package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import tech.jamalam.Context
import tech.jamalam.pack.save
import tech.jamalam.util.addModrinthProject
import tech.jamalam.util.addModrinthVersion

class AddFromModrinth :
    CliktCommand(name = "modrinth", help = "Add a project to the manifest from Modrinth") {
    private val query by argument().optional()
        .help("The slug of the project, or a query to search for")
    private val versionId by option().help("The version ID to add. If this is provided, the query will be ignored.")

    override fun run() = runBlocking {
        val ctx = Context.getOrCreate(terminal)

        if (versionId != null) {
            val version = ctx.modrinth.getVersion(versionId!!) ?: error("Version not found")
            val project = ctx.modrinth.getProject(version.projectId) ?: error("Project not found")
            addModrinthVersion(ctx, project, version, ignoreIfExists = false)
        } else {
            addModrinthProject(ctx, query ?: error("Query must be provided if version ID is not"))
        }

        ctx.dependencyGraph.save()
        ctx.pack.save(ctx.json)
    }
}
