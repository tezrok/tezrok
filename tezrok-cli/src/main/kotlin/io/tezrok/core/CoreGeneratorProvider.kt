package io.tezrok.core

import io.tezrok.api.GeneratorProvider
import io.tezrok.api.TezrokGenerator
import io.tezrok.api.annotations.KnownGenerator
import io.tezrok.sql.CoreSqlGenerator

internal class CoreGeneratorProvider : GeneratorProvider {
    private val generators = mutableMapOf<Class<*>, TezrokGenerator<*, *>>()
    private val generatorsFromTo = mutableMapOf<Pair<Class<*>, Class<*>>, TezrokGenerator<*, *>>()

    init {
        // TODO: load generators from configuration
        addGenerator(CoreSqlGenerator())
    }

    fun <T, R> addGenerator(generator: TezrokGenerator<T, R>) {
        addGenerator(generator.getFrom(), generator.getTo(), generator)
    }

    fun <T, R> addGenerator(classFrom: Class<T>, classTo: Class<R>, generator: TezrokGenerator<T, R>) {
        // TODO: check if the generator is already registered
        generators[getClass(generator)] = generator
        generatorsFromTo[Pair(classFrom, classTo)] = generator
    }

    private fun getClass(generator: TezrokGenerator<*, *>): Class<*> =
        getKnownGeneratorClass(generator.javaClass) ?: generator.javaClass

    /**
     * Returns the class of the generator if it is annotated with [KnownGenerator]
     */
    private fun getKnownGeneratorClass(clazz: Class<*>): Class<*>? {
        if (clazz.isAnnotationPresent(KnownGenerator::class.java)) {
            return clazz
        }

        return clazz.interfaces
            .firstNotNullOfOrNull { getKnownGeneratorClass(it) } ?: getKnownGeneratorClass(clazz.superclass)
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
