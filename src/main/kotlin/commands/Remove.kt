package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import tech.jamalam.ctx
import tech.jamalam.pack.InMemoryPack
import java.nio.file.Paths

class Remove : CliktCommand(name = "remove") {
    private val filename by argument().file(mustExist = true, canBeDir = false)

    override fun run() = runBlocking {
        val pack = InMemoryPack(ctx.json)
        val relativePath =
            filename.canonicalFile.toRelativeString(Paths.get("").toFile().canonicalFile)

        if (pack.getManifests().any { it.key == relativePath }) {
            pack.removeManifest(relativePath)
        } else {
            pack.removeFile(relativePath)
            terminal.info("File has been removed from manifest, but it will be re-added next time you refresh")
            terminal.info("Considering deleting it or adding it to the .sculkignore file")
        }

        pack.save(ctx.json)
        terminal.info("Removed $relativePath")
    }
}
