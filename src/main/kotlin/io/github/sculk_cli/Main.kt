package io.github.sculk_cli

import com.github.ajalt.clikt.completion.CompletionCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.mordant.terminal.Terminal
import io.github.sculk_cli.commands.AddByUrl
import io.github.sculk_cli.commands.AddFromCurseforge
import io.github.sculk_cli.commands.AddFromList
import io.github.sculk_cli.commands.AddFromModrinth
import io.github.sculk_cli.commands.ExportCurseforge
import io.github.sculk_cli.commands.ExportModrinth
import io.github.sculk_cli.commands.ExportMultiMc
import io.github.sculk_cli.commands.ImportCurseforge
import io.github.sculk_cli.commands.ImportModrinth
import io.github.sculk_cli.commands.Init
import io.github.sculk_cli.commands.Install
import io.github.sculk_cli.commands.Link
import io.github.sculk_cli.commands.Migrate
import io.github.sculk_cli.commands.ModList
import io.github.sculk_cli.commands.Refresh
import io.github.sculk_cli.commands.Remove
import io.github.sculk_cli.commands.Update
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
		exitProcess(1)
	}
}
