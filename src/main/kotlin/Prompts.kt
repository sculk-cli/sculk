package tech.jamalam

import biz.source_code.utils.RawConsoleInput
import com.github.ajalt.mordant.terminal.ConversionResult
import com.github.ajalt.mordant.terminal.Prompt
import com.github.ajalt.mordant.terminal.Terminal
import io.ktor.http.*
import tech.jamalam.util.clearLine
import tech.jamalam.util.clearLines
import java.io.File
import kotlin.system.exitProcess

class PrettyListPrompt(prompt: String, choices: Collection<String>, terminal: Terminal) :
    Prompt<String>(prompt, terminal, choices = choices) {
    override fun ask(): String {
        var filter: String? = null
        var choiceIdx = 0
        terminal.cursor.hide(true)
        terminal.println(buildString {
            append(terminal.theme.muted("?"))
            append(" ")
            append(prompt)
            append(" ")
            append(
                terminal.theme.muted(
                    "(arrow keys to move, enter to select, type to filter)"
                )
            )
        })

        var printedLines: Int

        while (true) {
            if (filter != null) {
                terminal.println(
                    terminal.theme.muted(
                        " (filter: $filter)"
                    )
                )
            }

            printedLines = printChoices(choices, choiceIdx, filter)

            if (filter != null) {
                printedLines += 1
            }

            when (val char = RawConsoleInput.read(true)) {
                3 -> exitProcess(0) // CTRL-C
                10 -> break // Enter
                127 -> { // Backspace
                    if (filter != null) {
                        filter = filter.dropLast(1)
                    }
                }

                27 -> {
                    if (RawConsoleInput.read(true) == 91) {
                        when (RawConsoleInput.read(true)) {
                            // Up arrow
                            65 -> choiceIdx = if (choiceIdx == 0) {
                                choices.size - 1
                            } else {
                                choiceIdx - 1
                            }
                            // Down arrow
                            66 -> choiceIdx = (choiceIdx + 1) % choices.size
                        }
                    }
                }

                else -> {
                    filter = (filter ?: "") + char.toChar()
                    choiceIdx = 0
                }
            }

            terminal.clearLines(printedLines)
        }

        val choice = choices.filter {
            filter == null || it.contains(filter, ignoreCase = true)
        }.elementAt(choiceIdx)

        terminal.clearLines(printedLines + 1)
        terminal.println(buildString {
            append(terminal.theme.muted("?"))
            append(" ")
            append(prompt)
            append(" ")
            append(
                terminal.theme.info(
                    choice
                )
            )
        })

        terminal.cursor.show()
        RawConsoleInput.resetConsoleMode()
        return choice
    }

    override fun convert(input: String): ConversionResult<String> = ConversionResult.Valid(input)

    private fun printChoices(choices: Collection<String>, choiceIdx: Int, filter: String?): Int {
        val filteredChoices = choices.filter {
            filter == null || it.contains(filter, ignoreCase = true)
        }

        if (filteredChoices.size > 5) {
            val startIdx = choiceIdx - 2
            val endIdx = choiceIdx + 2

            for (idx in startIdx..endIdx) {
                var actualIdx = idx

                if (actualIdx < 0) {
                    actualIdx += filteredChoices.size
                }

                if (actualIdx >= filteredChoices.size) {
                    actualIdx -= filteredChoices.size
                }

                printChoice(filteredChoices.elementAt(actualIdx), actualIdx == choiceIdx)
            }

            return 5
        } else {
            for ((idx, choice) in filteredChoices.withIndex()) {
                printChoice(choice, idx == choiceIdx)
            }

            return filteredChoices.size
        }
    }

    private fun printChoice(choice: String, isSelected: Boolean) {
        terminal.println(buildString {
            if (isSelected) {
                append(terminal.theme.muted("> "))
                append(terminal.theme.info(choice))
            } else {
                append("  ")
                append(choice)
            }
        })
    }
}

abstract class PrettyPrompt<T>(
    prompt: String, terminal: Terminal, default: T? = null
) : Prompt<T>(prompt, terminal, default = default) {
    override fun ask(): T {
        var input = ""
        var error = false

        while (true) {
            printPrompt(input, error)

            when (val char = RawConsoleInput.read(true)) {
                3 -> exitProcess(0) // CTRL-C
                10 -> { // Enter
                    return if (input.isNotEmpty()) {
                        when (val conversion = convert(input)) {
                            is ConversionResult.Valid -> {
                                terminal.clearLine()
                                printPrompt(input, error = false, finished = true)
                                RawConsoleInput.resetConsoleMode()
                                conversion.value
                            }

                            is ConversionResult.Invalid -> {
                                error = true
                                input = ""
                                terminal.clearLine()
                                continue
                            }
                        }
                    } else if (default != null) {
                        terminal.clearLine()
                        printPrompt(input, error = false, finished = true)
                        RawConsoleInput.resetConsoleMode()
                        default!!
                    } else {
                        error = true
                        terminal.clearLine()
                        continue
                    }
                }

                127 -> { // Backspace
                    input = input.dropLast(1)
                    terminal.clearLine()
                }

                27 -> {} // Control code
                else -> {
                    terminal.clearLine()
                    input += char.toChar()
                }
            }
        }
    }

    private fun printPrompt(input: String, error: Boolean, finished: Boolean = false) {
        terminal.print(buildString {
            if (error) {
                append(terminal.theme.danger("?"))
            } else {
                append(terminal.theme.muted("?"))
            }

            append(" ")
            append(prompt)

            if (default != null) {
                append(terminal.theme.info(" ($default)"))
            }

            append(": ")

            if (finished) {
                append(terminal.theme.info(input))
            } else {
                append(input)
            }
        })

        if (finished) {
            terminal.println()
        }
    }
}

class StringPrettyPrompt(
    prompt: String, terminal: Terminal, default: String? = null
) : PrettyPrompt<String>(prompt, terminal, default = default) {
    override fun convert(input: String): ConversionResult<String> = ConversionResult.Valid(input)
}

class FilePrettyPrompt(
    prompt: String, terminal: Terminal, default: File? = null
) : PrettyPrompt<File>(prompt, terminal, default = default) {
    override fun convert(input: String): ConversionResult<File> =
        ConversionResult.Valid(File(input))
}

class UrlPrettyPrompt(
    prompt: String, terminal: Terminal, default: Url? = null
) : PrettyPrompt<Url>(prompt, terminal, default = default) {
    override fun convert(input: String): ConversionResult<Url> {
        return try {
            ConversionResult.Valid(URLBuilder().takeFrom(input).build())
        } catch (e: URLParserException) {
            ConversionResult.Invalid("Invalid URL")
        }
    }
}

class BooleanPrettyPrompt(
    prompt: String, terminal: Terminal, default: Boolean? = null
): PrettyPrompt<Boolean>(prompt, terminal, default = default) {
    override fun convert(input: String): ConversionResult<Boolean> {
        return when (input) {
            "y" -> ConversionResult.Valid(true)
            "n" -> ConversionResult.Valid(false)
            else -> ConversionResult.Invalid("enter y/n")
        }
    }
}
