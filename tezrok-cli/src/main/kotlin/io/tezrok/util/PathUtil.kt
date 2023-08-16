package io.tezrok.util

import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths

fun Path.toURL(): URL = toUri().toURL()

fun Path.mkdirs(): Boolean = toFile().mkdirs()

object PathUtil {
    /**
     * Returns the current directory
     */
    fun currentDir(): Path = Paths.get("").toAbsolutePath()

    /**
     * Resolves the given path against the current directory
     */
    fun resolve(path: String): Path = currentDir().resolve(path)

    val NEW_LINE: String = System.lineSeparator()!!
}
