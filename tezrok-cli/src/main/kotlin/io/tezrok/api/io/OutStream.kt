package io.tezrok.api.io

import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Represents a provider for output stream
 */
interface OutStream {
    /**
     * Returns the output stream to write content
     */
    fun getOutputStream(): OutputStream

    /**
     * Writes the string content to this resource
     */
    fun setString(content: String, charset: Charset = StandardCharsets.UTF_8) =
        getOutputStream().bufferedWriter(charset).use { it.write(content) }
}
