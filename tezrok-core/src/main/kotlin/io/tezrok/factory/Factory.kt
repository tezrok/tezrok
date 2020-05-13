package io.tezrok.factory

/**
 * Creates instances of all classes
 */
interface Factory {
    fun <T> create(clazz: Class<T>): T
}
