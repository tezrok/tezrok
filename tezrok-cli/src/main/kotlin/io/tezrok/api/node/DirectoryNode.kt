package io.tezrok.api.node

import io.tezrok.api.io.DirectorySupport
import java.util.*

/**
 * Represents a directory node
 *
 * Implements the [DirectorySupport] interface
 */
open class DirectoryNode(name: String, parent: Node? = null) : BaseFileNode(name, parent), DirectorySupport {
    private val files: MutableList<BaseFileNode> = mutableListOf()

    override fun getFiles(): List<BaseFileNode> = Collections.unmodifiableList(files.toList())

    override fun addFile(name: String): FileNode {
        if (name.contains("/")) {
            val parts = name.split("/").filter { it.isNotEmpty() }
            val fileName = parts.lastOrNull() ?: throw IllegalArgumentException("File name cannot be empty")
            val finalParts = parts.dropLast(1)
            var dir: DirectoryNode = this
            finalParts.forEach { part ->
                dir = dir.getOrAddDirectory(part)
            }
            return dir.addFile(fileName)
        } else {
            check(files.none { it.getName() == name }) { "File with name '$name' already exists" }
            val file = FileNode(name, this)
            files.add(file)
            return file
        }
    }

    override fun addDirectory(name: String): DirectoryNode {
        if (name.contains("/")) {
            val parts = name.split("/").filter { it.isNotEmpty() }
            var dir: DirectoryNode = this
            parts.forEach { part ->
                dir = dir.getOrAddDirectory(part)
            }
            return dir
        } else {
            check(files.none { it.getName() == name }) { "Directory with name '$name' already exists" }
            val directory = DirectoryNode(name, this)
            files.add(directory)
            return directory
        }
    }

    override fun getFilesSize(): Int = files.size

    override fun getFile(name: String): BaseFileNode? {
        return if (name.contains("/")) {
            val parts = name.split("/").filter { it.isNotEmpty() }
            var dir: BaseFileNode? = this
            parts.forEach { part ->
                dir = dir?.getFile(part)
            }
            dir
        } else {
            files.find { it.getName() == name }
        }
    }

    override fun removeFiles(names: List<String>): Boolean = files.removeAll { it.getName() in names }

    fun getOrAddFile(name: String): FileNode = getFile(name) as? FileNode ?: addFile(name)

    fun getOrAddDirectory(name: String): DirectoryNode = getFile(name) as? DirectoryNode ?: addDirectory(name)

    override fun isDirectory(): Boolean = true
}
