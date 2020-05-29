package io.tezrok.core.generator

import io.tezrok.api.ExecuteContext
import io.tezrok.api.Generator
import io.tezrok.api.Phase
import io.tezrok.api.model.maven.Pom
import io.tezrok.api.model.maven.Version
import io.tezrok.api.model.node.ModuleNode
import io.tezrok.api.visitor.MavenVisitor
import io.tezrok.core.service.MavenVisitorsProvider
import io.tezrok.core.builder.PomBuilder
import io.tezrok.core.error.TezrokException
import org.slf4j.LoggerFactory
import java.lang.Exception

/**
 * Generates pom.xml
 */
class MavenGenerator : Generator {
    private val log = LoggerFactory.getLogger(javaClass)
    private val poms = mutableMapOf<ModuleNode, Pom>()

    override fun execute(context: ExecuteContext) {
        val module = context.getModule()

        if (context.getPhase() == Phase.Init) {
            poms.remove(context.getModule())
        }

        val pom = poms.computeIfAbsent(context.getModule()) {
            Pom(module.toMavenVersion(),
                    type = "",
                    properties = mutableListOf(),
                    dependencies = mutableListOf()
            )
        }

        populatePom(pom, context)

        if (context.getPhase() == Phase.Generate) {
            val builder = PomBuilder(pom, context)

            context.render(builder)
        }
    }

    private fun populatePom(pom: Pom, context: ExecuteContext) {
        context.getMavenVisitors().forEach { visitor ->
            try {
                log.debug("Begin Maven visitor {}", visitor.javaClass.name)

                visitor.visit(pom)

                log.debug("End Maven visitor {}", visitor.javaClass.name)
            } catch (e: Exception) {
                throw TezrokException("Maven visitor (${visitor.javaClass.name}) failed: ${e.message}", e)
            }
        }
    }
}

fun ModuleNode.toMavenVersion(): Version {
    return Version(groupId = packagePath,
            artifactId = name.toLowerCase().replace(' ', '-'),
            version = version)
}

fun ExecuteContext.getMavenVisitors(): List<MavenVisitor> {
    return getInstance(MavenVisitorsProvider::class.java).visitors
}
