package tech.jamalam

import com.github.ajalt.clikt.parameters.options.OptionWithValues
import com.github.ajalt.clikt.parameters.options.RawOption
import com.github.ajalt.clikt.parameters.options.transformAll
import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

fun File.mkdirsAndWriteText(text: String) {
    this.canonicalFile.parentFile.mkdirs()
    writeText(text)
}

inline fun <reified T> File.mkdirsAndWriteJson(json: Json, value: T) {
    mkdirsAndWriteText("${json.encodeToString(value)}\n")
}

fun Terminal.clearLines(count: Int) {
    for (i in 0..<count) {
        moveUp()
        clearLine()
    }
}

fun Terminal.moveUp() {
    rawPrint("\u001B[A")
}

fun Terminal.clearLine() {
    rawPrint("\u001B[2K\r")
}

fun parseUrl(url: String): Url {
    return URLBuilder(url).build()
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> RawOption.prettyPrompt(
    prompt: String,
    default: T? = null,
    choices: List<String>? = null
): OptionWithValues<T, String, String> {
    return transformAll {
        if (choices != null) {
            if (it.lastOrNull() != null) {
                it.lastOrNull() as T
            } else {
                PrettyListPrompt(prompt, choices, terminal).ask() as T
            }
        } else {
            when (T::class) {
                String::class -> {
                    if (it.lastOrNull() != null) {
                        it.lastOrNull() as T
                    } else {
                        StringPrettyPrompt(
                            prompt,
                            terminal,
                            default = default as String?
                        ).ask() as T
                    }
                }

                Url::class -> {
                    if (it.lastOrNull() != null) {
                        URLBuilder().takeFrom(it.lastOrNull()!!).build() as T
                    } else {
                        UrlPrettyPrompt(prompt, terminal, default = default as Url?).ask() as T
                    }
                }

                File::class -> {
                    if (it.lastOrNull() != null) {
                        File(it.lastOrNull()!!) as T
                    } else {
                        FilePrettyPrompt(prompt, terminal, default = default as File?).ask() as T
                    }
                }

                else -> {
                    if (T::class.java.isEnum) {
                        val entries =
                            T::class.java.enumConstants as Array<out Enum<*>>
                        val response = it.lastOrNull() ?: PrettyListPrompt(
                            prompt,
                            entries.map { enum -> enum.name.lowercase() },
                            terminal
                        ).ask()
                        entries.first { e -> e.name.lowercase() == response }
                    } else {
                        error("Can't create a prompt of type ${T::class}")
                    } as T
                }
            }
        }
    }
}

suspend fun downloadFileTemp(url: Url): File {
    val tempFile = File.createTempFile("sculk", null)
    tempFile.deleteOnExit()
    tempFile.writeBytes(tryReq(url))

    return tempFile
}

suspend fun tryReq(url: Url, maxAttempts: Int = 3): ByteArray {
    var attempts = 0

    while (attempts < maxAttempts) {
        try {
            val response = ctx.client.get(url)
            return response.body()
        } catch (e: Exception) {
            attempts += 1
        }
    }

    error("Failed to complete request to $url after $maxAttempts attempts")
}

fun pathMatchesGlob(path: String, glob: String): Boolean {
    val matcher = FileSystems.getDefault().getPathMatcher("glob:$glob")
    return matcher.matches(FileSystems.getDefault().getPath(path))
}
