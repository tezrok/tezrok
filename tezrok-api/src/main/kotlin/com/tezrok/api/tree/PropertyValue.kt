package com.tezrok.api.tree

import com.tezrok.api.TezrokService

/**
 * Converter for properties from and to string
 */
interface PropertyValue<T> : TezrokService {
    fun fromString(value: String): T

    fun asString(): String
}
