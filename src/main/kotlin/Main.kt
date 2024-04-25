package tech.jamalam

import com.github.ajalt.clikt.completion.CompletionCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import tech.jamalam.commands.*
import tech.jamalam.curseforge.CurseforgeApi
import tech.jamalam.modrinth.ModrinthApi
import tech.jamalam.services.*
import java.util.*
import kotlin.system.exitProcess

class Context(
    val json: Json,
    val client: HttpClient,
    val pistonMeta: PistonMeta,
    val modrinthApi: ModrinthApi,
    val curseforgeApi: CurseforgeApi,
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
    val modrinthApi =
        ModrinthApi("sculk-cli/sculk (email: james<at>jamalam<dot>tech / discord: jamalam)")
    val curseforgeApi = run {
        Cli::class.java.getResourceAsStream("/curseforge-credentials.properties").use {
            val properties = Properties()
            properties.load(it)
            CurseforgeApi(
                userAgent = "sculk-cli/sculk (email: james<at>jamalam<dot>tech / discord: jamalam)",
                apiUrl = properties.getProperty("api_url"),
                basePath = properties.getProperty("api_base_path"),
                token = properties.getProperty("api_key")
            )
        }
    }
    val fabricMeta = FabricMeta(client)
    val neoForgeMeta = NeoForgeMeta(client)
    val forgeMeta = ForgeMeta(client)
    val quiltMeta = QuiltMeta(client)
    Context(
        json,
        client,
        pistonMeta,
        modrinthApi,
        curseforgeApi,
        fabricMeta,
        neoForgeMeta,
        forgeMeta,
        quiltMeta
    )
}

class Cli : NoOpCliktCommand() {
    override fun aliases(): Map<String, List<String>> = mapOf(
        "rm" to listOf("remove"),
    )
}

class AddCmd : NoOpCliktCommand(name = "add") {
    override fun aliases(): Map<String, List<String>> = mapOf(
        "mr" to listOf("modrinth"),
        "cf" to listOf("curseforge"),
    )
}

class ExportCmd : NoOpCliktCommand(name = "export") {
    override fun aliases(): Map<String, List<String>> = mapOf(
        "mr" to listOf("modrinth"),
    )
}

class ImportCmd : NoOpCliktCommand(name = "import") {
    override fun aliases(): Map<String, List<String>> = mapOf(
        "mr" to listOf("modrinth"),
    )
}

fun main(args: Array<String>) {
    val cli = Cli()
        .subcommands(Init())
        .subcommands(ImportCmd().subcommands(ImportModrinth()))
        .subcommands(
            AddCmd().subcommands(
                AddByUrl(),
                AddFromModrinth(),
                AddFromCurseforge(),
            )
        )
        .subcommands(Refresh())
        .subcommands(Remove())
        .subcommands(Install())
        .subcommands(ModList())
        .subcommands(ExportCmd().subcommands(ExportModrinth()))
        .subcommands(CompletionCommand(name = "completion"))

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
}
