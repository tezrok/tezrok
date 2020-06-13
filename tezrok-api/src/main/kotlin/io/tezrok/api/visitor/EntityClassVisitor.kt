package io.tezrok.api.visitor

import io.tezrok.api.builder.JavaClassBuilder
import io.tezrok.api.service.Service

interface EntityClassVisitor : Service {
    /**
     * Called on each class generated from EntityNode
     */
    fun visit(clazz: JavaClassBuilder)
}
