package io.tezrok.api.io

interface FileSupport : OutStream, InStream {
    fun getSize(): Long

    fun isDirectory(): Boolean

    fun isFile(): Boolean

    fun isEmpty(): Boolean

    fun isNotEmpty(): Boolean = !isEmpty()
}
