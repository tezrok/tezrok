package io.tezrok.util

object NameUtil {
    fun validateWhitespaces(name: String): String {
        check(name.trim() == name) { "Name should not contain leading or trailing whitespaces: '$name'" }
        return name
    }

    /**
     * Converts a name from camelCase to standard name
     *
     * Example: `camelCase` -> `camel-case`
     */
    fun toHyphenName(name: String): String = regexCamelCase.replace(name, "$1-$2").lowercase()

    private val regexCamelCase = Regex("([a-z])([A-Z]+)")
}
