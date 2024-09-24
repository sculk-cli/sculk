package io.github.sculk_cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import io.github.sculk_cli.Context
import io.github.sculk_cli.pack.save
import io.github.sculk_cli.util.addModrinthProject
import io.github.sculk_cli.util.addModrinthVersion

class AddFromModrinth :
    CliktCommand(name = "modrinth", help = "Add a project to the manifest from Modrinth") {
    private val query by argument().optional()
        .help("The slug of the project, or a query to search for")
    private val versionId by option().help("The version ID to add. If this is provided, the query will be ignored.")
    private val skipDependencies by option().flag().help("Whether to skip checking for and adding dependencies.")

    override fun run() = runBlocking {
        val ctx = Context.Companion.getOrCreate(terminal)

        if (versionId != null) {
            val version = ctx.modrinth.getVersion(versionId!!) ?: error("Version not found")
            val project = ctx.modrinth.getProject(version.projectId) ?: error("Project not found")
	        addModrinthVersion(ctx, project, version, ignoreIfExists = false, downloadDependencies = !skipDependencies)
        } else {
	        addModrinthProject(
		        ctx,
		        query ?: error("Query must be provided if version ID is not"),
		        skipDependencies = skipDependencies
	        )
        }

        ctx.dependencyGraph.save()
        ctx.pack.save(ctx.json)
    }
}
