package tech.jamalam

import com.github.ajalt.clikt.completion.CompletionCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.mordant.terminal.Terminal
import tech.jamalam.commands.*
import kotlin.system.exitProcess

class Cli : NoOpCliktCommand(name = "sculk") {
    override fun aliases(): Map<String, List<String>> = mapOf(
        "rm" to listOf("remove"),
    )
}

class AddCmd : NoOpCliktCommand(name = "add", help = "Add projects to the pack") {
    override fun aliases(): Map<String, List<String>> = mapOf(
        "mr" to listOf("modrinth"),
        "cf" to listOf("curseforge"),
    )
}

class ExportCmd :
    NoOpCliktCommand(name = "export", help = "Export the pack to a different format") {
    override fun aliases(): Map<String, List<String>> = mapOf(
        "mr" to listOf("modrinth"),
        "cf" to listOf("curseforge"),
        "mmc" to listOf("multimc")
    )
}

class ImportCmd :
    NoOpCliktCommand(name = "import", help = "Import a pack from a different format") {
    override fun aliases(): Map<String, List<String>> = mapOf(
        "mr" to listOf("modrinth"), "cf" to listOf("curseforge")
    )
}

fun main(args: Array<String>) {
    val startTime = System.currentTimeMillis()
    val version = Cli::class.java.getResourceAsStream("/version").use {
        String(it?.readAllBytes() ?: "???".toByteArray())
    }

    val cli = Cli()
        .versionOption(version)
        .subcommands(Init())
        .subcommands(
            AddCmd().subcommands(
                AddByUrl(), AddFromModrinth(), AddFromCurseforge(), AddFromList()
            )
        )
        .subcommands(Update())
        .subcommands(Link(), Migrate(), Refresh())
        .subcommands(Remove())
        .subcommands(Install())
        .subcommands(ImportCmd().subcommands(ImportModrinth(), ImportCurseforge()))
        .subcommands(ExportCmd().subcommands(ExportModrinth(), ExportCurseforge(), ExportMultiMc()))
        .subcommands(ModList()).subcommands(
            CompletionCommand(
                name = "completion", help = "Generate a completion script for your shell"
            )
        )

    try {
        cli.parse(args)
    } catch (e: CliktError) {
        cli.echoFormattedHelp(e)
        exitProcess(e.statusCode)
    } catch (e: Exception) {
        val terminal = Terminal()
        terminal.danger(e.message ?: "An unknown error occurred")
        e.printStackTrace()
        exitProcess(0)
    }

//    println("Finished in ${System.currentTimeMillis() - time}ms")
}
