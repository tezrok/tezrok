package io.tezrok.api.visitor

import io.tezrok.api.builder.JavaClassBuilder
import io.tezrok.api.service.Visitor

interface EachClassVisitor : Visitor {
    /**
     * Called on each class
     */
    fun visit(clazz: JavaClassBuilder)
}
