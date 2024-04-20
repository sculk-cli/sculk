package tech.jamalam.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import tech.jamalam.ctx
import tech.jamalam.digestSha256
import tech.jamalam.pack.SerialPackManifest
import tech.jamalam.pack.load
import tech.jamalam.pack.toSerial
import java.nio.file.Paths

class Refresh : CliktCommand(name = "refresh") {
    override fun run() {
        val basePath = Paths.get("")
        val manifestFile = basePath.resolve("manifest.sculk.json").toFile()
        val manifest =
            ctx.json.decodeFromString<SerialPackManifest>(String(manifestFile.readBytes()))
                .load()
        val removedFiles = mutableListOf<String>()

        for (file in manifest.files) {
            val fileManifestFile = basePath.resolve(file.path).toFile()

            if (!fileManifestFile.exists()) {
                terminal.warning("File ${file.path} does not exist, removing it from the manifest")
                removedFiles += file.path
            } else if (fileManifestFile.readBytes().digestSha256() != file.sha256) {
                file.sha256 = fileManifestFile.readBytes().digestSha256()
            }
        }

        manifest.files = manifest.files.filter { it.path !in removedFiles }
        val newManifest = manifest.toSerial()
        manifestFile.writeBytes(
            ctx.json.encodeToString(
                SerialPackManifest.serializer(),
                newManifest
            ).toByteArray()
        )
        terminal.info("Updated manifest.sculk.json with new hashes")
    }
}
