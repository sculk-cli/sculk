package tech.jamalam.pack

import tech.jamalam.util.pathMatchesGlob
import java.nio.file.Path
import java.nio.file.Paths

typealias SculkIgnore = List<String>

fun loadSculkIgnore(basePath: Path = Paths.get("")): SculkIgnore {
    val ignoreFile = basePath.resolve(".sculkignore")
    if (!ignoreFile.toFile().exists()) {
        return emptyList()
    }

    return ignoreFile.toFile().readLines().filter { it.isNotBlank() }
}

fun SculkIgnore.isFileIgnored(path: String): Boolean {
    return this.any { pathMatchesGlob(path, it) }
}
