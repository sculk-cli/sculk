package io.github.sculk_cli.util

import com.github.ajalt.mordant.terminal.info
import com.github.ajalt.mordant.terminal.warning
import io.github.sculk_cli.curseforge.CURSEFORGE_MODS_CLASS
import io.github.sculk_cli.curseforge.models.*
import io.github.sculk_cli.Context
import io.github.sculk_cli.PrettyListPrompt
import io.github.sculk_cli.pack.FileManifest
import io.github.sculk_cli.pack.FileManifestCurseforgeSource
import io.github.sculk_cli.pack.FileManifestHashes
import io.github.sculk_cli.pack.FileManifestSources
import io.github.sculk_cli.pack.ModLoader
import io.github.sculk_cli.pack.Side
import io.github.sculk_cli.pack.addDependency

suspend fun findAndAddCurseforgeProject(
	ctx: Context,
	query: String,
	ignoreIfExists: Boolean = false,
	skipDependencies: Boolean = false,
) {
    val directMatches = ctx.curseforge.search(
        slug = query,
        gameVersion = ctx.pack.getManifest().minecraft,
        classId = CURSEFORGE_MODS_CLASS
    )

    val mod = if (directMatches.isNotEmpty()) {
        if (directMatches.size > 1) {
            PrettyListPrompt("Select a project", directMatches.map { it.name }, ctx.terminal).ask()
                .let { directMatches.find { p -> p.name == it } }!!
        } else {
            directMatches[0]
        }
    } else {
        val mods = ctx.curseforge.search(
            searchFilter = query,
            gameVersion = ctx.pack.getManifest().minecraft,
            classId = CURSEFORGE_MODS_CLASS
        )

        if (mods.isEmpty()) {
            error("No projects found")
        }

        PrettyListPrompt("Select a project", mods.map { it.name }, ctx.terminal).ask()
            .let { mods.find { p -> p.name == it } }!!
    }

    addCurseforgeProject(ctx, mod, ignoreIfExists, skipDependencies)
}

suspend fun addCurseforgeProject(
	ctx: Context,
	mod: CurseforgeMod,
	ignoreIfExists: Boolean = true,
	skipDependencies: Boolean = false,
): Boolean {
    if (mod.allowModDistribution == false) {
        ctx.terminal.warning("${mod.name} does not allow distribution")
        return false
    }

    val files =
	    ctx.curseforge.getModFiles(
		    modId = mod.id,
		    modLoader = ctx.pack.getManifest().loader.type.toCurseforge(),
		    gameVersion = ctx.pack.getManifest().minecraft
	    ).sortedBy {
		    it.fileDate
	    }.reversed().toMutableList()
    
    if (ctx.pack.getManifest().loader.type == ModLoader.Neoforge && ctx.pack.getManifest().minecraft == "1.20.1") {
        files += ctx.curseforge.getModFiles(
            modId = mod.id,
            modLoader = CurseforgeModLoader.Forge,
            gameVersion = ctx.pack.getManifest().minecraft
        ).sortedBy {
            it.fileDate
        }.reversed()
    }
    
    if (files.isEmpty()) {
        error("No files found for ${mod.name}")
    }

    val file = files[0] // Most recent version
    return addCurseforgeFile(ctx, mod, file, ignoreIfExists = ignoreIfExists, downloadDependencies = !skipDependencies)
}

