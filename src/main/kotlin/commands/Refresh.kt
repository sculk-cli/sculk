package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import tech.jamalam.Context
import tech.jamalam.pack.*
import tech.jamalam.util.digestSha256
import tech.jamalam.util.mkdirsAndWriteJson
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds

class Refresh : CliktCommand(
    name = "refresh",
    help = "Check all hashes in the manifest and update them if needed"
) {
    private val check by option().flag().help("Check hashes without updating them")
    private val watch by option().flag().help("Watch for changes in the filesystem and update the manifest automatically")

    override fun run() {
        val ctx = Context.getOrCreate(terminal)
        val basePath = Paths.get("")
        refresh(ctx, basePath)

        if (watch) {
            terminal.info("Watching for changes in the filesystem...")
            val svc = FileSystems.getDefault().newWatchService()
            basePath.register(svc, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY)

            poll_loop@ while (true) {
                val key = svc.take()

                for (event in key.pollEvents()) {
                    val changed = event.context() as Path

                    if (changed.fileName.toString() == "manifest.sculk.json") {
                        key.reset()
                        continue@poll_loop
                    }
                }

                terminal.info("Detected changes in the filesystem...")

                try {
                    refresh(ctx, basePath)
                } catch (e: Exception) {
                    terminal.warning("Error refreshing manifest: ${e.message}")
                    terminal.warning("Waiting for next change...")
                }

                key.reset()
            }
        }
    }

    private fun refresh(ctx: Context, basePath: Path) {
        val manifestFile = basePath.resolve("manifest.sculk.json").toFile()
        val manifest =
            ctx.json.decodeFromString<SerialPackManifest>(String(manifestFile.readBytes())).load()
        val ignore = loadSculkIgnore()
        val removedManifests = mutableListOf<String>()
        var updated = false

        for (file in manifest.manifests) {
            val fileManifestFile = basePath.resolve(file.path).toFile()

            if (!fileManifestFile.exists()) {
                if (check) {
                    error("Manifest ${file.path} does not exist")
                } else {
                    terminal.warning("Manifest ${file.path} does not exist, removing it from the manifest")
                    removedManifests += file.path
                    updated = true
                }
            } else if (fileManifestFile.readBytes().digestSha256() != file.sha256) {
                if (check) {
                    error("Manifest ${file.path} has a bad hash")
                } else {
                    file.sha256 = fileManifestFile.readBytes().digestSha256()
                    updated = true
                }
            }
        }

        manifest.manifests =
            manifest.manifests.filter { it.path !in removedManifests }.toMutableList()

        val removedFiles = mutableListOf<String>()

        for (file in manifest.files) {
            val fileFile = basePath.resolve(file.path).toFile()
            val relativePath =
                fileFile.canonicalFile.toRelativeString(Paths.get("").toFile().canonicalFile)

            if (!fileFile.exists()) {
                if (check) {
                    error("File ${file.path} does not exist")
                } else {
                    terminal.warning("File ${file.path} does not exist, removing it from the manifest")
                    removedFiles += file.path
                    updated = true
                }
            } else if (ignore.isFileIgnored(relativePath)) {
                if (check) {
                    error("File ${file.path} is ignored, but it is in the manifest")
                } else {
                    terminal.warning("File ${file.path} is ignored, removing it from the manifest")
                    removedFiles += file.path
                    updated = true
                }
            } else if (fileFile.readBytes().digestSha256() != file.sha256) {
                if (check) {
                    error("File ${file.path} has a bad hash")
                } else {
                    file.sha256 = fileFile.readBytes().digestSha256()
                    updated = true
                }
            }
        }

        manifest.files = manifest.files.filter { it.path !in removedFiles }.toMutableList()

        for (file in Paths.get("").toFile().canonicalFile.walkTopDown()) {
            if (file.isDirectory) {
                continue
            }

            val relativePath = file.toRelativeString(Paths.get("").toFile().canonicalFile)
            if (ignore.isFileIgnored(relativePath)) {
                continue
            }

            if (relativePath.endsWith(".sculk.json")) {
                if (manifest.manifests.none { it.path == relativePath }) {
                    if (check) {
                        error("Manifest $relativePath is not included in the root manifest")
                    } else {
                        terminal.info("Found new manifest $relativePath, adding it to the manifest")
                        manifest.manifests += PackManifestManifest(
                            path = relativePath,
                            sha256 = file.readBytes().digestSha256(),
                        )
                        updated = true
                    }
                }
            } else {
                if (manifest.files.none { it.path == relativePath }) {
                    if (check) {
                        error("File $relativePath is not included in the root manifest")
                    } else {
                        terminal.info("Found new file $relativePath, adding it to the manifest")
                        manifest.files += PackManifestFile(
                            path = relativePath,
                            sha256 = file.readBytes().digestSha256(),
                            side = Side.Both
                        )
                        updated = true
                    }
                }
            }
        }

        if (!check) {
            if (updated) {
                val newManifest = manifest.toSerial()
                manifestFile.mkdirsAndWriteJson(
                    ctx.json,
                    newManifest
                )
                terminal.info("Updated manifest.sculk.json")
            } else {
                terminal.info("Manifest already up to date")
            }
        }
    }
}
