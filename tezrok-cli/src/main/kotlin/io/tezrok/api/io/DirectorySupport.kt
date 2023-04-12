package io.tezrok.api.io

interface DirectorySupport : FileSupport {
    fun getFilesSize(): Int

    fun getFiles(): List<FileSupport>

    fun addFile(name: String): FileSupport

    fun addDirectory(name: String): DirectorySupport

    fun removeFiles(names: List<String>): Boolean

    fun getFile(name: String): FileSupport?
}
