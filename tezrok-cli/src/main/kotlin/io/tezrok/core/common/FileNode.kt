package io.tezrok.core.common

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*

/**
 * Node that supports file operations. Class is thread safe.
 *
 * Implements [FileSupport] for nodes
 */
open class FileNode(name: String, parent: BaseNode?) : BaseNode(name, parent), FileSupport {
    private val files: MutableList<FileNode> = mutableListOf()
    private var content: ByteArray = EMPTY_ARRAY

    override fun getParent(): BaseNode? = super.getParent()

    override fun getFiles(): List<FileNode> = Collections.unmodifiableList(files)

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

    @Synchronized
    override fun removeFiles(names: List<String>): Boolean =
        files.removeAll { it.getName() in names }

    override fun isEmpty(): Boolean = content.isEmpty()

    override fun getOutputStream(): OutputStream {
        return object : ByteArrayOutputStream() {
            override fun close() {
                super.close()
                val newContent = toByteArray()
                if (log.isTraceEnabled) {
                    log.trace("Write {} bytes to file {}", newContent.size, getPath())
                }
                synchronized(this@FileNode) {
                    content = newContent
                }
            }
        }
    }

    @Synchronized
    override fun getInputStream(): InputStream = content.clone().inputStream()

    private companion object {
        val EMPTY_ARRAY = ByteArray(0)
    }
}