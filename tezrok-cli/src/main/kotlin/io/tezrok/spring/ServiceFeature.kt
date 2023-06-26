package io.tezrok.spring

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.MethodDeclaration
import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.java.JavaClassNode
import io.tezrok.api.java.JavaDirectoryNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.getRootClass
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Files
import kotlin.io.path.isDirectory

/**
 * Creates service class for each entity.
 */
open class ServiceFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val applicationPackageRoot = module.source.main.java.applicationPackageRoot

        if (applicationPackageRoot != null) {
            val projectElem = context.getProject()
            val serviceDir = applicationPackageRoot.getOrAddJavaDirectory("service")
            val schemaModule = context.getProject().modules.find { it.name == module.getName() }
                    ?: throw IllegalStateException("Module ${module.getName()} not found")

            schemaModule.schema?.definitions?.keys?.forEach { name ->
                addServiceClass(name, serviceDir, projectElem.packagePath, context)
            }
        } else {
            log.warn("Application package root is not set")
        }

        return true
    }

    private fun addServiceClass(name: String, serviceDir: JavaDirectoryNode, packagePath: String, context: GeneratorContext) {
        val fileName = "${name}Service.java"
        if (!serviceDir.hasFile(fileName)) {
            val serviceFile = serviceDir.addJavaFile(fileName)
            val values = mapOf("package" to packagePath, "name" to name, "lname" to name.replaceFirstChar { it.lowercase() })
            context.writeTemplate(serviceFile, "/templates/spring/EntityService.java.vm", values)
            addCustomMethods(serviceDir, name, serviceFile.getRootClass())
        } else {
            log.warn("File already exists: {}", fileName)
        }
    }

    private fun addCustomMethods(serviceDir: JavaDirectoryNode, name: String, serviceClass: JavaClassNode) {
        val servicePhysicalPath = serviceDir.getPhysicalPath()
        if (servicePhysicalPath != null && Files.exists(servicePhysicalPath)) {
            val customFileName = "${name}CustomService.java"
            val customFilePath = servicePhysicalPath.resolve("custom/${customFileName}")

            if (Files.exists(customFilePath)) {
                if (customFilePath.isDirectory()) {
                    log.warn("Found directory instead of file: {}", customFilePath)
                    return
                }

                log.debug("Found custom repository file: {}", customFilePath)
                val javaParser = JavaParser()
                val parsedFile = javaParser.parse(customFilePath)

                if (parsedFile.isSuccessful) {
                    val cu = parsedFile.result.get()
                    val customClass = cu.getRootClass()
                    val addedMethods = mutableListOf<String>()

                    customClass.findAll(MethodDeclaration::class.java).filter { it.isPublic }.forEach { method ->
                        val methodName = method.nameAsString
                        if (!serviceClass.hasMethod(methodName)) {
                            addedMethods.add(methodName)
                            val newMethod = serviceClass.addMethod(methodName)
                                    .withModifiers(Modifier.Keyword.PUBLIC, Modifier.Keyword.ABSTRACT)
                                    .removeBody()
                                    .setReturnType(method.typeAsString)
                            method.parameters.forEach { param ->
                                // TODO: use type as fully qualified name
                                newMethod.addParameter(param.typeAsString, param.nameAsString)
                            }
                        }
                    }

                    if (addedMethods.isNotEmpty() && log.isDebugEnabled) {
                        log.debug("Added methods to class: {}, methods: {}", serviceClass.getName(), addedMethods.joinToString(", "))
                    }

                    // if we have custom service class, we need to remove annotations and make class abstract
                    serviceClass.withModifiers(Modifier.Keyword.ABSTRACT)
                            .removeAnnotation(Service::class.java)
                            .removeImport(Service::class.java)
                            .removeAnnotation("Transactional")
                } else {
                    log.warn("Failed to parse file: {}", customFilePath)
                    parsedFile.problems.forEach { problem ->
                        log.warn("Problem: {}", problem)
                    }
                }
            }
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(ServiceFeature::class.java)!!
    }
}
