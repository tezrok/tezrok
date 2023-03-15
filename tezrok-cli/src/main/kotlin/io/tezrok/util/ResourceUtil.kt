package io.tezrok.util

object ResourceUtil {
    /**
     * Reads a resource as a string
     */
    fun getResourceAsString(path: String): String {
        return javaClass.getResource(path)?.readText() ?: throw Exception("Resource not found: $path")
    }
}
