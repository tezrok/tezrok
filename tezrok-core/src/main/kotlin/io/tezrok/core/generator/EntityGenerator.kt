package io.tezrok.core.generator

import io.tezrok.api.ExecuteContext
import io.tezrok.api.Generator
import io.tezrok.api.Phase
import io.tezrok.api.builder.JMod
import io.tezrok.api.builder.JavaClassBuilder
import io.tezrok.api.model.node.EntityNode
import io.tezrok.api.service.CodeService
import io.tezrok.api.visitor.EntityClassVisitor
import io.tezrok.core.error.TezrokException
import io.tezrok.core.service.EntityClassVisitorsProvider
import org.slf4j.LoggerFactory
import java.io.Serializable

class EntityGenerator : Generator {
    override fun execute(context: ExecuteContext) {
        if (context.phase == Phase.Generate) {
            val codeGen = context.getInstance(CodeService::class.java)
            val visitors = context.getEntityClassVisitors()

            context.module.entities().forEach { entity ->
                val entityClass = createClazz(codeGen, context, entity)

                visitors.forEach { visitor ->
                    try {
                        log.debug("Begin EntityClass visitor {}", visitor.javaClass.name)

                        visitor.visit(entityClass)

                        log.debug("End EntityClass visitor {}", visitor.javaClass.name)
                    } catch (e: Exception) {
                        throw TezrokException("EntityClass visitor (${visitor.javaClass.name}) failed: ${e.message}", e)
                    }
                }

                context.render(entityClass)
            }
        }
    }

    private fun createClazz(codeGen: CodeService, context: ExecuteContext, entity: EntityNode): JavaClassBuilder {
        val clazz = codeGen.createClass(context.ofType(entity.name, "entity"), JMod.PUBLIC)

        clazz.addImplements(Serializable::class.java)

        if (entity.description.isNotBlank()) {
            clazz.comment(entity.description)
        }

        for (fieldNode in entity.fields()) {
            var mod = JMod.PRIVATE or JMod.GETSET

            if (fieldNode.primary) {
                mod = mod or JMod.USEEQUALS
            }

            clazz.field(fieldNode.name, context.resolveType(fieldNode.type), mod)
        }

        return clazz
    }

    companion object {
        private val log = LoggerFactory.getLogger(EntityGenerator::class.java)
    }
}

fun ExecuteContext.getEntityClassVisitors(): Set<EntityClassVisitor> {
    return getInstance(EntityClassVisitorsProvider::class.java).visitors
}
