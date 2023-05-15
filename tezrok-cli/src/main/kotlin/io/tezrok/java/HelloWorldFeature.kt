package io.tezrok.java

import com.github.javaparser.ast.Modifier.Keyword
import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode

/**
 * Creates a class MainApp with a main method.
 */
class HelloWorldFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        check(project.getModules().size == 1) { "TODO: Support multiple modules" }
        val module = project.getModules().first()
        val mainClass = module.source.main.java.getOrCreateJavaFile("MainApp").getRootClass()
        // TODO: check if main method already exists
        val mainMethod = mainClass.addMethod("main", Keyword.PUBLIC, Keyword.STATIC)
        mainMethod.addParameter("String[]", "args")
        mainMethod.addCallExpression("System.out.println").addArgument("Hello, World!")

        return true
    }
}
