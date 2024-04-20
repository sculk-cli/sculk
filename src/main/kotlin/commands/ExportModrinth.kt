package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import kotlinx.coroutines.runBlocking
import tech.jamalam.ctx
import tech.jamalam.manifest.InMemoryPack
import tech.jamalam.mrpack.exportModrinthPack

class ExportModrinth : CliktCommand(name = "modrinth") {
    override fun run() = runBlocking {
        val pack = InMemoryPack(ctx.json)
        exportModrinthPack(terminal, pack)
    }
}
