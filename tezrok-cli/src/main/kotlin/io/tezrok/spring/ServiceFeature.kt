package io.tezrok.spring

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.BooleanLiteralExpr
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
            val serviceClass = serviceFile.getRootClass()
            addRepositoryMethods(serviceDir, name, serviceClass)
            addCustomMethods(serviceDir, name, serviceClass)
        } else {
            log.warn("File already exists: {}", fileName)
        }
    }

    /**
     * Adds service public methods from repository class.
     */
    private fun addRepositoryMethods(serviceDir: JavaDirectoryNode, name: String, serviceClass: JavaClassNode) {
        val repoName = "${name}Repository"
        val repoBaseName = "JooqRepository" // TODO: get from config
        val fieldName = repoName.replaceFirstChar { it.lowercase() }
        val repoDir = serviceDir.getParentDirectory()?.getJavaDirectory("repository")
        repoDir?.getJavaFile(repoBaseName)?.getRootClass()?.let { repoBaseClass ->
            addMethodsFromRepoClass(serviceClass, repoBaseClass, fieldName, name)
        }
        repoDir?.getJavaFile(repoName)?.getRootClass()?.let { repoClass ->
            addMethodsFromRepoClass(serviceClass, repoClass, fieldName)
        }
    }

    private fun addMethodsFromRepoClass(serviceClass: JavaClassNode, repoClass: JavaClassNode, fieldName: String, name: String = "") {
        repoClass.getMethods().filter { method -> method.isPublic() }.forEach { method ->
            val newMethod = serviceClass.addMethod(method.getName())
                    .withModifiers(Modifier.Keyword.PUBLIC)
                    .setReturnType(replaceType(method.getTypeAsString(), name))
            val callExpr = newMethod.addCallExpression(fieldName + "." + method.getName())

            method.getParameters().forEach { param ->
                newMethod.addParameter(replaceType(param.getTypeAsString(), name), param.getName())
                callExpr.addNameArgument(param.getName())
            }
            newMethod.addReturnToLastExpression()
            if (isMethodReadOnly(method.getName())) {
                newMethod.addAnnotation("Transactional", mapOf("readOnly" to BooleanLiteralExpr(true)))
            }
        }
    }

    private fun isMethodReadOnly(name: String) = name.let {
        it.startsWith("get") || it.startsWith("find") || it.startsWith("count")
                || it.startsWith("exists") || it.startsWith("search") || it.startsWith("list")
    }

    private fun replaceType(typeName: String, name: String): String {
        val newType = if (name.isEmpty()) {
            typeName
        } else {
            typeName.replace(pojoParamPattern, "${name}Dto").replace(recordParamPattern, "${name}Record")
        }

        return newType.replace(idParamPattern, "Long")
    }

    /**
     * Adds custom methods to service class from physical custom file.
     */
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
        val pojoParamPattern = Regex("\\bP\\b")
        val recordParamPattern = Regex("\\bR\\b")
        val idParamPattern = Regex("\\bID\\b")
    }
}
