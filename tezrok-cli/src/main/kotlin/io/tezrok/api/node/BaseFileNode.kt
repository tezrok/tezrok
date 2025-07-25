package io.tezrok.api.node

import io.tezrok.api.io.DirectorySupport
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Base implementation for [FileNode] and [DirectoryNode]
 */
abstract class BaseFileNode(name: String, parent: Node?) : BaseNode(name, parent), DirectorySupport {
    override fun isDirectory(): Boolean = false

    override fun isFile(): Boolean = false

    override fun getSize(): Long = 0

    override fun getFilesSize(): Int = 0

    /**
     * Returns true if file node is empty
     */
    override fun isEmpty(): Boolean = true

    override fun getChildren(): List<BaseFileNode> = getFiles()

    override fun getFiles(): List<BaseFileNode> = NOT_SUPPORTED()

    override fun addFile(name: String): BaseFileNode = NOT_SUPPORTED()

    override fun addDirectory(name: String): BaseFileNode = NOT_SUPPORTED()

    override fun removeFiles(names: List<String>): Boolean = NOT_SUPPORTED()

    override fun getFile(name: String): BaseFileNode? = NOT_SUPPORTED()

    override fun getOutputStream(): OutputStream = NOT_SUPPORTED()

    override fun getInputStream(): InputStream = NOT_SUPPORTED()

    override fun getPhysicalPath(): Path? {
        // find first ancestor that has physical path
        val ancestor = getFirstAncestor { it is BaseFileNode && it.getPhysicalPath() != null } as BaseFileNode?

        if (ancestor != null) {
            val ancestorPath = ancestor.getPhysicalPath()!!
            return Paths.get(ancestorPath.toString(), getPathTo(ancestor))
        }

        return null
    }

    private fun NOT_SUPPORTED(): Nothing {
        throw UnsupportedOperationException("Not supported for " + this.javaClass.simpleName)
    }
}
