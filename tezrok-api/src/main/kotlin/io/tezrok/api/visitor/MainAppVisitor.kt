package io.tezrok.api.visitor

import io.tezrok.api.builder.JavaClassBuilder
import io.tezrok.api.service.Visitor

interface MainAppVisitor : Visitor {
    /**
     * Called on class with entry point method (main)
     */
    fun visit(clazz: JavaClassBuilder)
}
