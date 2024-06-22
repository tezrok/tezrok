package io.tezrok.spring

import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.ModuleElem
import io.tezrok.api.java.JavaClassNode
import io.tezrok.api.java.JavaDirectoryNode
import io.tezrok.api.maven.ModuleNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.addNewSettings
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import kotlin.io.path.exists

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

        val applicationPackageRoot = module.source.main.java.applicationPackageRoot
        if (applicationPackageRoot != null) {
            addAppConfig(applicationPackageRoot.getOrAddJavaDirectory("config"), module, context)
        }

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

    private fun addAppConfig(configDir: JavaDirectoryNode, module: ModuleNode, context: GeneratorContext) {
        val configClassName = "AppConfig"
        if (!configDir.hasClass(configClassName)) {
            val values = mapOf(
                "moduleName" to module.getName(),
                "productName" to context.getProject().productName.ifBlank { context.getProject().name }
            )
            context.addFile(configDir, "/templates/config/AppConfig.java.vm", values)
            val customConfigDir = configDir.getOrAddDirectory("custom")
            val physicalFile = customConfigDir.getPhysicalPath()?.resolve("AppCustomConfig.java")
            if (physicalFile == null || !physicalFile.exists()) {
                context.addFile(customConfigDir, "/templates/config/AppCustomConfig.java.vm", values)
            }
        } else {
            log.warn("Config class already exists: {}", configClassName)
        }
    }

    private fun updateApplicationProperties(module: ModuleNode, moduleElem: ModuleElem?) {
        val properties = module.properties
        val dbUrl = properties.getProperty("datasource.url")?.replace("localhost", "\${DB_HOST:localhost}")
        val dbUser = "\${DB_USER:${properties.getProperty("datasource.username")}}"
        val dbPwd = "\${DB_PWD:${properties.getProperty("datasource.password")}}"
        val appProps = module.source.main.resources.getOrAddFile("application.properties")
        appProps.addNewSettings(moduleElem, "spring.datasource.url=$dbUrl",
            "spring.datasource.username=$dbUser",
            "spring.datasource.password=$dbPwd",
            "spring.datasource.driver-class-name=${properties.getProperty("datasource.driver-class-name")}",
            "spring.data.web.pageable.default-page-size=10",
            "spring.data.web.pageable.max-page-size=20")
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
