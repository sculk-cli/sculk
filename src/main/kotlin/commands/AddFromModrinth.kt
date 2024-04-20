package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import kotlinx.coroutines.runBlocking
import tech.jamalam.PrettyListPrompt
import tech.jamalam.ctx
import tech.jamalam.downloadFileTemp
import tech.jamalam.pack.*
import tech.jamalam.parseUrl
import tech.jamalam.services.modrinthEnvTypePairToSide
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AddFromModrinth : CliktCommand(name = "modrinth") {
    private val query by argument()

    override fun run() = runBlocking {
        val pack = InMemoryPack(ctx.json)
        val directMatch = ctx.modrinth.getProject(query)

        val project = directMatch ?: run {
            val projects =
                ctx.modrinth.search(query)

            PrettyListPrompt("Select a project", projects.map { it.title }, terminal).ask()
                .let { projects.find { p -> p.title == it } }!!
        }

        val versions = runBlocking {
            ctx.modrinth.getValidVersions(
                project.slug, pack.getManifest().loader.type, pack.getManifest().minecraft
            )
        }.sortedBy {
            LocalDateTime.parse(
                it.publishedDate, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
            )
        }.reversed()

        val version = versions[0] // TODO: error handling
        val modrinthFile = version.files.first { it.primary }

        val tempFile = runBlocking { downloadFileTemp(parseUrl(modrinthFile.url)) }


        val dir = if (version.loaders.contains("minecraft")) {
            TODO()
        } else {
            "mods"
        }

        val existingManifest = pack.getFileManifest("$dir/${project.slug}.sculk.json")
        val fileManifest = if (existingManifest != null) {
            if (existingManifest.sources.modrinth != null) {
                error("Existing manifest already has a Modrinth source (did you mean to use the update command?)")
            }

            if (existingManifest.hashes.sha1 != modrinthFile.hashes.sha1 || existingManifest.hashes.sha512 != modrinthFile.hashes.sha512) {
                error("File hashes do not match for ${modrinthFile.filename}")
            }

            existingManifest.sources.modrinth = FileManifestModrinthSource(
                projectId = project.id, fileUrl = modrinthFile.url
            )

            existingManifest
        } else {
            FileManifest(
                filename = modrinthFile.filename, hashes = FileManifestHashes(
                    sha1 = modrinthFile.hashes.sha1, sha512 = modrinthFile.hashes.sha512
                ),
                fileSize = tempFile.readBytes().size,
                side = modrinthEnvTypePairToSide(project.clientSide, project.serverSide),
                sources = FileManifestSources(
                    curseforge = null, modrinth = FileManifestModrinthSource(
                        projectId = project.id, fileUrl = modrinthFile.url
                    ), url = null
                )
            )
        }

        pack.setFileManifest("$dir/${project.slug}.sculk.json", fileManifest)
        pack.save(ctx.json)
        terminal.info("Added ${project.title} to manifest")
    }
}
