package io.tezrok.api.node

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

interface DirectorySupport : FileSupport {
    fun getFilesSize(): Int

    fun getFiles(): List<FileSupport>

    fun addFile(name: String): FileSupport

    fun addDirectory(name: String): DirectorySupport

    fun removeFiles(names: List<String>): Boolean

    fun getFile(name: String): FileSupport?
}
