package com.anilibrix.plus.core.torrent

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * Парсер BEncode формата (.torrent файлов).
 */
class BencodeParser(private val input: InputStream) {

    fun parse(): Any? {
        val b = input.read()
        if (b == -1) return null
        val c = b.toChar()
        return when (c) {
            'i' -> parseInteger()
            'l' -> parseList()
            'd' -> parseDictionary()
            in '0'..'9' -> parseByteString(c)
            else -> null
        }
    }

    private fun parseInteger(): Long {
        val sb = StringBuilder()
        var b: Int
        while (input.read().also { b = it } != -1) {
            val c = b.toChar()
            if (c == 'e') break
            sb.append(c)
        }
        return sb.toString().toLongOrNull() ?: 0L
    }

    private fun parseByteString(firstDigit: Char): ByteArray {
        val lenSb = StringBuilder().append(firstDigit)
        var b: Int
        while (input.read().also { b = it } != -1) {
            val c = b.toChar()
            if (c == ':') break
            lenSb.append(c)
        }
        val length = lenSb.toString().toIntOrNull() ?: 0
        val bytes = ByteArray(length)
        var readTotal = 0
        while (readTotal < length) {
            val read = input.read(bytes, readTotal, length - readTotal)
            if (read == -1) break
            readTotal += read
        }
        return bytes
    }

    private fun parseList(): List<Any> {
        val list = mutableListOf<Any>()
        while (true) {
            input.mark(1)
            val b = input.read()
            if (b == -1 || b.toChar() == 'e') break
            input.reset()
            val item = parse()
            if (item != null) list.add(item)
        }
        return list
    }

    private fun parseDictionary(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        while (true) {
            input.mark(1)
            val b = input.read()
            if (b == -1 || b.toChar() == 'e') break
            input.reset()

            val keyBytes = parse() as? ByteArray ?: break
            val key = String(keyBytes, StandardCharsets.UTF_8)
            val value = parse() ?: break
            map[key] = value
        }
        return map
    }

    companion object {
        fun parse(bytes: ByteArray): Map<String, Any>? {
            return runCatching {
                val stream = ByteArrayInputStream(bytes)
                val parser = BencodeParser(stream)
                @Suppress("UNCHECKED_CAST")
                parser.parse() as? Map<String, Any>
            }.getOrNull()
        }
    }
}
