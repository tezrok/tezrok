package io.tezrok.api.visitor

import io.tezrok.api.builder.JavaClassBuilder

interface MainAppVisitor {
    /**
     * Called on class with entry point method (main)
     */
    fun visit(clazz: JavaClassBuilder)
}
