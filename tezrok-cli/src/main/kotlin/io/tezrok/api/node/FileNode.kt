package io.tezrok.api.node

import io.tezrok.api.io.FileSupport
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Node that supports file operations. Class is thread safe.
 *
 * Implements [FileSupport] for nodes
 */
open class FileNode(name: String, parent: Node?) : BaseFileNode(name, parent), FileSupport {
    private var content: ByteArray = EMPTY_ARRAY

    override fun isEmpty(): Boolean = content.isEmpty()

    override fun getSize(): Long = content.size.toLong()

    override fun isFile(): Boolean = true

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
