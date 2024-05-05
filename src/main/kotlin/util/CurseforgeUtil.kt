package tech.jamalam.util

import com.github.ajalt.mordant.terminal.Terminal
import tech.jamalam.PrettyListPrompt
import tech.jamalam.ctx
import tech.jamalam.curseforge.CURSEFORGE_MODS_CLASS
import tech.jamalam.curseforge.models.*
import tech.jamalam.pack.*

suspend fun findAndAddCurseforgeProject(
    pack: InMemoryPack,
    dependencyGraph: DependencyGraph,
    query: String,
    terminal: Terminal,
) {
    val directMatches = ctx.curseforgeApi.search(
        slug = query,
        gameVersion = pack.getManifest().minecraft,
        classId = CURSEFORGE_MODS_CLASS
    )

    val mod = if (directMatches.isNotEmpty()) {
        if (directMatches.size > 1) {
            PrettyListPrompt("Select a project", directMatches.map { it.name }, terminal).ask()
                .let { directMatches.find { p -> p.name == it } }!!
        } else {
            directMatches[0]
        }
    } else {
        val mods = ctx.curseforgeApi.search(
            searchFilter = query,
            gameVersion = pack.getManifest().minecraft,
            classId = CURSEFORGE_MODS_CLASS
        )

        if (mods.isEmpty()) {
            error("No projects found")
        }

        PrettyListPrompt("Select a project", mods.map { it.name }, terminal).ask()
            .let { mods.find { p -> p.name == it } }!!
    }

    addCurseforgeProject(terminal, pack, dependencyGraph, mod, false)
}

suspend fun addCurseforgeProject(
    terminal: Terminal,
    pack: InMemoryPack,
    dependencyGraph: DependencyGraph,
    mod: CurseforgeMod,
    ignoreIfExists: Boolean = true,
): Boolean {
    if (mod.allowModDistribution == false) {
        terminal.warning("${mod.name} does not allow distribution")
        return false
    }

    val files =
        ctx.curseforgeApi.getModFiles(
            modId = mod.id,
            modLoader = pack.getManifest().loader.type.toCurseforge(),
            gameVersion = pack.getManifest().minecraft
        ).sortedBy {
            it.fileDate
        }.reversed()

    if (files.isEmpty()) {
        error("No files found for ${mod.name}")
    }

    val file = files[0] // Most recent version
    return addCurseforgeFile(terminal, pack, dependencyGraph, mod, file, ignoreIfExists = ignoreIfExists)
}

