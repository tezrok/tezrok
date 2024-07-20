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

    var strategy: StoreStrategy = StoreStrategy.SAVE

    override fun isEmpty(): Boolean = content.isEmpty()

    override fun getSize(): Long = content.size.toLong()

    override fun isFile(): Boolean = true

    override fun getOutputStream(): OutputStream {
        return object : ByteArrayOutputStream() {
            override fun close() {
                val newContent = toByteArray()
                if (log.isTraceEnabled) {
                    log.trace("Write {} bytes to file {}", newContent.size, getPath())
                }
                setContent(newContent)
                super.close()
            }
        }
    }

    override fun getInputStream(): InputStream = content.clone().inputStream()

    protected open fun setContent(content: ByteArray) {
        this.content = content
    }

    private companion object {
        val EMPTY_ARRAY = ByteArray(0)
    }
}
