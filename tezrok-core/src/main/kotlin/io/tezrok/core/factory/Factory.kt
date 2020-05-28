package io.tezrok.core.factory

import io.tezrok.api.Generator

/**
 * Creates instances of all classes
 */
interface Factory {
    fun <T> getInstance(clazz: Class<T>): T

    fun getGenerator(className: String): Generator
}
