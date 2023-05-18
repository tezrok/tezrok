package io.tezrok.java

import com.github.javaparser.ast.Modifier.Keyword
import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode

/**
 * Creates a class MainApp with a main method.
 *
 * Note: If method main already exists, it will not be modified.
 */
class HelloWorldFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        check(project.getModules().size == 1) { "TODO: Support multiple modules" }
        val module = project.getModules().first()
        val packagePath = context.getProject().packagePath
        val classPackageRoot = module.source.main.java.makeDirectories(packagePath.replace('.', '/'))
        val mainClass = classPackageRoot.getOrCreateClass("MainApp")
        // create main method if not exists
        if (!mainClass.hasMethod("main")) {
            val mainMethod = mainClass.addMethod("main").withModifiers(Keyword.PUBLIC, Keyword.STATIC)
            mainMethod.addParameter("String[]", "args")
            mainMethod.addCallExpression("System.out.println").addArgument("Hello, World!")
        }

        return true
    }
}
