package io.tezrok.api.visitor

import io.tezrok.api.builder.JavaClassBuilder
import io.tezrok.api.model.node.EntityNode
import io.tezrok.api.service.Visitor

interface EntityClassVisitor : Visitor {
    /**
     * Called on each class generated from EntityNode
     *
     * @param clazz generated Entity class
     * @param node corresponding [EntityNode]
     */
    fun visit(clazz: JavaClassBuilder, node: EntityNode)
}
