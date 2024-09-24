package io.github.sculk_cli.curseforge

private const val M: Long = 0x5bd1e995L
private const val R = 24
private const val UNSIGNED_MASK: Long = 0xff
private const val UINT_MASK: Long = 0xFFFFFFFFL

public fun calculateCurseforgeMurmur2Hash(data: ByteArray): Long {
    val data = data
        .filter {
            !isWhitespace(it)
        }

    val length = data.size

    if (length == 0) {
        return 0L
    }

    // Initialize the hash to a 'random' value
    var hash = 1L xor length.toLong() and UINT_MASK

    // Mix 4 bytes at a time into the hash
    val length4 = length.ushr(2)

    for (i in 0 until length4) {
        val i4 = i shl 2

        var k = data[i4].toLong() and UNSIGNED_MASK
        k = k or ((data[i4 + 1].toLong() and UNSIGNED_MASK) shl 8)
        k = k or ((data[i4 + 2].toLong() and UNSIGNED_MASK) shl 16)
        k = k or ((data[i4 + 3].toLong() and UNSIGNED_MASK) shl 24)

        k = k * M and UINT_MASK
        k = k xor (k.ushr(R) and UINT_MASK)
        k = k * M and UINT_MASK

        hash = hash * M and UINT_MASK
        hash = hash xor k and UINT_MASK
    }

    // Handle the last few bytes of the input array
    val offset = length4 shl 2
    when (length and 3) {
        3 -> {
            hash = hash xor (data[offset + 2].toLong() shl 16 and UINT_MASK)
            hash = hash xor (data[offset + 1].toLong() shl 8 and UINT_MASK)
            hash = hash xor (data[offset].toLong() and UINT_MASK)
            hash = hash * M and UINT_MASK
        }

        2 -> {
            hash = hash xor (data[offset + 1].toLong() shl 8 and UINT_MASK)
            hash = hash xor (data[offset].toLong() and UINT_MASK)
            hash = hash * M and UINT_MASK
        }

        1 -> {
            hash = hash xor (data[offset].toLong() and UINT_MASK)
            hash = hash * M and UINT_MASK
        }
    }

    hash = hash xor (hash.ushr(13) and UINT_MASK)
    hash = hash * M and UINT_MASK
    hash = hash xor hash.ushr(15)

    return hash
}

private fun isWhitespace(b: Byte): Boolean {
    return b == 9.toByte() || b == 10.toByte() || b == 13.toByte() || b == 32.toByte()
}
