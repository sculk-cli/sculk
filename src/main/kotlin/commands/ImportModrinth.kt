package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.runBlocking
import tech.jamalam.mrpack.importModrinthPack
import java.nio.file.Paths

class ImportModrinth : CliktCommand(name = "modrinth") {
    private val mrpack by argument().file(mustExist = true, mustBeReadable = true)

    override fun run() = runBlocking {
        importModrinthPack(terminal, Paths.get(""), mrpack.toPath())
    }
}
