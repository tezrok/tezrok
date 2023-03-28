package io.tezrok.api.node

import java.io.InputStream

/**
 * Represents a provider for input stream
 */
interface InStream {
    /**
     * Returns the input stream to read content
     */
    fun getInputStream(): InputStream
}
