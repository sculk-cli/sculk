package tech.jamalam

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import tech.jamalam.commands.*
import tech.jamalam.modrinth.ModrinthApi
import tech.jamalam.services.*
import kotlin.system.exitProcess

class Context(
    val json: Json,
    val client: HttpClient,
    val pistonMeta: PistonMeta,
    val modrinthApi: ModrinthApi,
    val curseforge: Curseforge,
    val fabricMeta: FabricMeta,
    val neoForgeMeta: NeoForgeMeta,
    val forgeMeta: ForgeMeta,
    val quiltMeta: QuiltMeta,
)

@OptIn(ExperimentalSerializationApi::class)
val ctx = run {
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    val client = HttpClient {
        install(UserAgent) {
            agent = "sculk-cli/sculk (email: james<at>jamalam<dot>tech / discord: jamalam)"
        }

        install(ContentNegotiation) {
            json(json)
        }
    }
    val pistonMeta = PistonMeta(client)
    val modrinth = ModrinthApi("sculk-cli/sculk (email: james<at>jamalam<dot>tech / discord: jamalam)")
    val curseforge = Curseforge(client)
    val fabricMeta = FabricMeta(client)
    val neoForgeMeta = NeoForgeMeta(client)
    val forgeMeta = ForgeMeta(client)
    val quiltMeta = QuiltMeta(client)
    Context(json, client, pistonMeta, modrinth, curseforge, fabricMeta, neoForgeMeta, forgeMeta, quiltMeta)
}

class Cli : CliktCommand() {
    override fun run() = Unit
}

class AddCmd : CliktCommand(name = "add") {
    override fun run() = Unit
}

class ExportCmd : CliktCommand(name = "export") {
    override fun run() = Unit
}

class ImportCmd : CliktCommand(name = "import") {
    override fun run() = Unit
}

fun main(args: Array<String>) {
    val cli = Cli()
        .subcommands(Init())
        .subcommands(ImportCmd().subcommands(ImportModrinth()))
        .subcommands(AddCmd().subcommands(AddByUrl(), AddFromModrinth(), AddFromCurseforge(), AddByFile()))
        .subcommands(Refresh())
        .subcommands(Install())
        .subcommands(ExportCmd().subcommands(ExportModrinth()))

    try {
        cli.parse(args)
    } catch (e: CliktError) {
        cli.echoFormattedHelp(e)
        exitProcess(e.statusCode)
    } catch (e: Exception) {
        val terminal = Terminal()
        terminal.danger(e.message ?: "An unknown error occurred")

        if (e.message == null) {
            e.printStackTrace()
        }

        exitProcess(0)
    }
}
