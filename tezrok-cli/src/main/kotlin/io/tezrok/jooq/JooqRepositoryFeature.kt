package io.tezrok.jooq

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.MethodDeclaration
import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.java.JavaClassNode
import io.tezrok.api.java.JavaDirectoryNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.core.input.ProjectElem
import io.tezrok.util.getRootClass
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.nio.file.Files
import kotlin.io.path.isDirectory

/**
 * Creates repository class for each entity.
 */
internal class JooqRepositoryFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val applicationPackageRoot = module.source.main.java.applicationPackageRoot

        if (applicationPackageRoot != null) {
            val projectElem = context.getProject()
            val repositoryDir = applicationPackageRoot.getOrAddJavaDirectory("repository")
            val dtoDir = applicationPackageRoot.getOrAddJavaDirectory("dto")
            addBaseRepositoryClass(repositoryDir, context, projectElem)
            addWithIdInterface(dtoDir)

            val schemaModule = context.getProject().modules.find { it.name == module.getName() }
                    ?: throw IllegalStateException("Module ${module.getName()} not found")

            schemaModule.schema?.definitions?.forEach { name, defition ->
                addDtoClass(dtoDir, name, projectElem.packagePath)
                addRepositoryClass(repositoryDir, name, projectElem.packagePath, defition.customRepository == true)
            }
        } else {
            log.warn("Application package root is not set, module: {}", module.getName())
        }

        return true
    }

    private fun addWithIdInterface(dtoDir: JavaDirectoryNode) {
        dtoDir.getOrAddClass("WithId")
                .setInterface(true)
                .setTypeParameters("ID")
                .getOrAddMethod("getId")
                .removeBody()
                .setReturnType("ID")
    }

    private fun addDtoClass(dtoDir: JavaDirectoryNode, name: String, rootPackage: String) {
        val jooqPackageRoot = "${rootPackage}.jooq"
        val className = "${name}Dto"
        if (!dtoDir.hasFile("$className.java")) {
            val repoClass = dtoDir.addClass(className)
            repoClass.extendClass("$jooqPackageRoot.tables.pojos.$name")
            repoClass.implementInterface("WithId<Long>")
        }
    }

    private fun addBaseRepositoryClass(repositoryDir: JavaDirectoryNode, context: GeneratorContext, projectElem: ProjectElem) {
        val jooqRepoFile = repositoryDir.getOrAddFile("JooqRepository.java")
        if (jooqRepoFile.isEmpty()) {
            context.writeTemplate(jooqRepoFile, "/templates/jooq/JooqRepository.java.vm") { velContext ->
                velContext.put("package", projectElem.packagePath)
            }
        } else {
            log.warn("File already exists: {}", jooqRepoFile.getName())
        }
    }

    private fun addRepositoryClass(repositoryDir: JavaDirectoryNode, name: String, rootPackage: String, custom: Boolean) {
        val jooqPackageRoot = "${rootPackage}.jooq"
        val className = "${name}Repository"

        if (!repositoryDir.hasFile("$className.java")) {
            val repoClass = repositoryDir.addClass(className)
            repoClass.extendClass("JooqRepository<${name}Record, Long, ${name}Dto>")
            repoClass.addImport("$jooqPackageRoot.Tables")
            repoClass.addImport("$jooqPackageRoot.tables.records.${name}Record")
            repoClass.addImport("$rootPackage.dto.${name}Dto")

            repoClass.addImport(DSLContext::class.java)
            val constructor = repoClass.addConstructor()
                    .addParameter(DSLContext::class.java, "dsl")

            constructor.addCallSuperExpression()
                    .addNameArgument("dsl")
                    .addNameArgument("Tables.${name.uppercase()}")
                    .addNameArgument("Tables.${name.uppercase()}.ID")
                    .addNameArgument("${name}Dto.class")

            if (custom) {
                constructor.withModifiers(Modifier.Keyword.PROTECTED)
                repoClass.withModifiers(Modifier.Keyword.ABSTRACT)
                addCustomMethods(name, repoClass, repositoryDir)
            } else {
                constructor.withModifiers(Modifier.Keyword.PUBLIC)
                repoClass.addAnnotation(Repository::class.java)
            }
        }
    }

    private fun addCustomMethods(name: String, repoClass: JavaClassNode, repositoryDir: JavaDirectoryNode) {
        val repositoryPhysicalPath = repositoryDir.getPhysicalPath()
        val customClassName = "${name}CustomRepository"
        val customFileName = "${customClassName}.java"

        if (repositoryPhysicalPath != null && Files.exists(repositoryPhysicalPath)) {
            val customFilePath = repositoryPhysicalPath.resolve("custom/${customFileName}")

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
                    val clazz = cu.getRootClass()
                    val addedMethods = mutableListOf<String>()

                    clazz.findAll(MethodDeclaration::class.java).forEach { method ->
                        val methodName = method.nameAsString
                        if (!stdMethods.contains(methodName)) {
                            addedMethods.add(methodName)
                            val newMethod = repoClass.addMethod(methodName)
                                    .withModifiers(Modifier.Keyword.PUBLIC, Modifier.Keyword.ABSTRACT)
                                    .removeBody()
                                    .setReturnType(method.typeAsString)
                            method.parameters.forEach { param ->
                                newMethod.addParameter(param.typeAsString, param.nameAsString)
                            }
                        }
                    }

                    if (addedMethods.isNotEmpty() && log.isDebugEnabled) {
                        log.debug("Added methods to class: {}, methods: {}", repoClass.getName(), addedMethods.joinToString(", "))
                    }
                } else {
                    log.warn("Failed to parse file: {}", customFilePath)
                    parsedFile.problems.forEach { problem ->
                        log.warn("Problem: {}", problem)
                    }
                }
            }
        }

        // TODO create custom repository class if not exists
    }

    private companion object {
        val log = LoggerFactory.getLogger(JooqRepositoryFeature::class.java)!!
        val stdMethods = setOf("getById", "findAll", "findAllById", "count", "update", "create", "save", "saveAll", "deleteById", "deleteAllById", "deleteAll", "existsById", "getRecordById")
    }
}
