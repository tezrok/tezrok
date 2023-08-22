package io.tezrok.util

/**
 * Convert an object to a JSON string
 */
fun Any.toJson(): String = JsonUtil.mapper.writeValueAsString(this)

/**
 * Convert an object to a pretty JSON string
 */
fun Any.toPrettyJson(): String = JsonUtil.mapper.writerWithDefaultPrettyPrinter()!!.writeValueAsString(this)

/**
 * Convert an object to a secured JSON string.
 *
 * Passwords and other sensitive information will be replaced with asterisks.
 */
fun Any.toSecuredPrettyJson(): String = this.toSecuredMap().toPrettyJson()

/**
 * Convert an object to a secured map.
 *
 * Passwords and other sensitive information will be replaced with asterisks.
 */
fun Any.toSecuredMap(): Map<String, Any?> {
    return secureMap(JsonUtil.mapper.convertValue(this, Map::class.java) as Map<String, Any?>)
}

private fun secureMap(map: Map<String, Any?>): Map<String, Any?> {
    val result = mutableMapOf<String, Any?>()
    for ((key, value) in map) {
        val isPassword = key.contains("password", ignoreCase = true) || key.contains("pwd", ignoreCase = true)
        if (value is Map<*, *>) {
            result[key] = secureMap(value as Map<String, Any?>)
        } else if (value is List<*>) {
            result[key] = secureList(value as List<Any?>, isPassword)
        } else {
            result[key] = if (isPassword) "******" else value
        }
    }

    return result
}

private fun secureList(list: List<Any?>, isPassword: Boolean): List<Any?> {
    val result = mutableListOf<Any?>()
    for (value in list) {
        if (value is Map<*, *>) {
            result.add(secureMap(value as Map<String, Any?>))
        } else if (value is List<*>) {
            result.add(secureList(value as List<Any?>, isPassword))
        } else {
            result.add(if (isPassword) "******" else value)
        }
    }

    return result
}
