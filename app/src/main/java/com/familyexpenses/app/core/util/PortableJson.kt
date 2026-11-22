package com.familyexpenses.app.core.util

sealed interface JsonValue

data class JsonObject(val entries: Map<String, JsonValue>) : JsonValue {
    fun getString(key: String): String =
        (entries[key] as? JsonString)?.value
            ?: throw IllegalArgumentException("Campo '$key' ausente o no es texto.")

    fun getNullableString(key: String): String? = when (val v = entries[key]) {
        null, JsonNull -> null
        is JsonString -> v.value
        else -> throw IllegalArgumentException("Campo '$key' no es texto o null.")
    }

    fun getLong(key: String): Long =
        (entries[key] as? JsonNumber)?.value
            ?: throw IllegalArgumentException("Campo '$key' ausente o no es entero.")

    fun getNullableLong(key: String): Long? = when (val v = entries[key]) {
        null, JsonNull -> null
        is JsonNumber -> v.value
        else -> throw IllegalArgumentException("Campo '$key' no es entero o null.")
    }

    fun getInt(key: String): Int = getLong(key).toInt()

    fun getNullableInt(key: String): Int? = getNullableLong(key)?.toInt()

    fun getBoolean(key: String): Boolean =
        (entries[key] as? JsonBoolean)?.value
            ?: throw IllegalArgumentException("Campo '$key' ausente o no es booleano.")

    fun getArray(key: String): JsonArray =
        (entries[key] as? JsonArray)
            ?: throw IllegalArgumentException("Campo '$key' ausente o no es lista.")

    fun getObject(key: String): JsonObject =
        (entries[key] as? JsonObject)
            ?: throw IllegalArgumentException("Campo '$key' ausente o no es objeto.")
}

data class JsonArray(val items: List<JsonValue>) : JsonValue
data class JsonString(val value: String) : JsonValue
data class JsonNumber(val value: Long) : JsonValue
data class JsonBoolean(val value: Boolean) : JsonValue
data object JsonNull : JsonValue

object PortableJson {

    fun parseObject(raw: String): JsonObject {
        val parser = Parser(raw)
        val root = parser.parseValue()
        parser.skipWhitespace()
        if (!parser.isAtEnd()) {
            throw IllegalArgumentException("Contenido inesperado al final del JSON.")
        }
        return root as? JsonObject
            ?: throw IllegalArgumentException("La copia de seguridad debe ser un objeto JSON.")
    }

    fun stringify(value: JsonValue, indent: Int = 2): String {
        val sb = StringBuilder()
        writeValue(sb, value, indent, depth = 0)
        return sb.toString()
    }

    private fun writeValue(sb: StringBuilder, value: JsonValue, indent: Int, depth: Int) {
        when (value) {
            is JsonNull -> sb.append("null")
            is JsonBoolean -> sb.append(if (value.value) "true" else "false")
            is JsonNumber -> sb.append(value.value)
            is JsonString -> writeEscapedString(sb, value.value)
            is JsonArray -> {
                if (value.items.isEmpty()) {
                    sb.append("[]")
                } else {
                    sb.append('[')
                    val childPad = " ".repeat((depth + 1) * indent)
                    val selfPad = " ".repeat(depth * indent)
                    value.items.forEachIndexed { index, item ->
                        if (index > 0) sb.append(',')
                        sb.append('\n').append(childPad)
                        writeValue(sb, item, indent, depth + 1)
                    }
                    sb.append('\n').append(selfPad).append(']')
                }
            }
            is JsonObject -> {
                if (value.entries.isEmpty()) {
                    sb.append("{}")
                } else {
                    sb.append('{')
                    val childPad = " ".repeat((depth + 1) * indent)
                    val selfPad = " ".repeat(depth * indent)
                    var first = true
                    for ((k, v) in value.entries) {
                        if (!first) sb.append(',')
                        first = false
                        sb.append('\n').append(childPad)
                        writeEscapedString(sb, k)
                        sb.append(": ")
                        writeValue(sb, v, indent, depth + 1)
                    }
                    sb.append('\n').append(selfPad).append('}')
                }
            }
        }
    }

