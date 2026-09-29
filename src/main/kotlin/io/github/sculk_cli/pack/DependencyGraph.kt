package io.github.sculk_cli.pack

import io.github.sculk_cli.Context
import io.github.sculk_cli.util.mkdirsAndWriteJson
import io.github.sculk_cli.util.normalizePath
import java.nio.file.Path
import java.nio.file.Paths

// Maps from dependency to dependants
// e.g. fabric-api --> [trinkets, utility-belt]
typealias DependencyGraph = HashMap<String, MutableSet<String>>

fun loadDependencyGraph(basePath: Path = Paths.get("")): DependencyGraph {
    val ctx = Context.getOrCreate()
    val file = basePath.resolve("dependency-graph.sculk.json").toFile()

    return if (file.exists()) {
        val graph = ctx.json.decodeFromString<DependencyGraph>(
            file.readText()
        )

        // Graphs written on Windows may contain backslashes
        val normalizedGraph = DependencyGraph()
        for ((dependency, dependants) in graph) {
            for (dependant in dependants) {
                normalizedGraph.addDependency(dependency, dependant)
            }

            normalizedGraph.getOrPut(dependency.normalizePath()) { mutableSetOf() }
        }
        normalizedGraph
    } else {
        DependencyGraph()
    }
}

fun DependencyGraph.isFileDependency(path: String): Boolean = this.any { it.key == path.normalizePath() }

fun DependencyGraph.removeDependantFromAll(dependant: String) = this.forEach { entry ->
    entry.value.removeIf { dependant.normalizePath() == it }
}

fun DependencyGraph.removeDependency(dependency: String) = this.remove(dependency.normalizePath())

fun DependencyGraph.getDependants(dependency: String) = this[dependency.normalizePath()]

fun DependencyGraph.addDependency(dependency: String, dependant: String) {
    val dependency = dependency.normalizePath()
    val dependant = dependant.normalizePath()

    if (this.containsKey(dependency)) {
        this[dependency]!!.add(dependant)
    } else {
        this[dependency] = mutableSetOf(dependant)
    }
}

fun DependencyGraph.getUnusedDependencies(): List<String> =
    this.filter { it.value.isEmpty() }.keys.toList()

fun DependencyGraph.save(basePath: Path = Paths.get("")) =
    basePath.resolve("dependency-graph.sculk.json").toFile()
        .mkdirsAndWriteJson(Context.getOrCreate().json, this)
