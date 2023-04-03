package io.tezrok.api

interface GeneratorProvider {
    fun <T> getGenerator(clazz: Class<T>): T? where T : TezrokGenerator<*, *>

    fun <T, R> getGenerator(clasFrom: Class<T>, classTo: Class<R>): TezrokGenerator<T, R>?
}
