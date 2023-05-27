package io.tezrok.api.io

import java.io.BufferedReader
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Represents a provider for input stream
 */
interface InStream {
    /**
     * Returns the input stream to read content
     */
    fun getInputStream(): InputStream

    /**
     * Reads the content of this resource as string
     */
    fun asString(charset: Charset = StandardCharsets.UTF_8): String =
            getInputStream().bufferedReader(charset).use(BufferedReader::readText)
}
