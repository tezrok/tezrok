package com.tezrok.api

import java.io.InputStream
import java.io.OutputStream

/**
 * Reference to file
 */
interface FileRef : TezrokService {
    /**
     * Returns hash of file content (SHA-1)
     *
     * Hash changed only when opened OutputStream is closed
     */
    fun getHash(): String

    /**
     * Returns name of the file
     */
    fun getName(): String

    /**
     * Returns absolute path
     */
    fun getPath(): String

    /**
     * Returns content type (MIME)
     */
    fun getContentType(): String

    /**
     * Returns new stream to read
     */
    fun openInputSteam(): InputStream

    /**
     * Returns new stream to write
     */
    fun openOutputStream(): OutputStream
}
