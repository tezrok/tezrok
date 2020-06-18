package io.tezrok.spring.generator

import io.tezrok.api.ExecuteContext
import io.tezrok.api.Generator
import io.tezrok.api.builder.JavaClassBuilder
import io.tezrok.api.builder.JavaField
import io.tezrok.api.model.node.EntityNode
import io.tezrok.api.model.node.FieldNode
import io.tezrok.api.model.node.ProjectNode
import io.tezrok.api.visitor.EntityClassVisitor
import io.tezrok.api.visitor.LogicModelVisitor
import io.tezrok.api.visitor.ModelPhase
import io.tezrok.spring.util.NameUtil
import org.slf4j.LoggerFactory
import javax.persistence.Entity
import javax.persistence.Id

/**
 * Generates jpa-repositories
 */
class SpringJPAGenerator : Generator, EntityClassVisitor, LogicModelVisitor {
    override fun visit(project: ProjectNode, phase: ModelPhase) {
        if (phase == ModelPhase.PostEdit) {
            // TODO: build entity relations
        }
    }

    override fun execute(context: ExecuteContext) {
        log.warn("execute method not implemented")
    }

    override fun visit(clazz: JavaClassBuilder, node: EntityNode) {
        val tableName = NameUtil.getTableName(node)

        clazz.annotate(Entity::class.java)
                .annotate("Table(name = \"$tableName\")")

        node.fields().forEach { fNode ->
            clazz.getField(fNode.name).ifPresent { annotateField(it, fNode) }
        }
    }

    private fun annotateField(field: JavaField, node: FieldNode) {
        if (node.primary) {
            field.annotate(Id::class.java)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(SpringJPAGenerator::class.java)
    }
}
