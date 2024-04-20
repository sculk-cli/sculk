package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.option
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import tech.jamalam.*
import tech.jamalam.pack.*

class AddByUrl : CliktCommand(name = "url") {
    private val slug by option()
        .prettyPrompt<String>("Enter project name")
    private val url by option()
        .prettyPrompt<Url>("Enter download URL")
    private val filename by option().prettyPrompt<String>("Enter file name")
    private val type by option().prettyPrompt<Type>("Select type")
    private val side by option().prettyPrompt<Side>("Select side")

    override fun run() {
        val transformedSlug = slug.lowercase().replace(" ", "-")
        val pack = InMemoryPack(ctx.json)
        val tempFile = runBlocking { downloadFileTemp(url) }
        val contents = tempFile.readBytes()

        val dir = when (type) {
            Type.Mod -> "mods"
        }

        val existingManifest = pack.getFileManifest("$dir/$transformedSlug.sculk.json")
        val fileManifest = if (existingManifest != null) {
            if (existingManifest.hashes.sha1 != contents.digestSha1() || existingManifest.hashes.sha512 != contents.digestSha512()) {
                error("File hashes do not match for $filename")
            }

            existingManifest.sources.url = FileManifestUrlSource(url.toString())
            existingManifest
        } else {
            FileManifest(
                filename = filename,
                side = side,
                hashes = FileManifestHashes(
                    sha1 = contents.digestSha1(),
                    sha512 = contents.digestSha512()
                ),
                fileSize = contents.size,
                sources = FileManifestSources(
                    curseforge = null,
                    modrinth = null,
                    url = FileManifestUrlSource(url.toString())
                )
            )
        }

        pack.setFileManifest("$dir/$transformedSlug.sculk.json", fileManifest)
        pack.save(ctx.json)
        terminal.info("Added $transformedSlug to manifest")
    }

    enum class Type {
        Mod
    }
}
