package io.tezrok.core.output

import io.tezrok.core.common.BaseNode

/**
 * Represents a resource directory
 */
class ResourceNode : BaseNode("resources") {
    private val files: MutableList<FileNode> = mutableListOf()

    fun getFiles(): List<FileNode> = files

    fun addFile(name: String): FileNode {
        // TODO: check if file already exists
        // TODO: validate file name
        val file = FileNode(name)
        files.add(file)
        return file
    }

    fun addDirectory(name: String): DirectoryNode {
        // TODO: check if file already exists
        // TODO: validate file name
        val directory = DirectoryNode(name)
        files.add(directory)
        return directory
    }

    override fun setName(name: String) = throw UnsupportedOperationException("Cannot set name for resource node")
}
