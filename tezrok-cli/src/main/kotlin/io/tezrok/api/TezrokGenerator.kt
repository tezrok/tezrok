package io.tezrok.api

/**
 * Generate one object from another
 */
interface TezrokGenerator<T, R> {
    fun generate(from: T, context: GeneratorContext): R
}
