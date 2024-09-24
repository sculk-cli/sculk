package tech.jamalam.util

import kotlinx.coroutines.runBlocking
import tech.jamalam.Context
import tech.jamalam.PrettyListPrompt
import tech.jamalam.modrinth.ModrinthPackFileEnv
import tech.jamalam.modrinth.models.*
import tech.jamalam.pack.*

private val MODRINTH_DEFAULT_LOADERS = listOf(
    ModrinthLoader.Optifine,
    ModrinthLoader.Iris,
    ModrinthLoader.Datapack,
    ModrinthLoader.Minecraft // resource packs
).toTypedArray()

suspend fun addModrinthProject(
    ctx: Context,
    query: String,
    ignoreIfExists: Boolean = false,
    skipDependencies: Boolean = false
) {
    val directMatch = ctx.modrinth.getProject(query)

    val projectSlug = directMatch?.slug ?: run {
        val projects = ctx.modrinth.search(
            query,
            loaders = getAllLoaders(ctx.pack.getManifest().loader.type, ctx.pack.getManifest().minecraft),
            gameVersions = listOf(ctx.pack.getManifest().minecraft)
        ).hits

        if (projects.isEmpty()) {
            error("No projects found")
        }

        PrettyListPrompt("Select a project", projects.map { it.title }, ctx.terminal).ask()
            .let { projects.find { p -> p.title == it } }!!.slug
    }

    addModrinthProject(
        ctx, directMatch ?: ctx.modrinth.getProject(projectSlug)!!, ignoreIfExists, skipDependencies
    )
}

private suspend fun addModrinthProject(
    ctx: Context,
    project: ModrinthProject,
    ignoreIfExists: Boolean = true,
    skipDependencies: Boolean = false
): Boolean {
    val versions = runBlocking {
        ctx.modrinth.getProjectVersions(
            project.slug,
            loaders = getAllLoaders(ctx.pack.getManifest().loader.type, ctx.pack.getManifest().minecraft),
            gameVersions = listOf(ctx.pack.getManifest().minecraft)
        )
    }.sortedBy {
        it.publishedTime
    }.reversed()

    // Prefer mod versions over datapack versions
    val nonDatapackVersions = versions.filter { it.loaders.none { loader -> loader == ModrinthLoader.Datapack } }
    val version = if (nonDatapackVersions.isNotEmpty()) {
        nonDatapackVersions[0]
    } else {
        versions.elementAtOrNull(0)
    } ?: error("No valid versions found for ${project.title} (Minecraft: ${ctx.pack.getManifest().minecraft}, loader: ${ctx.pack.getManifest().loader.type})")

    return addModrinthVersion(ctx, project, version, ignoreIfExists = ignoreIfExists, downloadDependencies = !skipDependencies)
}

suspend fun addModrinthVersion(
    ctx: Context,
    project: ModrinthProject,
    version: ModrinthVersion,
    manifestPath: String? = null,
    ignoreIfExists: Boolean = true,
    downloadDependencies: Boolean = true
): Boolean {
    val modrinthFile = version.files.first { it.primary }
    val tempFile = downloadFileTemp(parseUrl(modrinthFile.downloadUrl))
    val dir = version.loaders.first().getSaveDir()

    val path = manifestPath ?: "$dir/${project.slug}.sculk.json"
    val existingManifest = ctx.pack.getManifest(path)
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
                sha1 = modrinthFile.hashes.sha1,
                sha512 = modrinthFile.hashes.sha512,
                murmur2 = tempFile.readBytes().digestMurmur2()
            ), fileSize = tempFile.readBytes().size, side = modrinthEnvTypePairToSide(
                project.clientSideSupport, project.serverSideSupport
            ), sources = FileManifestSources(
                curseforge = null, modrinth = FileManifestModrinthSource(
                    projectId = project.id, fileUrl = modrinthFile.downloadUrl
                ), url = null
            )
        )
    }

    ctx.pack.setManifest(path, fileManifest)
    ctx.terminal.info("Added ${project.title} to manifest")

    if (dir == "mods" && downloadDependencies) { // only supporting mod dependencies for now
        for (dependency in version.dependencies) {
            when (dependency.type) {
                ModrinthVersionDependencyType.Required -> {
                    val dependencyProject = ctx.modrinth.getProject(dependency.projectId)
                        ?: error("Dependency not found")

                    if (ctx.pack.getManifest("$dir/${dependencyProject.slug}.sculk.json") != null) {
                        continue
                    }

                    if (addModrinthProject(
                            ctx, dependencyProject
                        ) || ctx.dependencyGraph.containsKey("$dir/${dependencyProject.slug}.sculk.json")
                    ) {
                        ctx.dependencyGraph.addDependency(
                            "$dir/${dependencyProject.slug}.sculk.json", path
                        )
                    }
                }

                ModrinthVersionDependencyType.Optional -> {
                    val dependencyProject = ctx.modrinth.getProject(dependency.projectId)
                        ?: error("Dependency not found")

                    if (ctx.pack.getManifest("$dir/${dependencyProject.slug}.sculk.json") != null) {
                        continue
                    }

                    val prompt = PrettyListPrompt(
                        "Add optional dependency ${dependencyProject.title}?",
                        listOf("Yes", "No"),
                        ctx.terminal
                    )

                    if (prompt.ask() == "Yes") {
                        addModrinthProject(ctx, dependencyProject)
                    }
                }

                else -> {}
            }
        }
    }

    return true
}

