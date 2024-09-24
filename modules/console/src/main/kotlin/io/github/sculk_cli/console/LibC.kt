package io.github.sculk_cli.console

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Structure

internal interface LibC : Library {
	fun tcgetattr(fd: Int, termios: Termios): Int
	fun tcsetattr(fd: Int, opt: Int, termios: Termios): Int
	
	companion object {
		val INSTANCE: LibC = Native.load("c", LibC::class.java)
		const val TCSANOW: Int = 0
		const val FLAG_ISIG: Int = 1
		const val FLAG_ICANON: Int = 2
		const val FLAG_ECHO: Int = 8
		const val FLAG_ECHONL: Int = 64
	}
}

@Suppress("PropertyName")
internal class Termios(
	@JvmField var c_iflag: Int = 0,
	@JvmField var c_oflag: Int = 0,
	@JvmField var c_cflag: Int = 0,
	@JvmField var c_lflag: Int =0,
	@JvmField var c_line: Byte = 0,
	@JvmField var filler: ByteArray = ByteArray(64)
) : Structure() {
	constructor(other: Termios) : this(
		other.c_iflag,
		other.c_oflag,
		other.c_cflag,
		other.c_lflag,
		other.c_line,
		other.filler.clone()
	)
	
	override fun getFieldOrder(): List<String?> {
		return listOf("c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_line", "filler")
	}
}
