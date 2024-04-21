package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import kotlinx.coroutines.runBlocking
import tech.jamalam.*
import tech.jamalam.pack.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AddFromCurseforge : CliktCommand(name = "curseforge") {
    private val query by argument()

    override fun run() = runBlocking {
        val pack = InMemoryPack(ctx.json)
        val directMatches = ctx.curseforge.searchBySlug(query, pack.getManifest().minecraft)

        val project = if (directMatches.isNotEmpty()) {
            if (directMatches.size > 1) {
                PrettyListPrompt("Select a project", directMatches.map { it.name }, terminal).ask()
                    .let { directMatches.find { p -> p.name == it } }!!
            } else {
                directMatches[0]
            }
        } else {
            val projects = ctx.curseforge.search(query, pack.getManifest().minecraft)

            PrettyListPrompt("Select a project", projects.map { it.name }, terminal).ask()
                .let { projects.find { p -> p.name == it } }!!
        }

        val files = runBlocking {
            ctx.curseforge.getValidVersions(
                project.id, pack.getManifest().loader.type, pack.getManifest().minecraft
            )
        }.sortedBy {
//            LocalDateTime.parse(
//                it.fileDate, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SS[S]'Z'")
//            )
            it.fileDate
        }.reversed()

        val file = files[0] // TODO: error handling
        val tempFile = runBlocking { downloadFileTemp(parseUrl(file.downloadUrl)).readBytes() }
        val sha1 = tempFile.digestSha1()
        val sha512 = tempFile.digestSha512()


        val existingManifest = pack.getFileManifest("mods/${project.slug}.sculk.json")
        val fileManifest = if (existingManifest != null) {
            if (existingManifest.sources.curseforge != null) {
                error("Existing manifest already has a Curseforge source (did you mean to use the update command?)")
            }

            if (existingManifest.hashes.sha1 != sha1 || existingManifest.hashes.sha512 != sha512) {
                error("File hashes do not match for ${file.fileName}")
            }

            existingManifest.sources.curseforge = FileManifestCurseforgeSource(
                projectId = project.id, fileUrl = file.downloadUrl, fileId = file.id
            )

            existingManifest
        } else {
            FileManifest(
                filename = file.fileName, hashes = FileManifestHashes(
                    sha1 = sha1, sha512 = sha512
                ),
                fileSize = tempFile.size,
                side = Side.Both,
                sources = FileManifestSources(
                    curseforge = FileManifestCurseforgeSource(
                        projectId = project.id, fileUrl = file.downloadUrl, fileId = file.id
                    ), modrinth = null, url = null
                )
            )
        }

        pack.setFileManifest("mods/${project.slug}.sculk.json", fileManifest)
        pack.save(ctx.json)
        terminal.info("Added ${project.name} to manifest")
    }
}
