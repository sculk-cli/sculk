package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import kotlinx.coroutines.runBlocking
import tech.jamalam.PrettyListPrompt
import tech.jamalam.ctx
import tech.jamalam.downloadFileTemp
import tech.jamalam.modrinth.models.ModrinthModLoader
import tech.jamalam.pack.*
import tech.jamalam.parseUrl
import tech.jamalam.util.modrinthEnvTypePairToSide
import tech.jamalam.util.toModrinth

class AddFromModrinth : CliktCommand(name = "modrinth") {
    private val query by argument()

    override fun run() = runBlocking {
        val pack = InMemoryPack(ctx.json)
        val directMatch = ctx.modrinthApi.getProject(query)

        val projectSlug = directMatch?.slug ?: run {
            val projects =
                ctx.modrinthApi.search(
                    query,
                    loaders = listOf(
                        pack.getManifest().loader.type.toModrinth(),
                        ModrinthModLoader.Minecraft
                    ),
                    gameVersions = listOf(pack.getManifest().minecraft)
                ).hits

            if (projects.isEmpty()) {
                error("No projects found")
            }

            PrettyListPrompt("Select a project", projects.map { it.title }, terminal).ask()
                .let { projects.find { p -> p.title == it } }!!.slug
        }

        val project = directMatch ?: ctx.modrinthApi.getProject(projectSlug)!!

        val versions = runBlocking {
            ctx.modrinthApi.getProjectVersions(
                projectSlug,
                loaders = listOf(
                    pack.getManifest().loader.type.toModrinth(),
                    ModrinthModLoader.Minecraft,
                    *if (pack.getManifest().loader.type == ModLoader.Quilt) {
                        arrayOf(ModrinthModLoader.Fabric)
                    } else {
                        emptyArray()
                    }
                ),
                gameVersions = listOf(pack.getManifest().minecraft)
            )
        }.sortedBy {
            it.publishedTime
        }.reversed()

        val version = versions.elementAtOrNull(0) ?: error("No valid versions found for ${project.title} (Minecraft: ${pack.getManifest().minecraft}, loader: ${pack.getManifest().loader.type})")
        val modrinthFile = version.files.first { it.primary }

        val tempFile = runBlocking { downloadFileTemp(parseUrl(modrinthFile.downloadUrl)) }

        val dir = if (version.loaders.contains("minecraft")) {
            TODO()
        } else {
            "mods"
        }

        val existingManifest = pack.getManifest("$dir/${project.slug}.sculk.json")
        val fileManifest = if (existingManifest != null) {
            if (existingManifest.sources.modrinth != null) {
                error("Existing manifest already has a Modrinth source (did you mean to use the update command?)")
            }

            if (existingManifest.hashes.sha1 != modrinthFile.hashes.sha1 || existingManifest.hashes.sha512 != modrinthFile.hashes.sha512) {
                error("File hashes do not match for ${modrinthFile.filename}")
            }

            existingManifest.sources.modrinth = FileManifestModrinthSource(
                projectId = project.id, fileUrl = modrinthFile.downloadUrl
            )

            existingManifest
        } else {
            FileManifest(
                filename = modrinthFile.filename, hashes = FileManifestHashes(
                    sha1 = modrinthFile.hashes.sha1, sha512 = modrinthFile.hashes.sha512
                ),
                fileSize = tempFile.readBytes().size,
                side = modrinthEnvTypePairToSide(
                    project.clientSideSupport,
                    project.serverSideSupport
                ),
                sources = FileManifestSources(
                    curseforge = null, modrinth = FileManifestModrinthSource(
                        projectId = project.id, fileUrl = modrinthFile.downloadUrl
                    ), url = null
                )
            )
        }

        pack.setManifest("$dir/${projectSlug}.sculk.json", fileManifest)
        pack.save(ctx.json)
        terminal.info("Added ${project.title} to manifest")
    }
}
