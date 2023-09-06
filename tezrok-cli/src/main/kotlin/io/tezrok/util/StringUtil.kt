package io.tezrok.util

import java.nio.file.Path

/**
 * Get path to a resource
 */
fun String.resourceAsPath(): Path = ResourceUtil.getResourceAsPath(this)

fun String.resourceAsString(): String = ResourceUtil.getResourceAsString(this)

/**
 * Convert camel case string to snake case
 *
 * Example: "FooBar" -> "Foo_Bar"
 */
fun String.camelCaseToSnakeCase(): String = camelCaseRegex.replace(this, "$1_$2")

/**
 * Convert camel case string to sql case
 *
 * Example: "FooBar" -> "foo_bar"
 */
fun String.camelCaseToSqlCase(): String = camelCaseToSnakeCase().lowercase()

private val camelCaseRegex = Regex("([a-z\\d])([A-Z]+)")
