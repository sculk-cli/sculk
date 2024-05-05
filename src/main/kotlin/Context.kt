package tech.jamalam

import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import tech.jamalam.curseforge.CurseforgeApi
import tech.jamalam.modrinth.ModrinthApi
import tech.jamalam.pack.InMemoryPack
import tech.jamalam.pack.loadDependencyGraph
import tech.jamalam.services.*
import java.util.*

const val USER_AGENT = "sculk-cli/sculk (email: james<at>jamalam<dot>tech / discord: jamalam)"

class Context(val terminal: Terminal) {
    @OptIn(ExperimentalSerializationApi::class)
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    val client = HttpClient {
        install(UserAgent) {
            agent = USER_AGENT
        }

        install(ContentNegotiation) {
            json(json)
        }
    }

    val pistonMeta = PistonMeta(client)
    val fabricMeta = FabricMeta(client)
    val neoForgeMeta = NeoForgeMeta(client)
    val forgeMeta = ForgeMeta(client)
    val quiltMeta = QuiltMeta(client)
    val modrinth = ModrinthApi(USER_AGENT)
    val curseforge = run {
        Cli::class.java.getResourceAsStream("/curseforge-credentials.properties").use {
            val properties = Properties()
            properties.load(it)
            CurseforgeApi(
                userAgent = USER_AGENT,
                apiUrl = properties.getProperty("api_url"),
                basePath = properties.getProperty("api_base_path"),
                token = properties.getProperty("api_key")
            )
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
