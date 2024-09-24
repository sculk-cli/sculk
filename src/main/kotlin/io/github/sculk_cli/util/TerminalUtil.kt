package io.github.sculk_cli.util

import com.github.ajalt.clikt.parameters.options.OptionWithValues
import com.github.ajalt.clikt.parameters.options.RawOption
import com.github.ajalt.clikt.parameters.options.transformAll
import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.http.*
import org.fusesource.jansi.Ansi
import io.github.sculk_cli.FilePrettyPrompt
import io.github.sculk_cli.PrettyListPrompt
import io.github.sculk_cli.StringPrettyPrompt
import io.github.sculk_cli.UrlPrettyPrompt
import java.io.File

fun Terminal.clearLine() {
    rawPrint(Ansi.ansi().eraseLine(Ansi.Erase.ALL).toString())
    rawPrint("\r")
}

fun Terminal.clearLines(count: Int) {
	(0..<count).forEach {
		moveUp()
		clearLine()
	}
}

fun Terminal.moveUp() {
    rawPrint(Ansi.ansi().cursorUpLine().toString())
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
