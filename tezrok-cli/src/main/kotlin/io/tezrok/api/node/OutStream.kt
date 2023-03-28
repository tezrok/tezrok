package io.tezrok.api.node

import java.io.OutputStream

/**
 * Represents a provider for output stream
 */
interface OutStream {
    /**
     * Returns the output stream to write content
     */
    fun getOutputStream(): OutputStream
}
