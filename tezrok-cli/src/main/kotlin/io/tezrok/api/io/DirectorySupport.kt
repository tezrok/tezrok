package io.tezrok.api.io

interface DirectorySupport : FileSupport {
    fun getFilesSize(): Int

    /**
     * Returns list of files and directories
     */
    fun getFiles(): List<FileSupport>

    fun addFile(name: String): FileSupport

    fun addDirectory(name: String): DirectorySupport

    fun removeFiles(names: List<String>): Boolean

    fun getFile(name: String): FileSupport? = getFiles().find { it.getName() == name }

    /**
     * Returns true if file or directory exists
     */
    fun hasFile(name: String): Boolean = getFile(name) != null
}
