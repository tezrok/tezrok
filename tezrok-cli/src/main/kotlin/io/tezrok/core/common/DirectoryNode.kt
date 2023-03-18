package io.tezrok.core.common

import java.io.InputStream
import java.io.OutputStream

/**
 * Represents a directory node
 *
 * Implements the [DirectorySupport] interface
 */
open class DirectoryNode(name: String, parent: BaseNode?) : FileNode(name, parent), DirectorySupport {
    override fun getOutputStream(): OutputStream {
        throw UnsupportedOperationException("Output stream not supported for this node")
    }

    override fun getInputStream(): InputStream {
        throw UnsupportedOperationException("Input stream not supported for this node")
    }
}
