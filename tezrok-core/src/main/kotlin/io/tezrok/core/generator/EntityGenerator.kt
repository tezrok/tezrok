package io.tezrok.core.generator

import io.tezrok.api.ExecuteContext
import io.tezrok.api.Generator
import io.tezrok.api.Phase
import io.tezrok.api.builder.JMod
import io.tezrok.api.builder.JavaClassBuilder
import io.tezrok.api.model.node.EntityNode
import io.tezrok.api.service.CodeService
import io.tezrok.api.visitor.EntityClassVisitor
import org.slf4j.LoggerFactory
import java.io.Serializable

class EntityGenerator : Generator {
    override fun execute(context: ExecuteContext) {
        if (context.phase == Phase.Generate) {
            val codeGen = context.getInstance(CodeService::class.java)

            context.module.entities().forEach { entity ->
                val entityClass = createClazz(codeGen, context, entity)

                context.applyVisitors(EntityClassVisitor::class.java) { visitor ->
                    visitor.visit(entityClass, entity)
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
