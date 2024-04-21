package io.tezrok.spring

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.EntityElem
import io.tezrok.api.java.JavaClassNode
import io.tezrok.api.java.JavaDirectoryNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.*
import org.apache.commons.lang3.tuple.Pair
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Path
import kotlin.io.path.exists
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
            val schema = context.getProject().modules.find { it.name == module.getName() }?.schema

            schema?.entities?.forEach { entity ->
                addServiceClass(entity, serviceDir, projectElem.packagePath, context)
            }
        } else {
            log.warn("Application package root is not set")
        }

        return true
    }

    private fun addServiceClass(
        entity: EntityElem,
        serviceDir: JavaDirectoryNode,
        packagePath: String,
        context: GeneratorContext
    ) {
        if (entity.syntheticTo?.isNotBlank() == true) {
            // skip synthetic entities
            return
        }

        if (entity.skipService == true) {
            // skip service generation
            return
        }

        val name = entity.name
        val fileName = "${name}Service.java"
        if (!serviceDir.hasFile(fileName)) {
            val serviceFile = serviceDir.addJavaFile(fileName)
            val values =
                mapOf(
                    "package" to packagePath, "name" to name,
                    "lname" to name.lowerFirst(),
                    "primaryType" to entity.getPrimaryField().asJavaType()
                )
            context.writeTemplate(serviceFile, "/templates/spring/EntityService.java.vm", values)
            val customService = entity.customService == true || getCustomFilePath(serviceDir, name)?.exists() == true
            val serviceClass = serviceFile.getRootClass()
            val primaryFields = entity.fields.filter { it.isPrimary() }.map { it.asJavaType() }
            addRepositoryMethods(serviceDir, name, serviceClass, primaryFields)
            if (customService) {
                serviceClass.getConstructors()
                    .findFirst()
                    .orElseThrow { IllegalStateException("Constructor not found") }
                    .setModifiers(Modifier.Keyword.PROTECTED)
                serviceClass.setModifiers(Modifier.Keyword.PUBLIC, Modifier.Keyword.ABSTRACT)
                addCustomMethods(serviceDir, name, serviceClass, customService, context)
            } else {
                serviceClass.addAnnotation(Service::class.java)
                    .addAnnotation(Transactional::class.java)
            }
        } else {
            log.warn("File already exists: {}", fileName)
        }
    }

    /**
     * Adds service public methods from repository classes.
     */
    private fun addRepositoryMethods(
        serviceDir: JavaDirectoryNode,
        name: String,
        serviceClass: JavaClassNode,
        primaryFields: List<String>
    ) {
        val itemRepoName = "${name}Repository"
        val repoBaseName = "JooqBaseRepository" // TODO: get from config
        val singlePrimary = primaryFields.size == 1
        val repoExtName = if (singlePrimary) "JooqRepository" else "JooqRepository2"
        val fieldName = itemRepoName.lowerFirst()
        val repoDir = serviceDir.getParentDirectory()?.getJavaDirectory("repository")
        repoDir?.getJavaFile(repoBaseName)?.getRootClass()?.let { repoBaseClass ->
            addMethodsFromRepoClass(serviceClass, repoBaseClass, fieldName, primaryFields, name)
        }
        repoDir?.getJavaFile(repoExtName)?.getRootClass()?.let { repoBaseClass ->
            addMethodsFromRepoClass(serviceClass, repoBaseClass, fieldName, primaryFields, name)
        }
        repoDir?.getJavaFile(itemRepoName)?.getRootClass()?.let { repoClass ->
            addMethodsFromRepoClass(serviceClass, repoClass, fieldName, primaryFields, includeAbstract = true)
        }

        if (!singlePrimary) {
            serviceClass.addImport(Pair::class.java)
        }
    }

    private fun addMethodsFromRepoClass(
        serviceClass: JavaClassNode,
        repoClass: JavaClassNode,
        fieldName: String,
        primaryFields: List<String>,
        entityName: String = "",
        includeAbstract: Boolean = false
    ) {
        repoClass.getMethods().filter { method -> method.isPublic() && (includeAbstract || !method.isAbstract()) }.forEach { method ->
            val newMethod = serviceClass.addMethod(method.getName())
                .withModifiers(Modifier.Keyword.PUBLIC)
                .setReturnType(replaceType(method.getTypeAsString(), entityName, primaryFields))
                .setTypeParameters(method.getTypeParameters())
            val callExpr = newMethod.addCallExpression(fieldName + "." + method.getName())

            method.getParameters().forEach { param ->
                val typeName = replaceType(param.getTypeAsString(), entityName, primaryFields)
                newMethod.addParameter(typeName, param.getName())
                callExpr.addNameArgument(param.getName())
                serviceClass.addImportsByType(typeName)
            }
            newMethod.addReturnToLastExpression()
            if (isMethodReadOnly(method.getName())) {
                newMethod.addAnnotation("Transactional", mapOf("readOnly" to BooleanLiteralExpr(true)))
            }
            serviceClass.addImportsByType(method.getTypeAsString())
        }
    }

    private fun isMethodReadOnly(name: String) = name.let {
        it.startsWith("get") && !it.startsWith("getOrCreate") || it.startsWith("find") || it.startsWith("count")
                || it.startsWith("exists") || it.startsWith("search") || it.startsWith("list")
    }

    private fun replaceType(typeName: String, entityName: String, primaryFields: List<String>): String {
        val newType = if (entityName.isEmpty()) {
            typeName
        } else {
            typeName.replace(pojoParamPattern, "${entityName}Dto").replace(recordParamPattern, "${entityName}Record")
        }

        return if (primaryFields.size == 1) {
            newType.replace(idParamPattern, primaryFields[0])
        } else {
            newType.replace(id1ParamPattern, primaryFields[0]).replace(id2ParamPattern, primaryFields[1])
        }
    }

    /**
     * Adds custom methods to service class from physical custom file.
     */
    private fun addCustomMethods(
        serviceDir: JavaDirectoryNode,
        name: String,
        serviceClass: JavaClassNode,
        custom: Boolean,
        context: GeneratorContext
    ) {
        val customFilePath = getCustomFilePath(serviceDir, name)
        if (customFilePath != null) {
            if (customFilePath.exists()) {
                if (customFilePath.isDirectory()) {
                    log.warn("Found directory instead of file: {}", customFilePath)
                    return
                }

                log.debug("Found custom repository file: {}", customFilePath)
                val javaParser = JavaParserFactory.create()
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
                        log.debug(
                            "Added methods to class: {}, methods: {}",
                            serviceClass.getName(),
                            addedMethods.joinToString(", ")
                        )
                    }
                } else {
                    log.warn("Failed to parse file: {}", customFilePath)
                    parsedFile.problems.forEach { problem ->
                        log.warn("Problem: {}", problem)
                    }
                }
            } else if (custom) {
                log.debug("Custom service file not found, create it: {}", customFilePath)
                val customFileName = customFilePath.fileName.toString()
                val customRepoFile = serviceDir.getOrAddJavaDirectory("custom").addJavaFile(customFileName)
                context.writeTemplate(
                    customRepoFile,
                    "/templates/spring/EntityCustomService.java.vm",
                    mapOf("package" to context.getProject().packagePath, "name" to name, "lname" to name.lowerFirst())
                )
            }
        }
    }

    private fun getCustomFilePath(serviceDir: JavaDirectoryNode, name: String): Path? {
        val servicePhysicalPath = serviceDir.getPhysicalPath()
        val customFileName = "${name}CustomService.java"
        return if (servicePhysicalPath != null && servicePhysicalPath.exists()) {
            servicePhysicalPath.resolve("custom/${customFileName}")
        } else
            null
    }

    private companion object {
        val log = LoggerFactory.getLogger(ServiceFeature::class.java)!!
        val pojoParamPattern = Regex("\\bP\\b")
        val recordParamPattern = Regex("\\bR\\b")
        val idParamPattern = Regex("\\bID\\b")
        val id1ParamPattern = Regex("\\bID1\\b")
        val id2ParamPattern = Regex("\\bID2\\b")
    }
}
