package io.tezrok.core.util

object PackageUtil {
    fun concat(vararg packageParts: String) = packageParts.filter { it.isNotBlank() }.joinToString(separator = ".")
}
