package io.github.sculk_cli.util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.github.sculk_cli.curseforge.calculateCurseforgeMurmur2Hash
import java.io.File
import java.nio.file.FileSystems
import java.security.MessageDigest

fun ByteArray.digestSha1(): String {
    val digest = MessageDigest.getInstance("SHA-1")
    return digest.digest(this).fold("") { str, it -> str + "%02x".format(it) }
}

fun ByteArray.digestSha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(this).fold("") { str, it -> str + "%02x".format(it) }
}

fun ByteArray.digestSha512(): String {
    val digest = MessageDigest.getInstance("SHA-512")
    return digest.digest(this).fold("") { str, it -> str + "%02x".format(it) }
}

fun ByteArray.digestMurmur2(): Long {
    return calculateCurseforgeMurmur2Hash(this)
}

fun File.mkdirsAndWriteText(text: String) {
	tryWithContext("while writing to ${this.name}") {
		this.canonicalFile.parentFile.mkdirs()
		writeText(text)
	}
}

inline fun <reified T> File.mkdirsAndWriteJson(json: Json, value: T) {
    mkdirsAndWriteText("${json.encodeToString(value)}\n")
}

fun pathMatchesGlob(path: String, glob: String): Boolean {
    val matcher = FileSystems.getDefault().getPathMatcher("glob:$glob")
    return matcher.matches(FileSystems.getDefault().getPath(path))
}
