package io.tezrok.core.generator

import io.tezrok.api.builder.JavaClassBuilder
import io.tezrok.api.builder.expression.BlockExp
import io.tezrok.api.builder.expression.ExpressionBuilder
import io.tezrok.api.visitor.MainAppVisitor

class HelloWorldFeature : MainAppVisitor {
    override fun visit(clazz: JavaClassBuilder) {
        clazz.findMethod("main")
                .ifPresent { method ->
                    val expression = ExpressionBuilder._snippet("System.out.println(\"Hello, world!\");")

                    method.body = if (method.noBodyOrEmpty()) {
                        expression
                    } else {
                        BlockExp.asList(method.body, expression)
                    }
                }
    }
}