suspend fun addCurseforgeFile(
	ctx: Context,
	mod: CurseforgeMod,
	file: CurseforgeFile,
	manifestPath: String? = null,
	ignoreIfExists: Boolean = true,
	downloadDependencies: Boolean = true,
): Boolean {
    if (file.downloadUrl == null) {
        ctx.terminal.warning("No download URL for ${file.fileName}")
        return false
    }

    val dir = getClassIdDir(mod.classId ?: 6)
    val tempFile = downloadFileTemp(parseUrl(file.downloadUrl!!)).readBytes()
    val sha1 = tempFile.digestSha1()
    val sha512 = tempFile.digestSha512()
    val murmur2 = tempFile.digestMurmur2()

    val path = manifestPath ?: "$dir/${mod.slug}.sculk.json"

    val existingManifest = ctx.pack.getManifest(path)
    val fileManifest = if (existingManifest != null) {
        if (existingManifest.sources.curseforge != null) {
            if (ignoreIfExists) {
                return false
            }

            error("Existing manifest already has a Curseforge source (did you mean to use the update command?)")
        }

        existingManifest.sources.curseforge = FileManifestCurseforgeSource(
	        projectId = mod.id, fileUrl = file.downloadUrl!!, fileId = file.id, hashes = FileManifestHashes(sha1, sha512, murmur2)
        )

        existingManifest
    } else {
	    FileManifest(
		    filename = file.fileName,
		    fileSize = tempFile.size,
		    side = file.getSide().toSide(),
		    sources = FileManifestSources(
			    curseforge = FileManifestCurseforgeSource(
				    projectId = mod.id, fileUrl = file.downloadUrl!!, fileId = file.id, hashes = FileManifestHashes(sha1, sha512, murmur2)
			    ), modrinth = null, url = null
		    )
	    )
    }

    ctx.pack.setManifest(path, fileManifest)
    ctx.terminal.info("Added ${mod.name} to manifest")

    if (dir == "mods" && downloadDependencies) {
        for (dependency in file.dependencies) {
            when (dependency.relationType) {
                CurseforgeFileRelationType.RequiredDependency -> {
                    val dependencyMod = ctx.curseforge.getMod(dependency.modId)
                        ?: error("Dependency not found")

                    if (addCurseforgeProject(
                            ctx,
                            dependencyMod
                        ) || ctx.dependencyGraph.containsKey("$dir/${dependencyMod.slug}.sculk.json")
                    ) {
                        ctx.dependencyGraph.addDependency(
                            "$dir/${dependencyMod.slug}.sculk.json",
                            path
                        )
                    }
                }

                CurseforgeFileRelationType.OptionalDependency -> {
                    val dependencyMod = ctx.curseforge.getMod(dependency.modId)
                        ?: error("Dependency not found")
                    val prompt = PrettyListPrompt(
	                    "Add optional dependency ${dependencyMod.name}?",
	                    listOf("Yes", "No"),
	                    ctx.terminal
                    )

                    if (prompt.ask() == "Yes") {
                        if (addCurseforgeProject(
                                ctx,
                                dependencyMod
                            ) || ctx.dependencyGraph.containsKey("$dir/${dependencyMod.slug}.sculk.json")
                        ) {
                            ctx.dependencyGraph.addDependency(
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
	ctx: Context,
	manifest: FileManifest,
): Boolean {
    if (manifest.sources.curseforge == null) {
        return false
    }

    val mod = ctx.curseforge.getMod(manifest.sources.curseforge!!.projectId)
        ?: error("Project not found")

    val files =
	    ctx.curseforge.getModFiles(
		    modId = mod.id,
		    modLoader = ctx.pack.getManifest().loader.type.toCurseforge(),
		    gameVersion = ctx.pack.getManifest().minecraft
	    ).sortedBy {
		    it.fileDate
	    }.reversed().toMutableList()

    if (ctx.pack.getManifest().loader.type == ModLoader.Neoforge && ctx.pack.getManifest().minecraft == "1.20.1") {
        files += ctx.curseforge.getModFiles(
            modId = mod.id,
            modLoader = CurseforgeModLoader.Forge,
            gameVersion = ctx.pack.getManifest().minecraft
        ).sortedBy {
            it.fileDate
        }.reversed()
    }

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
    manifest.fileSize = tempFile.size
    manifest.filename = file.fileName

    manifest.sources.curseforge = FileManifestCurseforgeSource(
	    projectId = mod.id, fileUrl = file.downloadUrl!!, fileId = file.id, hashes = FileManifestHashes(tempFile.digestSha1(), tempFile.digestSha512(), tempFile.digestMurmur2())
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

fun getClassIdDir(classId: Int): String = when (classId) {
    6552 -> "shaderpacks"
    6 -> "mods"
    6945 -> "datapacks"
    12 -> "resourcepacks"
    else -> error("Unsupported class ID $classId")
}
