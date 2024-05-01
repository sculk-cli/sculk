package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import tech.jamalam.ctx
import tech.jamalam.pack.*
import java.io.File
import java.nio.file.Paths

class Remove : CliktCommand(name = "remove", help = "Remove a file or manifest") {
    private val filename by argument().file(mustExist = true, canBeDir = false)

    override fun run() = runBlocking {
        val pack = InMemoryPack(ctx.json, terminal = terminal)
        val dependencyGraph = loadDependencyGraph()
        val relativePath =
            filename.canonicalFile.toRelativeString(Paths.get("").toFile().canonicalFile)

        if (pack.getManifests().any { it.key == relativePath }) {
            pack.removeManifest(relativePath)
            filename.delete()
        } else {
            pack.removeFile(relativePath)
            terminal.info("File has been removed from manifest, but it will be re-added next time you refresh")
            terminal.info("Considering deleting it or adding it to the .sculkignore file")
        }

        dependencyGraph.removeDependantFromAll(relativePath)

        var unusedDependencies = dependencyGraph.getUnusedDependencies()
        while (unusedDependencies.isNotEmpty()) {
            for (dependency in unusedDependencies) {
                pack.removeManifest(dependency)
                dependencyGraph.removeDependency(dependency)
                dependencyGraph.removeDependantFromAll(dependency)
                terminal.info("Removed $dependency as it is a dependency mod that is no longer used")
                File(dependency).delete()
            }

            unusedDependencies = dependencyGraph.getUnusedDependencies()
        }

        if (dependencyGraph.isFileDependency(relativePath)) {
            terminal.warning(
                "Removing $relativePath, but it is a dependency of: ${
                    dependencyGraph.getDependants(
                        relativePath
                    )!!.joinToString(",")
                }"
            )

            dependencyGraph.removeDependency(relativePath)
        }

        dependencyGraph.save()
        pack.save(ctx.json)
        terminal.info("Removed $relativePath")
    }
}
