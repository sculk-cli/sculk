package tech.jamalam.pack

import tech.jamalam.Context
import tech.jamalam.util.mkdirsAndWriteJson
import java.nio.file.Path
import java.nio.file.Paths

// Map is dependency to dependants
// e.g. fabric-api --> [trinkets, utility-belt]
typealias DependencyGraph = HashMap<String, MutableList<String>>

fun loadDependencyGraph(basePath: Path = Paths.get("")): DependencyGraph {
    val ctx = Context.getOrCreate()
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

fun DependencyGraph.getUnusedDependencies(): List<String> =
    this.filter { it.value.isEmpty() }.keys.toList()

fun DependencyGraph.save(basePath: Path = Paths.get("")) =
    basePath.resolve("dependency-graph.sculk.json").toFile()
        .mkdirsAndWriteJson(Context.getOrCreate().json, this)

