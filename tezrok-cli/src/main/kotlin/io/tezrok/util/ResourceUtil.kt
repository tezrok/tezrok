package io.tezrok.util

import java.nio.file.Path
import kotlin.io.path.toPath

object ResourceUtil {
    /**
     * Reads a resource as a string
     */
    fun getResourceAsString(path: String): String {
        return javaClass.getResource(path)?.readText() ?: throw Exception("Resource not found: $path")
    }

    fun getResourceAsPath(path: String): Path =
        javaClass.getResource(path)?.toURI()?.toPath() ?: throw Exception("Resource not found: $path")
}
