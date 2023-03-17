package io.tezrok.core.output

import java.io.OutputStream

/**
 * Represents a file or directory
 */
open class FileNode(private val name: String) : BaseNode {
    override fun getName(): String = name

    fun getOutputStream(): OutputStream {
        TODO()
    }

    fun getInputStream(): OutputStream {
        TODO()
    }
}

class DirectoryNode(val name: String) : FileNode(name) {
    private val files: MutableList<FileNode> = mutableListOf()

    fun getFiles(): List<FileNode> = files

    fun addFile(name: String) {
        files.add(FileNode(name))
    }

    fun removeFiles(list: List<String>): Boolean {
        TODO()
    }
}
