package io.tezrok.util

/**
 * Convert an object to a JSON string
 */
fun Any.toJson(): String = JsonUtil.mapper.writeValueAsString(this)

/**
 * Convert an object to a pretty JSON string
 */
fun Any.toPrettyJson(): String = JsonUtil.mapper.writerWithDefaultPrettyPrinter()!!.writeValueAsString(this)
