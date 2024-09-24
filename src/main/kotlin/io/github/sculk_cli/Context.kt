package io.github.sculk_cli

import com.github.ajalt.mordant.terminal.Terminal
import io.github.sculk_cli.services.FabricMeta
import io.github.sculk_cli.services.ForgeMeta
import io.github.sculk_cli.services.NeoForgeMeta
import io.github.sculk_cli.services.PistonMeta
import io.github.sculk_cli.services.QuiltMeta
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import io.github.sculk_cli.curseforge.CurseforgeApi
import io.github.sculk_cli.pack.InMemoryPack
import io.github.sculk_cli.pack.loadDependencyGraph
import io.github.sculk_cli.modrinth.ModrinthApi
import java.util.*

const val USER_AGENT = "sculk-cli/sculk (email: james<at>jamalam<dot>tech / discord: jamalam)"

class Context(val terminal: Terminal) {
    @OptIn(ExperimentalSerializationApi::class)
    val json by lazy {
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }
    }

    val client by lazy {
        HttpClient {
            install(UserAgent) {
                agent = USER_AGENT
            }

            install(ContentNegotiation) {
                json(json)
            }

            install(HttpTimeout) { }
        }
    }

    val pistonMeta by lazy { PistonMeta(client) }
    val fabricMeta by lazy { FabricMeta(client) }
    val neoForgeMeta by lazy { NeoForgeMeta(client) }
    val forgeMeta by lazy { ForgeMeta(client) }
    val quiltMeta by lazy { QuiltMeta(client) }
    val modrinth by lazy { ModrinthApi(client) }
    val curseforge by lazy {
        run {
            Cli::class.java.getResourceAsStream("/curseforge-credentials.properties").use {
                val properties = Properties()
                properties.load(it)
                CurseforgeApi(
                    client,
                    apiUrl = properties.getProperty("api_url"),
                    basePath = properties.getProperty("api_base_path"),
                    token = properties.getProperty("api_key")
                )
            }
        }
    }

    val pack by lazy {
        InMemoryPack(this)
    }

    val dependencyGraph by lazy {
        loadDependencyGraph()
    }

    companion object {
        private lateinit var instance: Context

        fun getOrCreate(terminal: Terminal): Context {
            if (!::instance.isInitialized) {
                instance = Context(terminal)
            }
            return instance
        }

        fun getOrCreate(): Context {
            if (!::instance.isInitialized) {
                instance = Context(Terminal())
            }

            return instance
        }
    }
}
