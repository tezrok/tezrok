package io.tezrok.core.generator

import io.tezrok.api.ExecuteContext
import io.tezrok.api.Generator
import io.tezrok.api.builder.JavaClassBuilder
import io.tezrok.api.builder.expression.BlockExp
import io.tezrok.api.builder.expression.ExpressionBuilder
import io.tezrok.api.visitor.MainAppVisitor
import org.slf4j.LoggerFactory

class HelloWorldGenerator : Generator, MainAppVisitor {
    private val log = LoggerFactory.getLogger(javaClass)

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

    override fun execute(context: ExecuteContext) {
    }
}
