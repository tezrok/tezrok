package io.tezrok.spring

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.java.JavaClassNode
import io.tezrok.api.maven.MavenDependency
import io.tezrok.api.maven.ModuleNode
import io.tezrok.api.maven.ProjectNode
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Adds Spring related dependencies and classes.
 */
internal class SpringFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        check(project.getModules().size == 1) { "SpringGenerator only supports single module" }
        val module = project.getModules().first()
        val pom = module.pom
        // TODO: get spring version from context
        pom.getParentNode().dependencyId = MavenDependency.of("org.springframework.boot:spring-boot-starter-parent:3.1.0")
        pom.addDependency("org.springframework.boot:spring-boot-starter")
        pom.addPluginDependency("org.springframework.boot:spring-boot-maven-plugin")

        val mainClass = module.source.main.java.applicationClass
        if (mainClass != null) {
            handleMainMethod(mainClass)
            addApplicationProperties(module)
        } else {
            log.warn("Main application class not found")
        }

        return true
    }

    private fun addApplicationProperties(module: ModuleNode) {
        val appProps = module.source.main.resources.getOrAddFile("application.properties")
        if (appProps.isEmpty()) {
            // TODO: update only specified properties
            appProps.setString("spring.main.banner-mode=console\n")
        }
    }

    private fun handleMainMethod(mainClass: JavaClassNode) {
        mainClass.addAnnotation(SpringBootApplication::class.java)
        mainClass.addImport(SpringApplication::class.java)
        val mainMethod = mainClass.getOrAddMethod("main")
        mainMethod.setJavadocComment("Entry point of the application.")
        // TODO: add spring feature: not to clear body
        mainMethod.clearBody()
        mainMethod.addCallExpression("SpringApplication.run")
                .addNameArgument(mainClass.getName() + ".class")
                .addNameArgument("args")
    }

    private companion object {
        val log = LoggerFactory.getLogger(SpringFeature::class.java)!!
    }
}
