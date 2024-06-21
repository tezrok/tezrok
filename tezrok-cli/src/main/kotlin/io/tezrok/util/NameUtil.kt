package io.tezrok.util

object NameUtil {
    fun validateWhitespaces(name: String): String {
        check(name.trim() == name) { "Name should not contain leading or trailing whitespaces: '$name'" }
        return name
    }
}
