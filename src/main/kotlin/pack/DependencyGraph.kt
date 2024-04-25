package tech.jamalam.pack

import kotlinx.serialization.encodeToString
import tech.jamalam.ctx
import java.nio.file.Path
import java.nio.file.Paths

// Map is dependency to dependants
// e.g. fabric-api --> [trinkets, utility-belt]
typealias DependencyGraph = HashMap<String, MutableList<String>>

fun loadDependencyGraph(basePath: Path = Paths.get("")): DependencyGraph {
    val file = basePath.resolve("dependency-graph.sculk.json").toFile()

    return if (file.exists()) {
        ctx.json.decodeFromString<DependencyGraph>(
            file.readText()
        )
    } else {
        DependencyGraph()
    }
}

fun DependencyGraph.isFileDependency(path: String): Boolean = this.any { it.key == path }

fun DependencyGraph.removeDependantFromAll(dependant: String) = this.forEach { entry ->
    entry.value.removeIf { dependant == it }
}

fun DependencyGraph.removeDependency(dependency: String) = this.remove(dependency)

fun DependencyGraph.getDependants(dependency: String) = this[dependency]

fun DependencyGraph.addDependency(dependency: String, dependant: String) {
    if (this.containsKey(dependency)) {
        this[dependency]!!.add(dependant)
    } else {
        this[dependency] = mutableListOf(dependant)
    }
}

fun DependencyGraph.getUnusedDependencies(): List<String> = this.filter { it.value.isEmpty() }.keys.toList()

fun DependencyGraph.save(basePath: Path = Paths.get("")) =
    basePath.resolve("dependency-graph.sculk.json").toFile().writeText(ctx.json.encodeToString(this))

