package io.tezrok.core

import io.tezrok.api.GeneratorContext
import io.tezrok.api.GeneratorProvider
import io.tezrok.api.TezrokGenerator
import io.tezrok.api.input.ProjectElem
import io.tezrok.util.VelocityUtil
import org.apache.velocity.VelocityContext
import java.io.Writer
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer

internal class CoreGeneratorContext(
    private val project: ProjectElem,
    private val generatorProvider: GeneratorProvider = NullGeneratorProvider,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val generateTime: Boolean = true,
    private val authorLogin: String = "TezrokUser"
) : GeneratorContext {
    override fun getAuthorLogin(): String = authorLogin

    override fun getClock(): Clock = clock

    override fun getProject(): ProjectElem = project

    override fun isGenerateTime(): Boolean = generateTime

    override fun writeTemplate(writer: Writer, templatePath: String, contextInitializer: Consumer<VelocityContext>) {
        val masterTemplate = VelocityUtil.getTemplate(templatePath)
        val velocityContext = VelocityContext()

        if (isGenerateTime()) {
            velocityContext.put(
                "generateTime",
                LocalDateTime.now(getClock()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        }
        contextInitializer.accept(velocityContext)
        masterTemplate.merge(velocityContext, writer)
    }

    override fun <T : TezrokGenerator<*, *>> getGenerator(clazz: Class<T>): T? =
        generatorProvider.getGenerator(clazz)

    override fun <T, R> getGenerator(clasFrom: Class<T>, classTo: Class<R>): TezrokGenerator<T, R>? =
        generatorProvider.getGenerator(clasFrom, classTo)
}
