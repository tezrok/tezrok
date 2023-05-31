package io.tezrok.spring

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.java.JavaClassNode
import io.tezrok.api.maven.MavenDependency
import io.tezrok.api.maven.ModuleNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.PathUtil.NEW_LINE
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
            updateApplicationProperties(module)
        } else {
            log.warn("Main application class not found")
        }

        return true
    }

    private fun updateApplicationProperties(module: ModuleNode) {
        val appProps = module.source.main.resources.getOrAddFile("application.properties")
        val text = appProps.asString()
        if (!text.contains("spring.datasource")) {
            // TODO: update only specified properties
            val newLines = """
                spring.datasource.url=jdbc:postgresql://localhost:5432/tezrokdb
                spring.datasource.username=tezrokAdmin
                spring.datasource.password=tezrokPwd
                spring.datasource.driver-class-name=org.postgresql.Driver${NEW_LINE}
            """.trimIndent()
            appProps.setString(text + newLines)
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