suspend fun updateModrinthProject(
    ctx: Context,
    manifest: FileManifest,
): Boolean {
    if (manifest.sources.modrinth == null) {
        return false
    }

    val mod =
        ctx.modrinth.getProject(manifest.sources.modrinth!!.projectId) ?: error("Project not found")

    val versions = ctx.modrinth.getProjectVersions(
        idOrSlug = mod.id,
        loaders = getAllLoaders(ctx.pack.getManifest().loader.type, ctx.pack.getManifest().minecraft),
        gameVersions = listOf(ctx.pack.getManifest().minecraft)
    ).sortedBy {
        it.publishedTime
    }.reversed()

    if (versions.isEmpty()) {
        error("No versions found for ${mod.title}")
    }

    val version = versions[0] // Most recent version
    val file = version.files.first { it.primary }

    if (file.downloadUrl == manifest.sources.modrinth!!.fileUrl) {
        return false
    }

    val tempFile = downloadFileTemp(parseUrl(file.downloadUrl)).readBytes()
    manifest.hashes.sha1 = tempFile.digestSha1()
    manifest.hashes.sha512 = tempFile.digestSha512()
    manifest.fileSize = tempFile.size
    manifest.filename = file.filename

    manifest.sources.modrinth = FileManifestModrinthSource(
        projectId = mod.id, fileUrl = file.downloadUrl
    )

    return true
}

// Modrinth made a mistake with their V3 migration, the upshot being that we need to assume
// that all mods need to be loaded on both sides. Hopefully mods have their metadata set correctly
// so that they only load on the correct side.
// See https://discord.com/channels/734077874708938864/1103139849852162158/1251553321433432205
fun modrinthEnvTypePairToSide(clientSide: ModrinthEnvSupport, serverSide: ModrinthEnvSupport) =
    when (clientSide to serverSide) {
//        ModrinthEnvSupport.Unsupported to ModrinthEnvSupport.Required -> Side.ServerOnly
//        ModrinthEnvSupport.Unsupported to ModrinthEnvSupport.Optional -> Side.ClientOnly
//        ModrinthEnvSupport.Required to ModrinthEnvSupport.Unsupported -> Side.ClientOnly
//        ModrinthEnvSupport.Optional to ModrinthEnvSupport.Unsupported -> Side.ServerOnly
        else -> Side.Both
    }

fun ModrinthPackFileEnv.toSide() = modrinthEnvTypePairToSide(clientSupport, serverSupport)

fun Side.toModrinthEnvServerSupport() = when (this) {
//    Side.ServerOnly -> ModrinthEnvSupport.Required
//    Side.ClientOnly -> ModrinthEnvSupport.Unsupported
//    Side.Both -> ModrinthEnvSupport.Required
    else -> ModrinthEnvSupport.Required
}

fun Side.toModrinthEnvClientSupport() = when (this) {
//    Side.ServerOnly -> ModrinthEnvSupport.Unsupported
//    Side.ClientOnly -> ModrinthEnvSupport.Required
//    Side.Both -> ModrinthEnvSupport.Required
    else -> ModrinthEnvSupport.Required
}

fun ModrinthLoader.toModLoader(): ModLoader = when (this) {
    ModrinthLoader.NeoForge -> ModLoader.Neoforge
    ModrinthLoader.Fabric -> ModLoader.Fabric
    ModrinthLoader.Forge -> ModLoader.Forge
    ModrinthLoader.Quilt -> ModLoader.Quilt
    else -> error("Unsupported Modrinth mod loader: $this")
}

fun ModLoader.toModrinth(): ModrinthLoader = when (this) {
    ModLoader.Neoforge -> ModrinthLoader.NeoForge
    ModLoader.Fabric -> ModrinthLoader.Fabric
    ModLoader.Forge -> ModrinthLoader.Forge
    ModLoader.Quilt -> ModrinthLoader.Quilt
}

fun ModrinthLoader.getSaveDir(): String = when (this) {
    ModrinthLoader.Canvas -> "shaderpacks"
    ModrinthLoader.Datapack -> "datapacks"
    ModrinthLoader.Fabric -> "mods"
    ModrinthLoader.Forge -> "mods"
    ModrinthLoader.Iris -> "shaderpacks"
    ModrinthLoader.Minecraft -> "resourcepacks"
    ModrinthLoader.NeoForge -> "mods"
    ModrinthLoader.Optifine -> "shaderpacks"
    ModrinthLoader.Quilt -> "mods"
    ModrinthLoader.Vanilla -> "resourcepacks"
    else -> error("Unsupported Modrinth loader: $this")
}

fun getAllLoaders(loader: ModLoader, gameVersion: String): List<ModrinthLoader> = listOf(
    loader.toModrinth(),
    *if (loader == ModLoader.Quilt) {
        arrayOf(ModrinthLoader.Fabric)
    } else {
        emptyArray()
    },
    *if (loader == ModLoader.Neoforge && gameVersion == "1.20.1") {
        arrayOf(ModrinthLoader.Forge)
    } else {
        emptyArray()
    },
    *MODRINTH_DEFAULT_LOADERS
)
