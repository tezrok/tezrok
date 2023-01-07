package com.tezrok.api.tree

import com.tezrok.api.TezrokService

/**
 * Converter for properties from and to string
 */
interface PropertyValue : TezrokService {
    fun fromString(value: String): Any?

    fun asString(value: Any): String
}
