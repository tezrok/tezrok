package io.tezrok.core.generator

import io.tezrok.api.Generator
import io.tezrok.api.GeneratorContext
import io.tezrok.api.model.maven.Pom
import io.tezrok.api.model.maven.Version
import io.tezrok.api.model.node.ModuleNode
import io.tezrok.core.builder.PomBuilder
import io.tezrok.core.error.TezrokException
import java.lang.Exception

class MavenGenerator(private val context: GeneratorContext) : Generator {
    private var pom: Pom? = null

    override fun generate() {
        val module = context.module

        if (pom == null) {
            pom = Pom(module.toMavenVersion(),
                    type = "",
                    properties = mutableListOf(),
                    dependencies = mutableListOf()
            )
        }

        populatePom(pom!!)

        val builder = PomBuilder(pom!!, context)

        context.render(builder)
    }

    private fun populatePom(pom: Pom) {
        context.mavenVisitors.forEach { visitor ->
            try {
                visitor.visit(pom)
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
