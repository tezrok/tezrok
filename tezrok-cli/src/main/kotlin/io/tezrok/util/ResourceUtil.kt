package io.tezrok.util

import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.toPath

object ResourceUtil {
    /**
     * Reads a resource as a string
     */
    fun getResourceAsString(path: String): String {
        return javaClass.getResource(path)?.readText() ?: throw Exception("Resource not found: $path")
    }

    fun getResourceAsLines(path: String): List<String> {
        return javaClass.getResource(path)?.toURI()?.toPath()?.toFile()?.readLines()
            ?: throw Exception("Resource not found: $path")
    }

    fun getResourceAsPath(path: String): Path =
        javaClass.getResource(path)?.toURI()?.toPath() ?: throw Exception("Resource not found: $path")

    fun getResourceAsStream(path: String): InputStream = javaClass.getResource(path)?.openStream()
        ?: throw Exception("Resource not found: $path")
}
