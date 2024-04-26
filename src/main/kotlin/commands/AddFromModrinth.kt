package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import kotlinx.coroutines.runBlocking
import tech.jamalam.PrettyListPrompt
import tech.jamalam.ctx
import tech.jamalam.modrinth.models.ModrinthModLoader
import tech.jamalam.modrinth.models.ModrinthProject
import tech.jamalam.modrinth.models.ModrinthVersionDependencyType
import tech.jamalam.pack.*
import tech.jamalam.util.downloadFileTemp
import tech.jamalam.util.modrinthEnvTypePairToSide
import tech.jamalam.util.parseUrl
import tech.jamalam.util.toModrinth

class AddFromModrinth : CliktCommand(name = "modrinth") {
    private val query by argument()

    override fun run() = runBlocking {
        val pack = InMemoryPack(ctx.json)
        val dependencyGraph = loadDependencyGraph()
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

        addProject(pack, dependencyGraph, directMatch ?: ctx.modrinthApi.getProject(projectSlug)!!, false)
        dependencyGraph.save()
        pack.save(ctx.json)
    }

    private suspend fun addProject(
        pack: InMemoryPack,
        dependencyGraph: DependencyGraph,
        project: ModrinthProject,
        ignoreIfExists: Boolean = true,
    ) {
        val versions = runBlocking {
            ctx.modrinthApi.getProjectVersions(
                project.slug,
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

        val version = versions.elementAtOrNull(0)
            ?: error("No valid versions found for ${project.title} (Minecraft: ${pack.getManifest().minecraft}, loader: ${pack.getManifest().loader.type})")
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
                if (ignoreIfExists) {
                    return
                }

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

        pack.setManifest("$dir/${project.slug}.sculk.json", fileManifest)
        terminal.info("Added ${project.title} to manifest")

        for (dependency in version.dependencies) {
            when (dependency.type) {
                ModrinthVersionDependencyType.Required -> {
                    val dependencyProject = ctx.modrinthApi.getProject(dependency.projectId)
                        ?: error("Dependency not found")

                    dependencyGraph.addDependency(
                        "mods/${dependencyProject.slug}.sculk.json",
                        "mods/${project.slug}.sculk.json"
                    )

                    addProject(pack, dependencyGraph, dependencyProject)
                }

                ModrinthVersionDependencyType.Optional -> {
                    val dependencyProject = ctx.modrinthApi.getProject(dependency.projectId)
                        ?: error("Dependency not found")
                    val prompt = PrettyListPrompt(
                        "Add optional dependency ${dependencyProject.title}?",
                        listOf("Yes", "No"),
                        terminal
                    )

                    if (prompt.ask() == "Yes") {
                        dependencyGraph.addDependency(
                            "mods/${dependencyProject.slug}.sculk.json",
                            "mods/${project.slug}.sculk.json"
                        )

                        addProject(pack, dependencyGraph, dependencyProject)
                    }
                }

                else -> {}
            }
        }
    }
}
