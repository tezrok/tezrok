package io.tezrok.api.node

import io.tezrok.api.io.DirectorySupport
import java.util.*

/**
 * Represents a directory node
 *
 * Implements the [DirectorySupport] interface
 */
open class DirectoryNode(name: String, parent: BaseNode? = null) : BaseFileNode(name, parent), DirectorySupport {
    private val files: MutableList<BaseFileNode> = mutableListOf()

    @Synchronized
    override fun getFiles(): List<BaseFileNode> = Collections.unmodifiableList(files.toList())

    @Synchronized
    override fun addFile(name: String): FileNode {
        //TODO: check if file already exists
        val file = FileNode(name, this)
        files.add(file)
        return file
    }

    @Synchronized
    override fun addDirectory(name: String): DirectoryNode {
        //TODO: check if file already exists
        val directory = DirectoryNode(name, this)
        files.add(directory)
        return directory
    }

    override fun getFilesSize(): Int = files.size

    @Synchronized
    override fun getFile(name: String): BaseFileNode? = files.find { it.getName() == name }

    @Synchronized
    override fun removeFiles(names: List<String>): Boolean = files.removeAll { it.getName() in names }

    @Synchronized
    fun getOrAddFile(name: String): FileNode = getFile(name) as? FileNode ?: addFile(name)

    @Synchronized
    fun getOrAddDirectory(name: String): DirectoryNode = getFile(name) as? DirectoryNode ?: addDirectory(name)

    override fun isDirectory(): Boolean = true
}
