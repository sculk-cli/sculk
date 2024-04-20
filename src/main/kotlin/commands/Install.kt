package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import tech.jamalam.ctx
import tech.jamalam.digestSha256
import tech.jamalam.digestSha512
import tech.jamalam.pack.SerialFileManifest
import tech.jamalam.pack.SerialPackManifest
import java.io.File

class Install : CliktCommand(name = "install") {
    private val packLocation by argument()
    private val installLocation by argument().default(".")

    override fun run() = runBlocking {
        val manifest =
            ctx.json.decodeFromString(
                SerialPackManifest.serializer(),
                readFile("manifest.sculk.json")
            )

        for (file in manifest.files) {
            val manifestText = readFile(file.path)

            if (manifestText.toByteArray().digestSha256() != file.sha256) {
                error("SHA256 doesn't match")
            }

            if (file.path.endsWith(".sculk.json")) {
                val fileManifest =
                    ctx.json.decodeFromString(
                        SerialFileManifest.serializer(),
                        manifestText
                    )

                val downloadLink = if (fileManifest.sources.url != null) {
                    fileManifest.sources.url.url
                } else if (fileManifest.sources.modrinth != null) {
                    fileManifest.sources.modrinth.fileUrl
                } else if (fileManifest.sources.curseforge != null) {
                    TODO()
                } else {
                    error("No valid source")
                }

                val fileFile = File(installLocation).resolve(file.path)
                    .resolveSibling(fileManifest.filename)

                fileFile.parentFile.mkdirs()
                val request = ctx.client.get(downloadLink)
                fileFile.writeBytes(request.readBytes())

                if (fileFile.readBytes().digestSha512() != fileManifest.hashes.sha512) {
                    error("File was corrupted or hash was incorrect")
                }
            } else {
                val fileFile = File(installLocation).resolve(file.path)
                fileFile.parentFile.mkdirs()
                fileFile.writeText(manifestText)
            }

            echo("Downloaded ${file.path}")
        }
    }

    private suspend fun readFile(path: String): String {
        return if (packLocation.startsWith("http")) {
            ctx.client.get("$packLocation/$path").bodyAsText()
        } else {
            File(packLocation).resolve(path).readText()
        }
    }
}
