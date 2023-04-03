package io.tezrok.core

import io.tezrok.api.GeneratorContext
import io.tezrok.api.GeneratorProvider
import io.tezrok.api.TezrokGenerator
import io.tezrok.core.input.ProjectElem
import java.time.Clock

internal class CoreGeneratorContext(
    private val project: ProjectElem,
    private val generatorProvider: GeneratorProvider = NullGeneratorProvider,
    private val clock: Clock = Clock.systemDefaultZone()
) : GeneratorContext {
    override fun getAuthor(): String = "TezrokUser"

    override fun getClock(): Clock = clock

    override fun getProject(): ProjectElem = project

    override fun <T : TezrokGenerator<*, *>> getGenerator(clazz: Class<T>): T? =
        generatorProvider.getGenerator(clazz)

    override fun <T, R> getGenerator(clasFrom: Class<T>, classTo: Class<R>): TezrokGenerator<T, R>? =
        generatorProvider.getGenerator(clasFrom, classTo)
}
