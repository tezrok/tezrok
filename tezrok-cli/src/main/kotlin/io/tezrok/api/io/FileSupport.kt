package io.tezrok.api.io

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

interface FileSupport : OutStream, InStream {
    fun getSize(): Long

    fun isDirectory(): Boolean

    fun isFile(): Boolean

    fun asString(charset: Charset = StandardCharsets.UTF_8): String =
        String(getInputStream().use { it.readBytes() }, charset)

    fun isEmpty(): Boolean

    fun isNotEmpty(): Boolean = !isEmpty()
}

