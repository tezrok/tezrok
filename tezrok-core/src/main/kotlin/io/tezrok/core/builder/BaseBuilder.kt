package io.tezrok.core.builder

import io.tezrok.api.GeneratorContext
import io.tezrok.api.builder.Builder
import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import java.io.Writer
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

abstract class BaseBuilder(val context: GeneratorContext) : Builder {
    override fun build(writer: Writer) {
        val velContext = VelocityContext()

        if (context.isGenerateTime) {
            velContext.put("generateTime", OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        }

        onBuild(velContext)

        getTemplate().merge(velContext, writer)
    }

    protected abstract fun onBuild(context: VelocityContext)

    protected abstract fun getTemplate(): Template

    override fun init() {
    }

    override fun isCustomCode(): Boolean = false
}
