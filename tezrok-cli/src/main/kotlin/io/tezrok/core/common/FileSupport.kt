package io.tezrok.core.common

interface FileSupport : OutStream, InStream {
    fun getFiles(): List<FileSupport>

    fun addFile(name: String): FileSupport

    fun addDirectory(name: String): FileSupport

    fun removeFiles(names: List<String>): Boolean

    fun isEmpty(): Boolean

    fun isDirectory(): Boolean = false

    fun isFile(): Boolean = true
}

interface DirectorySupport : FileSupport {
    override fun isDirectory(): Boolean = true

    override fun isFile(): Boolean = false
}
