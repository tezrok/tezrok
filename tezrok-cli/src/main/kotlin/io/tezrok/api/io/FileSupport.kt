package io.tezrok.api.io

import java.nio.file.Path

interface FileSupport : OutStream, InStream {
    fun getName(): String

    fun getSize(): Long

    fun isDirectory(): Boolean

    fun isFile(): Boolean

    fun isEmpty(): Boolean

    fun isNotEmpty(): Boolean = !isEmpty()

    /**
     * Returns physical path to file or directory, if possible
     */
    fun getPhysicalPath(): Path?
}
