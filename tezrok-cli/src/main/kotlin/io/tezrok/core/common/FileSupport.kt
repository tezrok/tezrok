package io.tezrok.core.common

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

interface FileSupport : OutStream, InStream {
    fun getFiles(): List<FileSupport>

    fun addFile(name: String): FileSupport

    fun addDirectory(name: String): FileSupport

    fun removeFiles(names: List<String>): Boolean

    fun getFile(name: String): FileSupport?

    fun isEmpty(): Boolean

    fun isDirectory(): Boolean

    fun isFile(): Boolean

    fun getFilesSize(): Int

    fun asText(charset: Charset = StandardCharsets.UTF_8): String =
        String(getInputStream().use { it.readBytes() }, charset)
}

interface DirectorySupport : FileSupport {}