    private fun writeEscapedString(sb: StringBuilder, s: String) {
        sb.append('"')
        for (ch in s) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (ch.code < 0x20) {
                        sb.append("\\u").append(ch.code.toString(16).padStart(4, '0'))
                    } else {
                        sb.append(ch)
                    }
                }
            }
        }
        sb.append('"')
    }

    private class Parser(private val src: String) {
        private var pos = 0

        fun isAtEnd(): Boolean = pos >= src.length

        fun skipWhitespace() {
            while (pos < src.length && src[pos].isWhitespace()) {
                pos++
            }
        }

        fun parseValue(): JsonValue {
            skipWhitespace()
            if (isAtEnd()) throw IllegalArgumentException("JSON incompleto.")
            return when (val ch = src[pos]) {
                '{' -> parseObjectInternal()
                '[' -> parseArrayInternal()
                '"' -> JsonString(parseStringInternal())
                't' -> parseLiteral("true", JsonBoolean(true))
                'f' -> parseLiteral("false", JsonBoolean(false))
                'n' -> parseLiteral("null", JsonNull)
                '-', in '0'..'9' -> parseNumberInternal()
                else -> throw IllegalArgumentException("Carácter inesperado '$ch' en posición $pos.")
            }
        }

        private fun parseObjectInternal(): JsonObject {
            pos++ // '{'
            skipWhitespace()
            val map = linkedMapOf<String, JsonValue>()
            if (pos < src.length && src[pos] == '}') {
                pos++
                return JsonObject(map)
            }
            while (true) {
                skipWhitespace()
                if (isAtEnd() || src[pos] != '"') {
                    throw IllegalArgumentException("Se esperaba clave de texto en posición $pos.")
                }
                val key = parseStringInternal()
                skipWhitespace()
                if (isAtEnd() || src[pos] != ':') {
                    throw IllegalArgumentException("Se esperaba ':' tras clave '$key'.")
                }
                pos++
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                if (isAtEnd()) throw IllegalArgumentException("Objeto JSON sin cerrar.")
                when (src[pos]) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        return JsonObject(map)
                    }
                    else -> throw IllegalArgumentException("Se esperaba ',' o '}' en posición $pos.")
                }
            }
        }

        private fun parseArrayInternal(): JsonArray {
            pos++ // '['
            skipWhitespace()
            val list = mutableListOf<JsonValue>()
            if (pos < src.length && src[pos] == ']') {
                pos++
                return JsonArray(list)
            }
            while (true) {
                list += parseValue()
                skipWhitespace()
                if (isAtEnd()) throw IllegalArgumentException("Lista JSON sin cerrar.")
                when (src[pos]) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return JsonArray(list)
                    }
                    else -> throw IllegalArgumentException("Se esperaba ',' o ']' en posición $pos.")
                }
            }
        }

        private fun parseStringInternal(): String {
            pos++ // '"'
            val sb = StringBuilder()
            while (pos < src.length) {
                val ch = src[pos++]
                when (ch) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= src.length) throw IllegalArgumentException("Escape JSON truncado.")
                        when (val esc = src[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > src.length) {
                                    throw IllegalArgumentException("Escape unicode JSON truncado.")
                                }
                                val hex = src.substring(pos, pos + 4)
                                pos += 4
                                sb.append(hex.toInt(16).toChar())
                            }
                            else -> throw IllegalArgumentException("Escape JSON inválido '\\$esc'.")
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            throw IllegalArgumentException("Texto JSON sin cerrar.")
        }

        private fun parseLiteral(expected: String, value: JsonValue): JsonValue {
            if (!src.startsWith(expected, startIndex = pos)) {
                throw IllegalArgumentException("Token inesperado en posición $pos.")
            }
            pos += expected.length
            return value
        }

        private fun parseNumberInternal(): JsonNumber {
            val start = pos
            if (src[pos] == '-') pos++
            if (pos >= src.length || !src[pos].isDigit()) {
                throw IllegalArgumentException("Número JSON inválido en posición $start.")
            }
            while (pos < src.length && src[pos].isDigit()) {
                pos++
            }
            val token = src.substring(start, pos)
            val parsed = token.toLongOrNull()
                ?: throw IllegalArgumentException("Número JSON fuera de rango: '$token'.")
            return JsonNumber(parsed)
        }
    }
}
