package io.tezrok.java

import com.github.javaparser.ast.Modifier.Keyword
import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.maven.ProjectNode
import org.slf4j.LoggerFactory

/**
 * Creates a class MainApp with a main method.
 *
 * Note: If method main already exists, it will not be modified.
 */
class HelloWorldFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val javaRoot = module.source.main.java
        val applicationPackageRoot = javaRoot.applicationPackageRoot

        if (applicationPackageRoot != null) {
            if (javaRoot.applicationClass == null) {
                val mainClass = applicationPackageRoot.getOrAddClass("MainApp")
                // create main method if not exists
                if (!mainClass.hasMethod("main")) {
                    val mainMethod = mainClass.addMethod("main").withModifiers(Keyword.PUBLIC, Keyword.STATIC)
                    mainMethod.addParameter("String[]", "args")
                    mainMethod.addCallExpression("System.out.println").addStringArgument("Hello, World!")
                } else {
                    log.debug("Main method already exists")
                }
                javaRoot.applicationClass = mainClass
            } else {
                log.debug("Application class already exists")
            }
        } else {
            log.debug("Application package root is not set")
        }

        return true
    }

    private companion object {
        val log = LoggerFactory.getLogger(HelloWorldFeature::class.java)!!
    }
}
