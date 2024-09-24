package io.github.sculk_cli.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

class NeoForgeMeta(private val client: HttpClient) {
	suspend fun getLoaderVersions(gameVersion: String): List<String> {
		val artifact = when (gameVersion) {
			"1.20.1" -> "net/neoforged/forge"
			else -> "net/neoforged/neoforge"
		}

		return client.get(
			"https://maven.neoforged.net/api/maven/versions/releases/$artifact"
		).body<NeoForgeVersionResponse>().versions.filter {
			if (gameVersion == "1.20.1") {
				it.startsWith("1.20.1")
			} else {
				it.startsWith(
					gameVersion.trimStart(
						'1',
						'.'
					)
				)
			}
		}.reversed()
	}
}

@Serializable
data class NeoForgeVersionResponse(val versions: List<String>)
