package io.tezrok.jooq

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.MethodDeclaration
import io.tezrok.api.GeneratorContext
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.ProjectElem
import io.tezrok.api.java.JavaClassNode
import io.tezrok.api.java.JavaDirectoryNode
import io.tezrok.api.java.JavaFileNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.getRootClass
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.stream.Collectors
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * Creates repository class for storable entity.
 *
 * Creates custom repository class if it doesn't exist.
 */
internal class JooqRepositoryFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val applicationPackageRoot = module.source.main.java.applicationPackageRoot

        if (applicationPackageRoot != null) {
            val projectElem = context.getProject()
            val repositoryDir = applicationPackageRoot.getOrAddJavaDirectory("repository")
            val dtoDir = applicationPackageRoot.getOrAddJavaDirectory("dto")
            val javaBaseRepoFile = addBaseRepositoryFile(repositoryDir, projectElem, context)
            addWithIdInterface(dtoDir)

            val schemaModule = context.getProject().modules.find { it.name == module.getName() }
                    ?: throw IllegalStateException("Module ${module.getName()} not found")

            // extract methods from base repository class
            val baseMethods = javaBaseRepoFile.getRootClass()
                    .getMethods()
                    .filter { it.isPublic() || it.isProtected() }
                    .map { it.getName() }
                    .collect(Collectors.toSet())
            log.debug("Base repository methods: {}", baseMethods)
            val schema = schemaModule.schema

            if (schema != null) {
                schema.entities?.forEach { entity ->
                    addDtoClass(dtoDir, entity.name, projectElem.packagePath)
                    addRepositoryClass(repositoryDir, entity.name, entity.customRepository == true, baseMethods, context)
                }
                // TODO: handle enums
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
        } else {
            log.warn(FILE_ALREADY_EXISTS, "$className.java")
        }
    }

    private fun addBaseRepositoryFile(repositoryDir: JavaDirectoryNode, projectElem: ProjectElem, context: GeneratorContext): JavaFileNode {
        if (!repositoryDir.hasFile(JOOQ_BASE_REPO)) {
            val jooqRepoFile = repositoryDir.addJavaFile(JOOQ_BASE_REPO)
            context.writeTemplate(jooqRepoFile, "/templates/jooq/JooqRepository.java.vm",
                    mapOf("package" to projectElem.packagePath))
            return jooqRepoFile
        } else {
            log.warn(FILE_ALREADY_EXISTS, repositoryDir)
        }

        return repositoryDir.getJavaFile(JOOQ_BASE_REPO)
                ?: throw IllegalStateException("File not found: $JOOQ_BASE_REPO")
    }

    /**
     * Adds repository class for entity with given name and base methods which must be ignored
     * during custom methods generating.
     *
     * @param repositoryDir directory where repository class will be created
     * @param name entity name
     * @param custom true if custom repository class already exists or must be created
     * @param baseMethods set of base methods which must be ignored during custom methods generating
     */
    private fun addRepositoryClass(repositoryDir: JavaDirectoryNode, name: String, custom: Boolean, baseMethods: Set<String>, context: GeneratorContext) {
        val rootPackage = context.getProject().packagePath
        val repoClassFileName = "${name}Repository.java"

        if (!repositoryDir.hasFile(repoClassFileName)) {
            val repoClassFile = repositoryDir.addJavaFile(repoClassFileName)
            context.writeTemplate(repoClassFile, "/templates/jooq/JooqTargetRepository.java.vm",
                    mapOf("package" to rootPackage, "name" to name, "uname" to name.uppercase()))
            val repoClass = repoClassFile.getRootClass()
            val constructor = repoClass.getConstructors()
                    .findFirst()
                    .orElseThrow { IllegalStateException("Constructor not found") }

            if (custom) {
                constructor.setModifiers(Modifier.Keyword.PROTECTED)
                repoClass.setModifiers(Modifier.Keyword.PUBLIC, Modifier.Keyword.ABSTRACT)
                addCustomMethods(name, repoClass, repositoryDir, baseMethods, context)
            } else {
                repoClass.addAnnotation(Repository::class.java)
            }
        } else {
            log.warn(FILE_ALREADY_EXISTS, repoClassFileName)
        }
    }

    /**
     * Adds custom methods from custom (not generated) repository class into generated repository class.
     *
     * @param name entity name
     * @param repoClass generated repository class
     * @param repositoryDir directory where repository class is located
     * @param baseMethods set of base methods which must be ignored during custom methods generating
     */
    private fun addCustomMethods(name: String, repoClass: JavaClassNode, repositoryDir: JavaDirectoryNode, baseMethods: Set<String>, context: GeneratorContext) {
        val repositoryPhysicalPath = repositoryDir.getPhysicalPath()

        if (repositoryPhysicalPath != null && repositoryPhysicalPath.exists()) {
            val customFileName = "${name}CustomRepository.java"
            val customFilePath = repositoryPhysicalPath.resolve("custom/${customFileName}")

            if (customFilePath.exists()) {
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

                    // add methods from custom repository class
                    clazz.findAll(MethodDeclaration::class.java).filter { it.isPublic }.forEach { method ->
                        val methodName = method.nameAsString
                        if (!baseMethods.contains(methodName)) {
                            addedMethods.add(methodName)
                            val newMethod = repoClass.addMethod(methodName)
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
                        log.debug("Added methods to class: {}, methods: {}", repoClass.getName(), addedMethods.joinToString(", "))
                    }
                } else {
                    log.warn("Failed to parse file: {}", customFilePath)
                    parsedFile.problems.forEach { problem ->
                        log.warn("Problem: {}", problem)
                    }
                }
            } else {
                log.debug("Custom repository file not found, create it: {}", customFilePath)
                val customRepoFile = repositoryDir.getOrAddJavaDirectory("custom").addJavaFile("${name}CustomRepository");
                context.writeTemplate(customRepoFile, "/templates/jooq/JooqCustomRepository.java.vm",
                        mapOf("package" to context.getProject().packagePath, "name" to name))
            }
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(JooqRepositoryFeature::class.java)!!
        const val JOOQ_BASE_REPO = "JooqRepository.java"
        const val FILE_ALREADY_EXISTS = "File already exists: {}"
    }
}
