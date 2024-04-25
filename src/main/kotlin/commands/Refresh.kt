package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import tech.jamalam.ctx
import tech.jamalam.digestSha256
import tech.jamalam.pack.*
import java.nio.file.Paths

class Refresh : CliktCommand(name = "refresh") {
    override fun run() {
        val basePath = Paths.get("")
        val manifestFile = basePath.resolve("manifest.sculk.json").toFile()
        val manifest =
            ctx.json.decodeFromString<SerialPackManifest>(String(manifestFile.readBytes()))
                .load()
        val ignore = loadSculkIgnore()
        val removedManifests = mutableListOf<String>()
        var updated = false

        for (file in manifest.manifests) {
            val fileManifestFile = basePath.resolve(file.path).toFile()

            if (!fileManifestFile.exists()) {
                terminal.warning("Manifest ${file.path} does not exist, removing it from the manifest")
                removedManifests += file.path
                updated = true
            } else if (fileManifestFile.readBytes().digestSha256() != file.sha256) {
                file.sha256 = fileManifestFile.readBytes().digestSha256()
                updated = true
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
                terminal.warning("File ${file.path} does not exist, removing it from the manifest")
                removedFiles += file.path
                updated = true
            } else if (ignore.isFileIgnored(relativePath)) {
                terminal.warning("File ${file.path} is ignored, removing it from the manifest")
                removedFiles += file.path
                updated = true
            } else if (fileFile.readBytes().digestSha256() != file.sha256) {
                file.sha256 = fileFile.readBytes().digestSha256()
                updated = true
            }
        }

        manifest.files = manifest.files.filter { it.path !in removedFiles }.toMutableList()

        for (file in Paths.get("").toFile().canonicalFile.walkTopDown()) {
            if (file.isDirectory) {
                continue
            }

            val relativePath = file.toRelativeString(Paths.get("").toFile().canonicalFile)
            if (relativePath == "manifest.sculk.json" || relativePath == "dependency-graph.sculk.json" || relativePath == ".sculkignore" || ignore.isFileIgnored(
                    relativePath
                )
            ) {
                continue
            }

            if (relativePath.endsWith(".sculk.json")) {
                if (manifest.manifests.none { it.path == relativePath }) {
                    terminal.info("Found new manifest $relativePath, adding it to the manifest")
                    manifest.manifests += PackManifestManifest(
                        path = relativePath,
                        sha256 = file.readBytes().digestSha256(),
                    )
                    updated = true
                }
            } else {
                if (manifest.files.none { it.path == relativePath }) {
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


        if (updated) {
            val newManifest = manifest.toSerial()
            manifestFile.writeBytes(
                ctx.json.encodeToString(
                    SerialPackManifest.serializer(),
                    newManifest
                ).toByteArray()
            )
            terminal.info("Updated manifest.sculk.json")
        } else {
            terminal.info("Manifest already up to date")
        }
    }
}