suspend fun addCurseforgeFile(
    terminal: Terminal,
    pack: InMemoryPack,
    dependencyGraph: DependencyGraph,
    mod: CurseforgeMod,
    file: CurseforgeFile,
    manifestPath: String? = null,
    ignoreIfExists: Boolean = true,
    downloadDependencies: Boolean = true,
): Boolean {
    if (file.downloadUrl == null) {
        terminal.warning("No download URL for ${file.fileName}")
        return false
    }

    val dir = when (mod.classId) {
        6552 -> "shaderpacks"
        6 -> "mods"
        6945 -> "datapacks"
        12 -> "resourcepacks"
        else -> error("Unsupported class ID ${mod.classId}")
    }
    val tempFile = downloadFileTemp(parseUrl(file.downloadUrl!!)).readBytes()
    val sha1 = tempFile.digestSha1()
    val sha512 = tempFile.digestSha512()
    val murmur2 = tempFile.digestMurmur2()

    val path = manifestPath ?: "$dir/${mod.slug}.sculk.json"

    val existingManifest = pack.getManifest(path)
    val fileManifest = if (existingManifest != null) {
        if (existingManifest.sources.curseforge != null) {
            if (ignoreIfExists) {
                return false
            }

            error("Existing manifest already has a Curseforge source (did you mean to use the update command?)")
        }

        if (existingManifest.hashes.sha1 != sha1 || existingManifest.hashes.sha512 != sha512) {
            error("File hashes do not match for ${file.fileName}")
        }

        existingManifest.sources.curseforge = FileManifestCurseforgeSource(
            projectId = mod.id, fileUrl = file.downloadUrl!!, fileId = file.id
        )

        existingManifest
    } else {
        FileManifest(
            filename = file.fileName, hashes = FileManifestHashes(
                sha1 = sha1, sha512 = sha512, murmur2 = murmur2
            ),
            fileSize = tempFile.size,
            side = file.getSide().toSide(),
            sources = FileManifestSources(
                curseforge = FileManifestCurseforgeSource(
                    projectId = mod.id, fileUrl = file.downloadUrl!!, fileId = file.id
                ), modrinth = null, url = null
            )
        )
    }

    pack.setManifest(path, fileManifest)
    terminal.info("Added ${mod.name} to manifest")

    if (dir == "mods" && downloadDependencies) {
        for (dependency in file.dependencies) {
            when (dependency.relationType) {
                CurseforgeFileRelationType.RequiredDependency -> {
                    val dependencyMod = ctx.curseforgeApi.getMod(dependency.modId)
                        ?: error("Dependency not found")

                    if (addCurseforgeProject(
                            terminal,
                            pack,
                            dependencyGraph,
                            dependencyMod
                        ) || dependencyGraph.containsKey("$dir/${dependencyMod.slug}.sculk.json")
                    ) {
                        dependencyGraph.addDependency(
                            "$dir/${dependencyMod.slug}.sculk.json",
                            path
                        )
                    }
                }

                CurseforgeFileRelationType.OptionalDependency -> {
                    val dependencyMod = ctx.curseforgeApi.getMod(dependency.modId)
                        ?: error("Dependency not found")
                    val prompt = PrettyListPrompt(
                        "Add optional dependency ${dependencyMod.name}?",
                        listOf("Yes", "No"),
                        terminal
                    )

                    if (prompt.ask() == "Yes") {
                        if (addCurseforgeProject(
                                terminal,
                                pack,
                                dependencyGraph,
                                dependencyMod
                            ) || dependencyGraph.containsKey("$dir/${dependencyMod.slug}.sculk.json")
                        ) {
                            dependencyGraph.addDependency(
                                "$dir/${dependencyMod.slug}.sculk.json",
                                path
                            )
                        }
                    }
                }

                else -> {}
            }
        }
    }

    return true
}

suspend fun updateCurseforgeProject(
    pack: InMemoryPack,
    manifest: FileManifest,
): Boolean {
    if (manifest.sources.curseforge == null) {
        return false
    }

    val mod = ctx.curseforgeApi.getMod(manifest.sources.curseforge!!.projectId)
        ?: error("Project not found")

    val files =
        ctx.curseforgeApi.getModFiles(
            modId = mod.id,
            modLoader = pack.getManifest().loader.type.toCurseforge(),
            gameVersion = pack.getManifest().minecraft
        ).sortedBy {
            it.fileDate
        }.reversed()

    if (files.isEmpty()) {
        error("No files found for ${mod.name}")
    }

    val file = files[0] // Most recent version

    if (file.id == manifest.sources.curseforge!!.fileId) {
        return false
    }

    if (file.downloadUrl == null) {
        error("No download URL for ${file.fileName}")
    }

    val tempFile = downloadFileTemp(parseUrl(file.downloadUrl!!)).readBytes()
    manifest.hashes.sha1 = tempFile.digestSha1()
    manifest.hashes.sha512 = tempFile.digestSha512()
    manifest.fileSize = tempFile.size
    manifest.filename = file.fileName

    manifest.sources.curseforge = FileManifestCurseforgeSource(
        projectId = mod.id, fileUrl = file.downloadUrl!!, fileId = file.id
    )

    return true
}

fun ModLoader.toCurseforge(): CurseforgeModLoader = when (this) {
    ModLoader.Neoforge -> CurseforgeModLoader.Neoforge
    ModLoader.Fabric -> CurseforgeModLoader.Fabric
    ModLoader.Forge -> CurseforgeModLoader.Forge
    ModLoader.Quilt -> CurseforgeModLoader.Quilt
}

fun CurseforgeSide.toSide(): Side = when (this) {
    CurseforgeSide.Client -> Side.ClientOnly
    CurseforgeSide.Server -> Side.ServerOnly
    CurseforgeSide.Both -> Side.Both
}
