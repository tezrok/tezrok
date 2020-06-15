package io.tezrok.core.generator

import io.tezrok.api.ExecuteContext
import io.tezrok.api.Generator
import io.tezrok.api.Phase
import io.tezrok.api.builder.JMod
import io.tezrok.api.builder.JavaClassBuilder
import io.tezrok.api.builder.expression.JavaExpression
import io.tezrok.api.service.CodeService
import io.tezrok.api.visitor.EachClassVisitor
import io.tezrok.api.visitor.MainAppVisitor
import org.slf4j.LoggerFactory

/**
 * Main application class service
 */
class CoreGenerator : Generator {
    private val log = LoggerFactory.getLogger(CoreGenerator::class.java)

    override fun execute(context: ExecuteContext) {
        if (context.phase == Phase.Generate) {
            val codeGen = context.getInstance(CodeService::class.java)
            val mainApp = codeGen.createClass(context.ofType("MainApp"), JMod.PUBLIC)
            val mainMethod = mainApp.methodMain()
            mainMethod.body = JavaExpression.EMPTY
            applyVisitors(mainApp, context)

            context.render(mainApp)
        }
    }

    private fun applyVisitors(mainApp: JavaClassBuilder, context: ExecuteContext) {
        context.applyVisitors(MainAppVisitor::class.java) { visitor ->
            visitor.visit(mainApp)
        }

        context.applyVisitors(EachClassVisitor::class.java) { visitor ->
            visitor.visit(mainApp)
        }
    }
}
