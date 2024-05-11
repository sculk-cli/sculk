package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import kotlinx.coroutines.runBlocking
import tech.jamalam.Context
import tech.jamalam.pack.save
import tech.jamalam.util.findAndAddCurseforgeProject

class AddFromCurseforge :
    CliktCommand(name = "curseforge", help = "Add a project to the manifest from Curseforge") {
    private val query by argument().help("The slug of the project, or a query to search for")

    override fun run() = runBlocking {
        val ctx = Context.getOrCreate(terminal)
        findAndAddCurseforgeProject(ctx, query)
        ctx.dependencyGraph.save()
        ctx.pack.save(ctx.json)
    }
}
