package io.tezrok.spring

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.ModuleElem
import io.tezrok.api.java.JavaClassNode
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
        val module = project.getSingleModule()
        val pom = module.pom
        // TODO: get spring version from context
        pom.addProperty("spring-boot.version", "3.2.3")
        pom.addDependency("org.springframework.boot:spring-boot-starter:${'$'}{spring-boot.version}")
        pom.addDependency("org.springframework.boot:spring-boot-starter-validation:${'$'}{spring-boot.version}")
        pom.addDependency("org.springframework.data:spring-data-commons:3.2.3")
        pom.addDependency("org.springframework.boot:spring-boot-starter-test:${'$'}{spring-boot.version}:test")
        val springBootPlugin =
            pom.addPluginDependency("org.springframework.boot:spring-boot-maven-plugin:${'$'}{spring-boot.version}")

        val mainClass = module.source.main.java.applicationClass
        if (mainClass != null) {
            handleMainMethod(mainClass)
            updateApplicationProperties(module, context.getProject().modules.find { it.name == module.getName() })

            springBootPlugin.getConfiguration().node.apply {
                add("mainClass", mainClass.getFullName())
                add("layout", "JAR")
            }

            springBootPlugin.addExecution("app-repackage", "repackage")
        } else {
            log.warn("Main application class not found")
        }

        return true
    }

    private fun updateApplicationProperties(module: ModuleNode, moduleElem: ModuleElem?) {
        val appProps = module.source.main.resources.getOrAddFile("application.properties")
        val lines = appProps.asLines().toMutableList()
        if (!lines.contains("spring.datasource")) {
            val properties = module.properties
            // TODO: update only specified properties
            lines.addAll(
                listOf(
                    "spring.datasource.url=${properties.getProperty("datasource.url")}",
                    "spring.datasource.username=${properties.getProperty("datasource.username")}",
                    "spring.datasource.password=${properties.getProperty("datasource.password")}",
                    "spring.datasource.driver-class-name=${properties.getProperty("datasource.driver-class-name")}",
                    "spring.data.web.pageable.default-page-size=10",
                    "spring.data.web.pageable.max-page-size=20"
                )
            )
        }

        moduleElem?.spring?.properties?.let { props ->
            props.includes?.forEach(lines::add)
            props.excludes?.forEach(lines::remove)
        }

        appProps.setString(lines.joinToString(NEW_LINE) + NEW_LINE)
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
