package tech.jamalam.services

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class ForgeMeta(private val client: HttpClient, private val json: Json) {
    suspend fun getLoaderVersions(gameVersion: String): List<String> {
        val response = client.get(
            "https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json"
        )

        return json.decodeFromString(
            MapSerializer(String.serializer(), ListSerializer(String.serializer())),
            response.bodyAsText()
        )[gameVersion].orEmpty()
    }
}
