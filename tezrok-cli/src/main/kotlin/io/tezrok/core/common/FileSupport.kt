package io.tezrok.core.common

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
}

interface DirectorySupport : FileSupport {
    override fun isDirectory(): Boolean

    override fun isFile(): Boolean
}
