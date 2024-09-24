package tech.jamalam.console

import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference
import org.fusesource.jansi.AnsiConsole
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CodingErrorAction

private val console by lazy {
	val c =if (System.getProperty("os.name").startsWith("Windows")) {
		WindowsConsoleImpl()
	} else {
		UnixConsoleImpl()
	}
	c.initialize()
	c
}

public fun getOrCreateConsole(): Console = console

public sealed class Console {
	internal open fun initialize() {
		AnsiConsole.systemInstall()
		Runtime.getRuntime().addShutdownHook(Thread(this::deinitialize))
	}

	internal fun deinitialize() {
		AnsiConsole.systemUninstall()
		reset()
	}
	
	public abstract fun reset()
	public abstract fun read(): Key

	public sealed class Key
	public class Enter : Key()
	public class ArrowUp : Key()
	public class ArrowDown : Key()
	public class CtrlC : Key()
	public class Backspace : Key()
	public class Invalid : Key()
	public class Other(public val value: Char) : Key() {
		override fun toString(): String {
			return "$value (${value.code})"
		}
	}
}

public class WindowsConsoleImpl : Console() {
	private var consoleHandle: WinNT.HANDLE? = null
	private var originalConsoleMode: Int = 0

	override fun initialize() {
		super.initialize()
		consoleHandle = Kernel32.INSTANCE.GetStdHandle(Kernel32.STD_INPUT_HANDLE)
		originalConsoleMode = getConsoleMode()
		Kernel32.INSTANCE.SetConsoleMode(consoleHandle!!, originalConsoleMode and Kernel32.ENABLE_PROCESSED_INPUT.inv())
	}

	override fun reset() {
		Kernel32.INSTANCE.SetConsoleMode(consoleHandle!!, originalConsoleMode)
	}

	override fun read(): Key {
		var c = Msvcrt.INSTANCE._getwch()

		return when (c) {
			3 -> CtrlC()
			13 -> Enter()
			8 -> Backspace()
			224 -> { // Arrow or function keys
				c = Msvcrt.INSTANCE._getwch()

				return when (c) {
					72 -> ArrowUp()
					80 -> ArrowDown()
					else -> Invalid()
				}
			}

			else -> Other(c.toChar())
		}
	}

	private fun getConsoleMode(): Int {
		val mode = IntByReference()
		Kernel32.INSTANCE.GetConsoleMode(consoleHandle!!, mode)
		return mode.value
	}
}

public class UnixConsoleImpl : Console() {
	private val decoder: CharsetDecoder = Charset.defaultCharset().newDecoder()
	private var originalTermios: Termios? = null
	private var rawTermios: Termios? = null
	private var intermediaryTermios: Termios? = null

	override fun initialize() {
		super.initialize()
		originalTermios = getTermios()
		rawTermios = Termios(originalTermios!!)
		rawTermios!!.c_lflag =
			rawTermios!!.c_lflag and (LibC.FLAG_ICANON or LibC.FLAG_ECHO or LibC.FLAG_ECHONL or LibC.FLAG_ISIG).inv()
		intermediaryTermios = Termios(rawTermios!!)
		intermediaryTermios!!.c_lflag = intermediaryTermios!!.c_lflag or LibC.FLAG_ICANON
	}

	override fun reset() {
		setTermios(originalTermios!!)
	}

	override fun read(): Key {
		val c = readInternal()
		
		if (c == null) {
			return Invalid()
		}
		
		return when (c) {
			3 -> CtrlC()
			10 -> Enter()
			127 -> Backspace()
			27 -> {
				if (readInternal() != 91) Invalid()
				else when (readInternal()) {
					65 -> ArrowUp()
					66 -> ArrowDown()
					else -> Invalid()
				}
			}
			else -> Other(c.toChar())
		}
	}

	private fun readInternal(): Int? {
		setTermios(rawTermios!!)

		try {
			val buf = ByteArray(4)
			var inLen = 0

			while (true) {
				if (inLen >= buf.size) {
					return null
				}

				val c = System.`in`.read()
				if (c == -1) {
					return null
				}

				buf[inLen++] = c.toByte()
				decoder.reset()
				decoder.onMalformedInput(CodingErrorAction.REPLACE)
				decoder.replaceWith("?")
				val inBuf = java.nio.ByteBuffer.wrap(buf, 0, inLen)
				val outBuf = CharBuffer.allocate(1)
				decoder.decode(inBuf, outBuf, false)

				if (outBuf.position() != 0) {
					return outBuf.get(0).code
				}
			}
		} finally {
			setTermios(intermediaryTermios!!)
		}
	}

	private fun getTermios(): Termios {
		val termios = Termios()
		LibC.INSTANCE.tcgetattr(0, termios)
		return termios
	}

	private fun setTermios(termios: Termios) {
		LibC.INSTANCE.tcsetattr(0, LibC.TCSANOW, termios)
	}
}
