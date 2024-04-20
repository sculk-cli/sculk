package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import tech.jamalam.ctx
import tech.jamalam.pack.InMemoryPack
import java.nio.file.Paths

class AddByFile : CliktCommand(name = "file") {
    private val file by argument().file(mustExist = true, canBeDir = false, mustBeReadable = true)

    override fun run() {
        val pack = InMemoryPack(ctx.json)
        pack.addDirectFile(file.relativeTo(Paths.get("").toFile()).toString())
        echo("Added $file to manifest")
        terminal.println(terminal.theme.info("Saving pack manifest..."))
        pack.save(ctx.json)
    }
}

