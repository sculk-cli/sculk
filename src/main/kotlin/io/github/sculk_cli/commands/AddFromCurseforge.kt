package io.github.sculk_cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import io.github.sculk_cli.Context
import io.github.sculk_cli.pack.save
import io.github.sculk_cli.util.findAndAddCurseforgeProject

class AddFromCurseforge :
    CliktCommand(name = "curseforge", help = "Add a project to the manifest from Curseforge") {
    private val query by argument().help("The slug of the project, or a query to search for")
    private val skipDependencies by option().flag().help("Whether to skip checking for and adding dependencies.")

    override fun run() = runBlocking {
        val ctx = Context.Companion.getOrCreate(terminal)
	    findAndAddCurseforgeProject(ctx, query, skipDependencies = skipDependencies)
        ctx.dependencyGraph.save()
        ctx.pack.save(ctx.json)
    }
}
