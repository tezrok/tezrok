package io.tezrok.util

import java.nio.file.Path

/**
 * Get path to a resource
 */
fun String.resourceAsPath(): Path = ResourceUtil.getResourceAsPath(this)

fun String.resourceAsString(): String = ResourceUtil.getResourceAsString(this)

fun String.camelCaseToSnakeCase(): String = camelCaseRegex.replace(this, "$1_$2").lowercase()

private val camelCaseRegex = Regex("([a-z])([A-Z]+)")
