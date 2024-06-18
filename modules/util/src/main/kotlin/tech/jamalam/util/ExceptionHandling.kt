package tech.jamalam.util

public fun <T> tryWithContext(context: String, block: () -> T): T {
    try {
        return block()
    } catch (e: Exception) {
        error("[$context] ${e.message}")
    }
}

public suspend fun <T> tryWithContextSuspend(context: String, block: suspend () -> T): T {
    try {
        return block()
    } catch (e: Exception) {
        error("[$context] ${e.message}")
    }
}
