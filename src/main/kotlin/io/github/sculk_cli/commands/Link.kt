package io.github.sculk_cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import kotlinx.coroutines.runBlocking
import io.github.sculk_cli.BooleanPrettyPrompt
import io.github.sculk_cli.Context
import io.github.sculk_cli.pack.FileManifest
import io.github.sculk_cli.pack.save
import io.github.sculk_cli.util.addCurseforgeFile
import io.github.sculk_cli.util.addModrinthVersion
import io.github.sculk_cli.modrinth.models.ModrinthHashAlgorithm

class Link : CliktCommand(
    name = "link",
    help = "Finds Curseforge/Modrinth projects for files in the manifest"
) {
    private val target by argument().enum<Target>().help("The site to search for projects on")
    private val yes by option().flag("yes", "y", default = false)
        .help("Automatically accept matches")

    override fun run() = runBlocking {
        val ctx = Context.Companion.getOrCreate(terminal)

        val manifests = ctx.pack.getManifests().filter {
            when (target) {
                Target.Curseforge -> {
                    it.value.sources.curseforge == null && (it.value.sources.modrinth != null || it.value.sources.url != null)
                }

                Target.Modrinth -> {
                    it.value.sources.modrinth == null && (it.value.sources.curseforge != null || it.value.sources.url != null)
                }
            }
        }

        var i = 1
        for (manifest in manifests) {
            terminal.info("Attempting to find matches for ${manifest.key} (${i}/${manifests.size})")
            tryLinkManifest(ctx, manifest)
            i += 1
        }

        ctx.pack.save(ctx.json)
        ctx.dependencyGraph.save()
    }

    private suspend fun tryLinkManifest(
	    ctx: Context,
	    manifest: Map.Entry<String, FileManifest>
    ) {
        when (target) {
            Target.Curseforge -> {
                val matches = ctx.curseforge.getFingerprintMatches(manifest.value.hashes.murmur2)

                if (matches.isEmpty()) {
                    terminal.info("No match found for ${manifest.key}")
                    return
                }

                var file = matches.firstOrNull { it.fileName == manifest.value.filename }

                if (file == null) {
                    file = matches.first()
                }

                val mod = ctx.curseforge.getMod(file.modId) ?: error("File's mod does not exist")

                if (showConfirmation(manifest.key, "${mod.name} (${mod.id})")) {
	                addCurseforgeFile(
		                ctx,
		                mod,
		                file,
		                downloadDependencies = false,
		                manifestPath = manifest.key
	                )
                }
            }

            Target.Modrinth -> {
                val version = ctx.modrinth.getVersionFromHash(
                    manifest.value.hashes.sha512,
                    ModrinthHashAlgorithm.SHA512
                )

                if (version == null) {
                    terminal.info("No match found for ${manifest.key}")
                    return
                }

                val project = ctx.modrinth.getProject(version.projectId)
                    ?: error("Version's project does not exist")

                if (showConfirmation(manifest.key, "${project.title} (${project.id})")) {
	                addModrinthVersion(
		                ctx,
		                project,
		                version,
		                downloadDependencies = false,
		                manifestPath = manifest.key
	                )
                }
            }
        }
    }

    private fun showConfirmation(path: String, projectTitle: String): Boolean {
        if (yes) {
            terminal.info("Found match $projectTitle for $path")
            return true
        } else {
            terminal.info("Found potential match $projectTitle for $path")
            return BooleanPrettyPrompt("Accept [y/n]", terminal = terminal).ask()
        }
    }

    enum class Target {
        Curseforge,
        Modrinth
    }
}
