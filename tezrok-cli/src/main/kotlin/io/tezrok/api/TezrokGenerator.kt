package io.tezrok.api

/**
 * Generate or get one object [R] from another [T]
 */
interface TezrokGenerator<T, R> {
    fun getFrom(): Class<T>

    fun getTo(): Class<R>

    fun generate(from: T, context: GeneratorContext): R
}
