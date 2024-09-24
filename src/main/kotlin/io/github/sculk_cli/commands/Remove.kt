package io.github.sculk_cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import io.github.sculk_cli.Context
import io.github.sculk_cli.pack.getDependants
import io.github.sculk_cli.pack.getUnusedDependencies
import io.github.sculk_cli.pack.isFileDependency
import io.github.sculk_cli.pack.removeDependantFromAll
import io.github.sculk_cli.pack.removeDependency
import io.github.sculk_cli.pack.save
import java.io.File
import java.nio.file.Paths

class Remove : CliktCommand(name = "remove", help = "Remove a file or manifest") {
    private val filename by argument().file(mustExist = true, canBeDir = false)
        .help("The file to remove")

    override fun run() = runBlocking {
        val ctx = Context.Companion.getOrCreate(terminal)
        val relativePath =
            filename.canonicalFile.toRelativeString(Paths.get("").toFile().canonicalFile)

        if (ctx.pack.getManifests().any { it.key == relativePath }) {
            ctx.pack.removeManifest(relativePath)
            filename.delete()
        } else {
            ctx.pack.removeFile(relativePath)
            terminal.info("File has been removed from manifest, but it will be re-added next time you refresh")
            terminal.info("Considering deleting it or adding it to the .sculkignore file")
        }

        ctx.dependencyGraph.removeDependantFromAll(relativePath)

        var unusedDependencies = ctx.dependencyGraph.getUnusedDependencies()
        while (unusedDependencies.isNotEmpty()) {
            for (dependency in unusedDependencies) {
                ctx.pack.removeManifest(dependency)
                ctx.dependencyGraph.removeDependency(dependency)
                ctx.dependencyGraph.removeDependantFromAll(dependency)
                terminal.info("Removed $dependency as it is a dependency mod that is no longer used")
                File(dependency).delete()
            }

            unusedDependencies = ctx.dependencyGraph.getUnusedDependencies()
        }

        if (ctx.dependencyGraph.isFileDependency(relativePath)) {
            terminal.warning(
                "Removing $relativePath, but it is a dependency of: ${
                    ctx.dependencyGraph.getDependants(
                        relativePath
                    )!!.joinToString(",")
                }"
            )

            ctx.dependencyGraph.removeDependency(relativePath)
        }

        ctx.dependencyGraph.save()
        ctx.pack.save(ctx.json)
        terminal.info("Removed $relativePath")
    }
}
