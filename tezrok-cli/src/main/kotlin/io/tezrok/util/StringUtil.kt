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
 * Convert camel case string to lower snake case
 *
 * Example: "FooBar" -> "foo_bar"
 */
fun String.camelCaseToLowerSnakeCase(): String = camelCaseToSnakeCase().lowercase()

/**
 * Convert camel case string to sql case
 *
 * Example: "FooBar" -> "foo_bar"
 */
fun String.camelCaseToSqlCase(): String = camelCaseToSnakeCase().lowercase()

/**
 * Convert camel case string to sql upper case
 *
 * Example: "FooBar" -> "FOO_BAR"
 */
fun String.camelCaseToSqlUppercase(): String = camelCaseToSnakeCase().uppercase()

/**
 * Convert first char to lower case
 */
fun String.lowerFirst(): String = replaceFirstChar { it.lowercase() }

/**
 * Convert first char to upper case
 */
fun String.upperFirst(): String = replaceFirstChar { it.uppercase() }

/**
 * Converts a name from camelCase to standard name
 *
 * Example: `camelCase` -> `camel-case`
 */
fun String.toHyphenName(): String = camelCaseRegex.replace(this, "$1-$2").lowercase()

/**
 * Converts a name from camelCase to standard name
 *
 * Example: `camel-case` -> `camelCase`
 */
fun String.hyphenNameToCamelCase(): String = hyphenNameRegex.replace(this) {
    it.groupValues[1] + it.groupValues[3].uppercase()
}

private val camelCaseRegex = Regex("([a-z\\d])([A-Z]+)")

private val hyphenNameRegex = Regex("([a-z\\d])(-)([a-z])")
