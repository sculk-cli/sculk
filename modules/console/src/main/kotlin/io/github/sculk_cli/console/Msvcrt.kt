package io.github.sculk_cli.console

import com.sun.jna.Library
import com.sun.jna.Native

internal interface Msvcrt: Library {
	@Suppress("FunctionName")
	fun _getwch(): Int

	companion object {
		val INSTANCE: Msvcrt = Native.load("msvcrt", Msvcrt::class.java)
	}
}
