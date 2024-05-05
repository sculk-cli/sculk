package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.option
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import tech.jamalam.ctx
import tech.jamalam.pack.*
import tech.jamalam.util.*

class AddByUrl :
    CliktCommand(name = "url", help = "Add a project to the manifest from a direct download URL") {
    private val slug by option()
        .prettyPrompt<String>("Enter project name")
    private val url by option()
        .prettyPrompt<Url>("Enter download URL")
    private val filename by option().prettyPrompt<String>("Enter file name")
    private val type by option().prettyPrompt<Type>("Select type")
    private val side by option().prettyPrompt<Side>("Select side")

    override fun run() = runBlocking {
        val transformedSlug = slug.lowercase().replace(" ", "-")
        val pack = InMemoryPack(ctx.json, terminal = terminal)
        val tempFile = downloadFileTemp(url)
        val contents = tempFile.readBytes()

        val dir = when (type) {
            Type.Mod -> "mods"
            Type.Shaderpack -> "shaderpacks"
            Type.Resourcepack -> "resourcepacks"
            Type.Datapack -> "datapacks"
        }

        val existingManifest = pack.getManifest("$dir/$transformedSlug.sculk.json")
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
                    sha512 = contents.digestSha512(),
                    murmur2 = contents.digestMurmur2()
                ),
                fileSize = contents.size,
                sources = FileManifestSources(
                    curseforge = null,
                    modrinth = null,
                    url = FileManifestUrlSource(url.toString())
                )
            )
        }

        pack.setManifest("$dir/$transformedSlug.sculk.json", fileManifest)
        pack.save(ctx.json)
        terminal.info("Added $transformedSlug to manifest")
    }

    enum class Type {
        Mod,
        Shaderpack,
        Resourcepack,
        Datapack,
    }
}
