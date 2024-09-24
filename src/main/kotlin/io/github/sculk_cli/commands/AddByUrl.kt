package io.github.sculk_cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import io.github.sculk_cli.Context
import io.github.sculk_cli.pack.FileManifest
import io.github.sculk_cli.pack.FileManifestHashes
import io.github.sculk_cli.pack.FileManifestSources
import io.github.sculk_cli.pack.FileManifestUrlSource
import io.github.sculk_cli.pack.Side
import io.github.sculk_cli.util.digestMurmur2
import io.github.sculk_cli.util.digestSha1
import io.github.sculk_cli.util.digestSha512
import io.github.sculk_cli.util.downloadFileTemp
import io.github.sculk_cli.util.prettyPrompt

class AddByUrl :
    CliktCommand(name = "url", help = "Add a project to the manifest from a direct download URL") {
    private val slug by option().prettyPrompt<String>("Enter project name")
        .help("The slug of the project, used for naming the file manifest")
    private val url by option().prettyPrompt<Url>("Enter download URL")
        .help("The download URL of the file")
    private val filename by option().prettyPrompt<String>("Enter file name")
        .help("The name of the file (e.g. sodium-1.2.3.jar")
    private val type by option().prettyPrompt<Type>("Select type").help("The project type")
    private val side by option().prettyPrompt<Side>("Select side")
        .help("The side the project is for")

    override fun run() = runBlocking {
        val transformedSlug = slug.lowercase().replace(" ", "-")
        val ctx = Context.Companion.getOrCreate(terminal)
        val tempFile = downloadFileTemp(url)
        val contents = tempFile.readBytes()

        val dir = when (type) {
            Type.Mod -> "mods"
            Type.Shaderpack -> "shaderpacks"
            Type.Resourcepack -> "resourcepacks"
            Type.Datapack -> "datapacks"
        }

        val existingManifest = ctx.pack.getManifest("$dir/$transformedSlug.sculk.json")
        val fileManifest = if (existingManifest != null) {
            if (existingManifest.hashes.sha1 != contents.digestSha1() || existingManifest.hashes.sha512 != contents.digestSha512()) {
                error("File hashes do not match for $filename")
            }

            existingManifest.sources.url = FileManifestUrlSource(url.toString())
            existingManifest
        } else {
	        FileManifest(
		        filename = filename, side = side, hashes = FileManifestHashes(
			        sha1 = contents.digestSha1(),
			        sha512 = contents.digestSha512(),
			        murmur2 = contents.digestMurmur2()
		        ), fileSize = contents.size, sources = FileManifestSources(
			        curseforge = null, modrinth = null, url = FileManifestUrlSource(url.toString())
		        )
	        )
        }

        ctx.pack.setManifest("$dir/$transformedSlug.sculk.json", fileManifest)
        ctx.pack.save(ctx.json)
        terminal.info("Added $transformedSlug to manifest")
    }

    enum class Type {
        Mod, Shaderpack, Resourcepack, Datapack,
    }
}
