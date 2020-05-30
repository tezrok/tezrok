package io.tezrok.core.generator

import io.tezrok.api.ExecuteContext
import io.tezrok.api.Generator
import io.tezrok.api.Phase
import io.tezrok.api.builder.JMod
import io.tezrok.api.builder.JavaClassBuilder
import io.tezrok.api.builder.expression.JavaExpression
import io.tezrok.api.service.CodeService
import io.tezrok.api.visitor.MainAppVisitor
import io.tezrok.core.error.TezrokException
import io.tezrok.core.service.MainAppVisitorsProvider
import org.slf4j.LoggerFactory
import java.lang.Exception

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
            visitMainApp(mainApp, context)

            context.render(mainApp)
        }
    }

    private fun visitMainApp(mainApp: JavaClassBuilder, context: ExecuteContext) {
        context.getMainAppVisitors().forEach { visitor ->
            try {
                log.debug("Begin MainApp visitor {}", visitor.javaClass.name)

                visitor.visit(mainApp)

                log.debug("End MainApp visitor {}", visitor.javaClass.name)
            } catch (e: Exception) {
                throw TezrokException("MainApp visitor (${visitor.javaClass.name}) failed: ${e.message}", e)
            }
        }
    }
}

fun ExecuteContext.getMainAppVisitors(): List<MainAppVisitor> {
    return getInstance(MainAppVisitorsProvider::class.java).visitors
}
