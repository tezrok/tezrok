package io.tezrok.util

import java.nio.file.Path

/**
 * Get path to a resource
 */
fun String.resourceAsPath(): Path = ResourceUtil.getResourceAsPath(this)

fun String.resourceAsString(): String = ResourceUtil.getResourceAsString(this)
