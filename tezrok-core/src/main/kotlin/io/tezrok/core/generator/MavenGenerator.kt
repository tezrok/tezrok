package io.tezrok.core.generator

import io.tezrok.api.ExecuteContext
import io.tezrok.api.Generator
import io.tezrok.api.Phase
import io.tezrok.api.model.maven.Pom
import io.tezrok.api.model.maven.Version
import io.tezrok.api.model.node.ModuleNode
import io.tezrok.api.visitor.MavenVisitor
import io.tezrok.core.builder.PomBuilder
import org.slf4j.LoggerFactory

/**
 * Generates maven related files (pom.xml and other)
 */
class MavenGenerator : Generator {
    private val log = LoggerFactory.getLogger(javaClass)
    private val poms = mutableMapOf<ModuleNode, Pom>()

    override fun execute(context: ExecuteContext) {
        val module = context.module

        if (context.phase == Phase.Init) {
            poms.remove(module)
        }

        val pom = poms.computeIfAbsent(module) {
            Pom(module.toMavenVersion(),
                    type = "",
                    properties = mutableListOf(),
                    dependencies = mutableListOf()
            )
        }

        context.applyVisitors(MavenVisitor::class.java) { visitor ->
            visitor.visit(pom)
        }

        if (context.phase == Phase.Generate) {
            val builder = PomBuilder(pom, context)

            context.render(builder)
        }
    }
}

fun ModuleNode.toMavenVersion(): Version {
    return Version(groupId = packagePath,
            artifactId = name.toLowerCase().replace(' ', '-'),
            version = version)
}
