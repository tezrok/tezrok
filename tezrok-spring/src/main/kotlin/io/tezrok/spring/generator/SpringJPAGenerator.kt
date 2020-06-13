package io.tezrok.spring.generator

import io.tezrok.api.ExecuteContext
import io.tezrok.api.Generator
import io.tezrok.api.builder.JavaClassBuilder
import io.tezrok.api.visitor.EachClassVisitor

/**
 * Generates jpa-repositories
 */
class SpringJPAGenerator : Generator, EachClassVisitor {
    override fun execute(context: ExecuteContext) {
        TODO("not implemented")
    }

    override fun visit(clazz: JavaClassBuilder) {
        TODO("not implemented")
    }
}
