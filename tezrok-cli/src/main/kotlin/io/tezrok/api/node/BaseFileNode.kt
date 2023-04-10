package io.tezrok.api.node

import java.io.InputStream
import java.io.OutputStream

/**
 * Base implementation for [FileNode] and [DirectoryNode]
 */
abstract class BaseFileNode(name: String, parent: BaseNode?) : BaseNode(name, parent), DirectorySupport {
    override fun isDirectory(): Boolean = false

    override fun isFile(): Boolean = false

    override fun getFiles(): List<BaseFileNode> = NOT_SUPPORTED()

    override fun addFile(name: String): BaseFileNode = NOT_SUPPORTED()

    override fun addDirectory(name: String): BaseFileNode = NOT_SUPPORTED()

    override fun removeFiles(names: List<String>): Boolean = NOT_SUPPORTED()

    override fun getFile(name: String): BaseFileNode? = NOT_SUPPORTED()

    override fun isEmpty(): Boolean = NOT_SUPPORTED()

    override fun getFilesSize(): Int = NOT_SUPPORTED()

    override fun getOutputStream(): OutputStream = NOT_SUPPORTED()

    override fun getInputStream(): InputStream = NOT_SUPPORTED()

    private fun NOT_SUPPORTED(): Nothing {
        throw UnsupportedOperationException("Not supported for " + this.javaClass.simpleName)
    }
}
