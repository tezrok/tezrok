package io.tezrok.jooq

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.ReturnStmt
import io.tezrok.api.GeneratorContext
import io.tezrok.api.ProcessModelPhase
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.EntityElem
import io.tezrok.api.input.ModuleElem
import io.tezrok.api.input.ProjectElem
import io.tezrok.api.java.JavaClassNode
import io.tezrok.api.java.JavaDirectoryNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.asJavaType
import io.tezrok.util.camelCaseToSnakeCase
import io.tezrok.util.getRootClass
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.nio.file.Path
import java.util.stream.Collectors
import java.util.stream.Stream
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
            addBaseRepositoryFile(repositoryDir, projectElem, context)
            addWithIdInterfaces(dtoDir)

            val schemaModule = context.getProject().modules.find { it.name == module.getName() }
                ?: throw IllegalStateException("Module ${module.getName()} not found")

            val schema = schemaModule.schema

            if (schema != null) {
                val entities = schema.entities?.associate { it.name to it } ?: emptyMap()

                schema.entities?.forEach { entity ->
                    val primaryCount = entity.fields.count { it.primary == true }
                    check(primaryCount in 1..2) { "Entity ${entity.name} has unsupported count of primary keys: $primaryCount" }
                    val singlePrimary = primaryCount == 1

                    addDtoClass(dtoDir, entity, projectElem.packagePath, singlePrimary)
                    val baseMethods = getRepoMethods(repositoryDir, singlePrimary)
                    addRepositoryClass(
                        repositoryDir,
                        entity,
                        entity.customRepository == true,
                        entities,
                        baseMethods,
                        singlePrimary,
                        context
                    )
                }
                // TODO: handle enums
            }
        } else {
            log.warn("Application package root is not set, module: {}", module.getName())
        }

        return true
    }

   override fun processModel(project: ProjectElem, phase: ProcessModelPhase): ProjectElem {
        if (phase != ProcessModelPhase.Process) {
            return project
        }

        return project.copy(modules = project.modules.map { processModule(it) })
    }

    /**
     * Returns set of methods from base repository class depending on number of primary keys.
     */
    private fun getRepoMethods(repoDir: JavaDirectoryNode, singlePrimary: Boolean): Set<String> {
        val jooqBaseRepoFile = JOOQ_BASE_REPO
        val jooqRepoFile = if (singlePrimary) JOOQ_SINGLE_ID_REPO else JOOQ_TWO_ID_REPO

        // extract methods from base repository class
        return Stream.of(jooqBaseRepoFile, jooqRepoFile)
            .map { file -> repoDir.getJavaFile(file) ?: error("File not found: $file") }
            .map { it.getRootClass() }
            .flatMap { it.getMethods() }
            .filter { it.isPublic() || it.isProtected() }
            .map { it.getName() }
            .collect(Collectors.toSet())
    }

    private fun addWithIdInterfaces(dtoDir: JavaDirectoryNode) {
        dtoDir.getOrAddClass("WithId")
            .setInterface(true)
            .setTypeParameters("ID")
            .getOrAddMethod("getId")
            .removeBody()
            .setReturnType("ID")

        val class2 = dtoDir.getOrAddClass("WithId2")
            .setInterface(true)
            .setTypeParameters("ID1", "ID2")

        class2
            .getOrAddMethod("getId1")
            .removeBody()
            .setReturnType("ID1")

        class2
            .getOrAddMethod("getId2")
            .removeBody()
            .setReturnType("ID2")
    }

    private fun addDtoClass(
        dtoDir: JavaDirectoryNode,
        entity: EntityElem,
        rootPackage: String,
        singlePrimary: Boolean
    ) {
        val name = entity.name
        val jooqPackageRoot = "${rootPackage}.jooq"
        val className = "${name}Dto"
        if (!dtoDir.hasFile("$className.java")) {
            val dtoClass = dtoDir.addClass(className)
            dtoClass.extendClass("$jooqPackageRoot.tables.pojos.$name")
            val fields = entity.fields.filter { it.primary == true }
            val type1 = fields[0].asJavaType()
            val name1 = fields[0].name.capitalize()
            if (singlePrimary) {
                dtoClass.implementInterface("WithId<$type1>")
                dtoClass.addMethod("getId")
                    .withModifiers(Modifier.Keyword.PUBLIC)
                    .setReturnType(type1)
                    .addAnnotation(Override::class.java)
                    .setBody(ReturnStmt("super.get$name1()"));
            } else {
                val type2 = fields[1].asJavaType()
                val name2 = fields[1].name.capitalize()
                dtoClass.implementInterface("WithId2<$type1, $type2>")
                dtoClass.addMethod("getId1")
                    .withModifiers(Modifier.Keyword.PUBLIC)
                    .setReturnType(type1)
                    .addAnnotation(Override::class.java)
                    .setBody(ReturnStmt("super.get$name1()"))
                dtoClass.addMethod("getId2")
                    .withModifiers(Modifier.Keyword.PUBLIC)
                    .setReturnType(type2)
                    .addAnnotation(Override::class.java)
                    .setBody(ReturnStmt("super.get$name2()"))
            }
        } else {
            log.warn(FILE_ALREADY_EXISTS, "$className.java")
        }
    }

    private fun addBaseRepositoryFile(
        repositoryDir: JavaDirectoryNode,
        projectElem: ProjectElem,
        context: GeneratorContext
    ) {
        val values = mapOf("package" to projectElem.packagePath)
        val jooqSingleRepoFile = repositoryDir.addJavaFile(JOOQ_SINGLE_ID_REPO)
        context.writeTemplate(jooqSingleRepoFile, "/templates/jooq/JooqRepository.java.vm", values)
        val jooq2IdRepoFile = repositoryDir.addJavaFile(JOOQ_TWO_ID_REPO)
        context.writeTemplate(jooq2IdRepoFile, "/templates/jooq/JooqRepository2.java.vm", values)
        val jooqBaseRepoFile = repositoryDir.addJavaFile(JOOQ_BASE_REPO)
        context.writeTemplate(jooqBaseRepoFile, "/templates/jooq/JooqBaseRepository.java.vm", values)
    }

    /**
     * Adds repository class for entity with given name and base methods which must be ignored
     * during custom methods generating.
     *
     * @param repositoryDir directory where repository class will be created
     * @param entity entity
     * @param custom true if custom repository class already exists or must be created
     * @param baseMethods set of base methods which must be ignored during custom methods generating
     */
    private fun addRepositoryClass(
        repositoryDir: JavaDirectoryNode,
        entity: EntityElem,
        custom: Boolean,
        entities: Map<String, EntityElem>,
        baseMethods: Set<String>,
        singlePrimary: Boolean,
        context: GeneratorContext
    ) {
        val name = entity.name
        val rootPackage = context.getProject().packagePath
        val repoClassFileName = "${name}Repository.java"


        if (!repositoryDir.hasFile(repoClassFileName)) {
            val repoClassFile = repositoryDir.addJavaFile(repoClassFileName)
            val values =
                mapOf("package" to rootPackage, "name" to name, "uname" to name.camelCaseToSnakeCase().uppercase())
            val fields = generateFields(entity)
            val templateName = if (singlePrimary) "JooqTargetRepository" else "JooqTargetRepository2"

            context.writeTemplate(repoClassFile, "/templates/jooq/${templateName}.java.vm", values + fields)
            val repoClass = repoClassFile.getRootClass()

            if (custom) {
                repoClass.getConstructors()
                    .findFirst()
                    .orElseThrow { IllegalStateException("Constructor not found") }
                    .setModifiers(Modifier.Keyword.PROTECTED)
                repoClass.setModifiers(Modifier.Keyword.PUBLIC, Modifier.Keyword.ABSTRACT)
                addCustomMethods(entity, repoClass, repositoryDir, entities, baseMethods, context)
            } else {
                repoClass.addAnnotation(Repository::class.java)
            }

            addCustomMethodsByNames(entity, repoClass, entities, baseMethods)
        } else {
            log.warn(FILE_ALREADY_EXISTS, repoClassFileName)
        }
    }

    /**
     * Adds custom methods from custom (not generated) repository class into generated repository class.
     *
     * @param entity target entity
     * @param repoClass generated repository class
     * @param repositoryDir directory where repository class is located
     * @param baseMethods set of base methods which must be ignored during custom methods generating
     */
    private fun addCustomMethods(
        entity: EntityElem,
        repoClass: JavaClassNode,
        repositoryDir: JavaDirectoryNode,
        entities: Map<String, EntityElem>,
        baseMethods: Set<String>,
        context: GeneratorContext
    ) {
        val name = entity.name
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
                    val customRepoClass = cu.getRootClass()
                    val addedMethods = mutableListOf<String>()

                    // add methods from custom repository class
                    customRepoClass.findAll(MethodDeclaration::class.java).filter { it.isPublic }.forEach { method ->
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

                    addMethodsFromCustomInterface(
                        entity,
                        repoClass,
                        customRepoClass,
                        entities,
                        baseMethods,
                        addedMethods,
                        repositoryPhysicalPath
                    )

                    if (addedMethods.isNotEmpty() && log.isDebugEnabled) {
                        log.debug(
                            "Added methods to class: {}, methods: {}",
                            repoClass.getName(),
                            addedMethods.joinToString(", ")
                        )
                    }
                } else {
                    log.warn("Failed to parse file: {}", customFilePath)
                    parsedFile.problems.forEach { problem ->
                        log.warn("Problem: {}", problem)
                    }
                }
            } else {
                log.debug("Custom repository file not found, create it: {}", customFilePath)
                val customRepoFile =
                    repositoryDir.getOrAddJavaDirectory("custom").addJavaFile("${name}CustomRepository");
                context.writeTemplate(
                    customRepoFile, "/templates/jooq/JooqCustomRepository.java.vm",
                    mapOf("package" to context.getProject().packagePath, "name" to name)
                )
            }
        }
    }

    /**
     * Adds (generates) methods from custom repository interface into generated repository class by methods names.
     */
    private fun addMethodsFromCustomInterface(
        entity: EntityElem,
        repoClass: JavaClassNode,
        customRepoClass: ClassOrInterfaceDeclaration,
        entities: Map<String, EntityElem>,
        baseMethods: Set<String>,
        addedMethods: MutableList<String>,
        repositoryPhysicalPath: Path
    ) {
        if (customRepoClass.implementedTypes.isNonEmpty) {
            val interfaceName = customRepoClass.implementedTypes.first().nameAsString
            val interfaceFileName = "${interfaceName}.java"
            val interfaceFilePath = repositoryPhysicalPath.resolve("custom/$interfaceFileName")

            if (interfaceFilePath.exists()) {
                log.debug("Found custom repository interface file: {}", interfaceFilePath)

                val methodGenerator = JooqMethodGenerator(entity, repoClass, entities)
                val javaParser = JavaParser()
                val parsedFile = javaParser.parse(interfaceFilePath)

                if (parsedFile.isSuccessful) {
                    val interfaceRepoClass = parsedFile.result.get().getRootClass()
                    check(interfaceRepoClass.isInterface) { "Class is not interface: $interfaceFilePath" }

                    // generates methods from custom repository interface by methods names
                    interfaceRepoClass.findAll(MethodDeclaration::class.java).filter { !it.isDefault }
                        .forEach { method ->
                            val methodName = method.nameAsString
                            if (!baseMethods.contains(methodName)) {
                                methodGenerator.generateByName(methodName, method)
                                addedMethods.add(methodName)
                            }
                        }
                } else {
                    log.warn("Failed to parse file: {}", interfaceFilePath)
                    parsedFile.problems.forEach { problem ->
                        log.warn("Problem: {}", problem)
                    }
                }

            } else {
                log.debug("Custom repository file not found, create it: {}", interfaceFilePath)
            }
        }
    }

    /**
     * Add custom methods from [EntityElem.customMethods] into generated repository class.
     */
    private fun addCustomMethodsByNames(
        entity: EntityElem,
        repoClass: JavaClassNode,
        entities: Map<String, EntityElem>,
        baseMethods: Set<String>
    ) {
        val names = entity.customMethods ?: return
        val methodGenerator = JooqMethodGenerator(entity, repoClass, entities)
        names.forEach { methodName ->
            if (!baseMethods.contains(methodName)) {
                methodGenerator.generateByOnlyName(methodName)
            }
        }
    }

    private fun generateFields(entity: EntityElem): Map<String, String> {
        var counter = 1
        return entity.fields.filter { it.primary == true }
            .map { it.name }
            .map { it.camelCaseToSnakeCase() }
            .map { "field${counter++}" to it.uppercase() }
            .toMap()
    }

    private fun processModule(module: ModuleElem): ModuleElem {
        return module.copy(schema = module.schema?.copy(entities = module.schema.entities?.map { entity -> processEntity(entity) }))
    }

    private fun processEntity(entity: EntityElem): EntityElem {
        // add custom methods for unique fields
        val uniqueFields = entity.fields.filter { it.unique == true }
        if (uniqueFields.isNotEmpty()) {
            val methods = uniqueFields.map { "getBy${it.name.capitalize()}" }.toTypedArray()
            return entity.withCustomMethods(*methods)
        }

        return entity
    }

    private companion object {
        val log = LoggerFactory.getLogger(JooqRepositoryFeature::class.java)!!
        const val JOOQ_SINGLE_ID_REPO = "JooqRepository.java"
        const val JOOQ_TWO_ID_REPO = "JooqRepository2.java"
        const val JOOQ_BASE_REPO = "JooqBaseRepository.java"
        const val FILE_ALREADY_EXISTS = "File already exists: {}"
    }
}
