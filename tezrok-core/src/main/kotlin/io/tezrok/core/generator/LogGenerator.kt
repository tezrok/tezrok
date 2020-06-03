package io.tezrok.core.generator

import io.tezrok.api.ExecuteContext
import io.tezrok.api.Generator
import io.tezrok.api.Phase
import io.tezrok.api.builder.JMod
import io.tezrok.api.builder.JavaClassBuilder
import io.tezrok.api.model.maven.Dependency
import io.tezrok.api.model.maven.Pom
import io.tezrok.api.visitor.EachClassVisitor
import io.tezrok.api.visitor.MavenVisitor
import io.tezrok.core.builder.LogbackConfigBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Creates log-config and add log field to each class
 */
class LogGenerator : Generator, MavenVisitor, EachClassVisitor {
    override fun execute(context: ExecuteContext) {
        if (context.phase == Phase.Generate) {
            val builder = LogbackConfigBuilder(context)

            context.render(builder)
        }
    }

    override fun visit(pom: Pom) {
        pom.add(Dependency("ch.qos.logback", "logback-classic", "1.2.3"))
    }

    override fun visit(clazz: JavaClassBuilder) {
        if (!clazz.hasField(FIELD_NAME)) {
            val field = clazz.field(FIELD_NAME, Logger::class.java, JMod.PRIVATE or JMod.FINAL or JMod.STATIC)
            field.value = "LoggerFactory.getLogger(${clazz.name}.class)"
            clazz.addImports(LoggerFactory::class.java)
        }
    }

    companion object {
        const val FIELD_NAME = "log"
    }
}
