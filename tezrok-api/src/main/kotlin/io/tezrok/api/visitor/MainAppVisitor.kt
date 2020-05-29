package io.tezrok.api.visitor

import io.tezrok.api.builder.JavaClassBuilder
import io.tezrok.api.service.Service

interface MainAppVisitor : Service {
    /**
     * Called on class with entry point method (main)
     */
    fun visit(clazz: JavaClassBuilder)
}
