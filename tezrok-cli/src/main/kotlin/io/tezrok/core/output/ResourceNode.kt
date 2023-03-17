package io.tezrok.core.output

/**
 * Represents a resource directory
 */
class ResourceNode {
    private val files: MutableList<FileNode> = mutableListOf()

    fun getFiles(): List<FileNode> = files
}
