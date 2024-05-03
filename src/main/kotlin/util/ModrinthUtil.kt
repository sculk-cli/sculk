package tech.jamalam.util

import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import tech.jamalam.PrettyListPrompt
import tech.jamalam.ctx
import tech.jamalam.modrinth.ModrinthPackFileEnv
import tech.jamalam.modrinth.models.ModrinthEnvSupport
import tech.jamalam.modrinth.models.ModrinthModLoader
import tech.jamalam.modrinth.models.ModrinthProject
import tech.jamalam.modrinth.models.ModrinthVersionDependencyType
import tech.jamalam.pack.*

suspend fun addModrinthMod(
    pack: InMemoryPack,
    dependencyGraph: DependencyGraph,
    query: String,
    terminal: Terminal,
) {
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

    addProject(
        terminal,
        pack,
        dependencyGraph,
        directMatch ?: ctx.modrinthApi.getProject(projectSlug)!!,
        false
    )
}

private suspend fun addProject(
    terminal: Terminal,
    pack: InMemoryPack,
    dependencyGraph: DependencyGraph,
    project: ModrinthProject,
    ignoreIfExists: Boolean = true,
): Boolean {
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
                return false
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

                if (addProject(
                        terminal,
                        pack,
                        dependencyGraph,
                        dependencyProject
                    ) || dependencyGraph.containsKey("mods/${dependencyProject.slug}.sculk.json")
                ) {
                    dependencyGraph.addDependency(
                        "mods/${dependencyProject.slug}.sculk.json",
                        "mods/${project.slug}.sculk.json"
                    )
                }
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

                    addProject(terminal, pack, dependencyGraph, dependencyProject)
                }
            }

            else -> {}
        }
    }

    return true
}

fun modrinthEnvTypePairToSide(clientSide: ModrinthEnvSupport, serverSide: ModrinthEnvSupport) =
    when (clientSide to serverSide) {
        ModrinthEnvSupport.Unsupported to ModrinthEnvSupport.Required -> Side.ServerOnly
        ModrinthEnvSupport.Unsupported to ModrinthEnvSupport.Optional -> Side.ClientOnly
        ModrinthEnvSupport.Required to ModrinthEnvSupport.Unsupported -> Side.ClientOnly
        ModrinthEnvSupport.Optional to ModrinthEnvSupport.Unsupported -> Side.ServerOnly
        else -> Side.Both
    }

fun ModrinthPackFileEnv.toSide() = modrinthEnvTypePairToSide(clientSupport, serverSupport)

fun Side.toModrinthEnvServerSupport() = when (this) {
    Side.ServerOnly -> ModrinthEnvSupport.Required
    Side.ClientOnly -> ModrinthEnvSupport.Unsupported
    Side.Both -> ModrinthEnvSupport.Required
}

fun Side.toModrinthEnvClientSupport() = when (this) {
    Side.ServerOnly -> ModrinthEnvSupport.Unsupported
    Side.ClientOnly -> ModrinthEnvSupport.Required
    Side.Both -> ModrinthEnvSupport.Required
}

fun ModrinthModLoader.toModLoader(): ModLoader = when (this) {
    ModrinthModLoader.NeoForge -> ModLoader.Neoforge
    ModrinthModLoader.Fabric -> ModLoader.Fabric
    ModrinthModLoader.Forge -> ModLoader.Forge
    ModrinthModLoader.Quilt -> ModLoader.Quilt
    else -> error("Unknown Modrinth mod loader: $this")
}

fun ModLoader.toModrinth(): ModrinthModLoader = when (this) {
    ModLoader.Neoforge -> ModrinthModLoader.NeoForge
    ModLoader.Fabric -> ModrinthModLoader.Fabric
    ModLoader.Forge -> ModrinthModLoader.Forge
    ModLoader.Quilt -> ModrinthModLoader.Quilt
}
