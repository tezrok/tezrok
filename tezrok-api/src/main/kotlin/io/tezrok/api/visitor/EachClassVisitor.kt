package io.tezrok.api.visitor

import io.tezrok.api.builder.JavaClassBuilder
import io.tezrok.api.service.Service

interface EachClassVisitor : Service {
    /**
     * Called on each class
     */
    fun visit(clazz: JavaClassBuilder)
}
