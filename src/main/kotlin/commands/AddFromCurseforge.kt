package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import kotlinx.coroutines.runBlocking
import tech.jamalam.*
import tech.jamalam.curseforge.CURSEFORGE_MODS_CLASS
import tech.jamalam.curseforge.models.CurseforgeFileRelationType
import tech.jamalam.curseforge.models.CurseforgeMod
import tech.jamalam.curseforge.models.getSide
import tech.jamalam.pack.*
import tech.jamalam.util.toCurseforge
import tech.jamalam.util.toSide

class AddFromCurseforge : CliktCommand(name = "curseforge") {
    private val query by argument()

    override fun run() = runBlocking {
        val pack = InMemoryPack(ctx.json)
        val dependencyGraph = loadDependencyGraph()
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

        addMod(pack, dependencyGraph, mod, false)
        dependencyGraph.save()
        pack.save(ctx.json)
    }

    private suspend fun addMod(
        pack: InMemoryPack,
        dependencyGraph: DependencyGraph,
        mod: CurseforgeMod,
        ignoreIfExists: Boolean = true,
    ) {
        if (mod.allowModDistribution == false) {
            error("${mod.name} does not allow distribution")
        }

        val files = runBlocking {
            ctx.curseforgeApi.getModFiles(
                modId = mod.id,
                modLoader = pack.getManifest().loader.type.toCurseforge(),
                gameVersion = pack.getManifest().minecraft
            )
        }.sortedBy {
            it.fileDate
        }.reversed()

        if (files.isEmpty()) {
            error("No files found for ${mod.name}")
        }

        val file = files[0] // Most recent version

        if (file.downloadUrl == null) {
            error("No download URL for ${file.fileName}")
        }

        val tempFile = runBlocking { downloadFileTemp(parseUrl(file.downloadUrl!!)).readBytes() }
        val sha1 = tempFile.digestSha1()
        val sha512 = tempFile.digestSha512()

        val existingManifest = pack.getManifest("mods/${mod.slug}.sculk.json")
        val fileManifest = if (existingManifest != null) {
            if (existingManifest.sources.curseforge != null) {
                if (ignoreIfExists) {
                    return
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
                    sha1 = sha1, sha512 = sha512
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

        pack.setManifest("mods/${mod.slug}.sculk.json", fileManifest)
        terminal.info("Added ${mod.name} to manifest")

        for (dependency in file.dependencies) {
            when (dependency.relationType) {
                CurseforgeFileRelationType.RequiredDependency -> {
                    val dependencyMod = ctx.curseforgeApi.getMod(dependency.modId)
                        ?: error("Dependency not found")

                    dependencyGraph.addDependency(
                        "mods/${dependencyMod.slug}.sculk.json",
                        "mods/${mod.slug}.sculk.json"
                    )

                    addMod(pack, dependencyGraph, dependencyMod)
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
                        dependencyGraph.addDependency(
                            "mods/${dependencyMod.slug}.sculk.json",
                            "mods/${mod.slug}.sculk.json"
                        )

                        addMod(pack, dependencyGraph, dependencyMod)
                    }
                }

                else -> {}
            }
        }
    }
}
