package io.tezrok.core

import io.tezrok.api.GeneratorProvider
import io.tezrok.api.TezrokGenerator

internal class CoreGeneratorProvider : GeneratorProvider {
    private val generators = mutableMapOf<Class<*>, TezrokGenerator<*, *>>()
    private val generatorsFromTo = mutableMapOf<Pair<Class<*>, Class<*>>, TezrokGenerator<*, *>>()

    fun <T, R> addGenerator(classFrom: Class<T>, classTo: Class<R>, generator: TezrokGenerator<T, R>) {
        // TODO: check if the generator is already registered
        generators[generator.javaClass] = generator
        generatorsFromTo[Pair(classFrom, classTo)] = generator
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : TezrokGenerator<*, *>> getGenerator(clazz: Class<T>): T? =
        generators[clazz] as T?

    @Suppress("UNCHECKED_CAST")
    override fun <T, R> getGenerator(clasFrom: Class<T>, classTo: Class<R>): TezrokGenerator<T, R>? =
        generatorsFromTo[Pair(clasFrom, classTo)] as TezrokGenerator<T, R>?
}

/**
 * A generator provider that returns null for all generators
 */
object NullGeneratorProvider : GeneratorProvider {
    override fun <T : TezrokGenerator<*, *>> getGenerator(clazz: Class<T>): T? = null

    override fun <T, R> getGenerator(clasFrom: Class<T>, classTo: Class<R>): TezrokGenerator<T, R>? = null
}
