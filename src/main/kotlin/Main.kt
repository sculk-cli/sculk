package tech.jamalam

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import io.ktor.client.*
import io.ktor.client.plugins.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import tech.jamalam.commands.*
import tech.jamalam.services.*

class Context(
    val json: Json,
    val client: HttpClient,
    val pistonMeta: PistonMeta,
    val modrinth: Modrinth,
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
    }
    val client = HttpClient {
        install(UserAgent) {
            agent = "sculk-cli/sculk (james<at>jamalam.tech / discord: jamalam)"
        }
    }
    val pistonMeta = PistonMeta(client)
    val modrinth = Modrinth(client, json)
    val fabricMeta = FabricMeta(client, json)
    val neoForgeMeta = NeoForgeMeta(client, json)
    val forgeMeta = ForgeMeta(client, json)
    val quiltMeta = QuiltMeta(client, json)
    Context(json, client, pistonMeta, modrinth, fabricMeta, neoForgeMeta, forgeMeta, quiltMeta)
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
    Cli()
        .subcommands(Init())
        .subcommands(ImportCmd().subcommands(ImportModrinth()))
        .subcommands(AddCmd().subcommands(AddByUrl(), AddFromModrinth()))
        .subcommands(Install())
        .subcommands(ExportCmd().subcommands(ExportModrinth()))
        .main(args)
}
