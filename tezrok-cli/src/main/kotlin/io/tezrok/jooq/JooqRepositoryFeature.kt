package io.tezrok.jooq

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.stmt.ReturnStmt
import io.tezrok.api.GeneratorContext
import io.tezrok.api.ProcessModelPhase
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.*
import io.tezrok.api.java.JavaClassNode
import io.tezrok.api.java.JavaDirectoryNode
import io.tezrok.api.java.JavaFieldNode
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.io.Serializable
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
            addIdDtoClasses(dtoDir, projectElem.packagePath, context)

            val schemaModule = context.getProject().modules.find { it.name == module.getName() }
                ?: throw IllegalStateException("Module ${module.getName()} not found")

            val schema = schemaModule.schema

            if (schema != null) {
                val entities = schema.entities?.associate { it.name to it } ?: emptyMap()

                schema.entities?.forEach { entity ->
                    val primaryCount = entity.getPrimaryFieldCount()
                    check(primaryCount in 1..2) { "Entity ${entity.name} has unsupported count of primary keys: $primaryCount" }
                    val singlePrimary = primaryCount == 1

                    addDtoClass(dtoDir, entity, projectElem.packagePath, singlePrimary)
                    if (entity.isNotSynthetic() && entity.hasFullDto()) {
                        addFullDtoClass(dtoDir, entity, projectElem.packagePath)
                    }
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

    private fun addIdDtoClasses(dtoDir: JavaDirectoryNode, packagePath: String, context: GeneratorContext) {
        val fileName = "IdDto.java"
        val idFile = dtoDir.addJavaFile(fileName)
        val values = mapOf("package" to packagePath)
        context.writeTemplate(idFile, "/templates/jooq/IdDto.java.vm", values)
    }

    /**
     * Adds dto class for entity into "dto" directory.
     */
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
            entity.description?.let { dtoClass.setJavadocComment(it) }

            // add field size constants
            entity.fields.filter { it.isStringType() }.forEach { field ->
                val minLength = field.minLength ?: 0
                dtoClass.addField("int", field.minSizeConstantName())
                    .setModifiers(Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL)
                    .setInitializer(minLength.toString())
                val maxLength = field.maxLength ?: DEFAULT_VARCHAR_LENGTH
                dtoClass.addField("int", field.maxSizeConstantName())
                    .setModifiers(Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL)
                    .setInitializer(maxLength.toString())
            }

            // add empty constructor
            dtoClass.addConstructor()
                .withModifiers(Modifier.Keyword.PUBLIC)
            val constructor2 = dtoClass.addConstructor()
                .withModifiers(Modifier.Keyword.PUBLIC)
            val args = mutableListOf<String>()
            entity.fields.filter { it.isNotLogic() }.forEach { field ->
                val type = field.asJavaType()
                val name = field.name
                constructor2.addParameter(type, name)
                args.add(name)
            }
            constructor2.setBody("super(${args.joinToString(", ")});".parseAsStatement())

            val fields = entity.fields.filter { it.isPrimary() }
            val type1 = fields[0].asJavaType()
            val name1 = fields[0].name.upperFirst()
            if (singlePrimary) {
                dtoClass.implementInterface("WithId<$type1>")
                var method = dtoClass.addMethod("getId")
                    .withModifiers(Modifier.Keyword.PUBLIC)
                    .setReturnType(type1)
                    .addAnnotation(Override::class.java)
                    .setBody(ReturnStmt("super.get$name1()"));

                if (name1 != "Id") {
                    method.addAnnotation(JsonIgnore::class.java)
                }
            } else {
                val type2 = fields[1].asJavaType()
                val name2 = fields[1].name.upperFirst()
                dtoClass.implementInterface("WithId2<$type1, $type2>")
                var method1 = dtoClass.addMethod("getId1")
                    .withModifiers(Modifier.Keyword.PUBLIC)
                    .setReturnType(type1)
                    .addAnnotation(Override::class.java)
                    .setBody(ReturnStmt("super.get$name1()"))
                if (name1 != "Id1") {
                    method1.addAnnotation(JsonIgnore::class.java)
                }
                val method2 = dtoClass.addMethod("getId2")
                    .withModifiers(Modifier.Keyword.PUBLIC)
                    .setReturnType(type2)
                    .addAnnotation(Override::class.java)
                    .setBody(ReturnStmt("super.get$name2()"))
                if (name2 != "Id2") {
                    method2.addAnnotation(JsonIgnore::class.java)
                }
            }

            val sensitiveFields = entity.fields.filter { it.hasMetaType(MetaType.Sensitive) }
            if (sensitiveFields.isNotEmpty()) {
                // override sensitive fields with @JsonIgnore
                sensitiveFields.forEach { field ->
                    val returnType = field.asJavaType()
                    val getter = field.getGetterName()
                    dtoClass.addMethod(getter)
                        .withModifiers(Modifier.Keyword.PUBLIC)
                        .setReturnType(returnType)
                        .setBody(ReturnStmt("super.$getter()"))
                        .addAnnotation(Override::class.java)
                        .addAnnotation(JsonIgnore::class.java)
                }
            }
        } else {
            log.warn(FILE_ALREADY_EXISTS, "$className.java")
        }
    }

    /**
     * Adds full dto class for entity into "dto.full" directory.
     *
     * Full dto class contains logical fields as well and doesn't contain synthetic fields.
     */
    private fun addFullDtoClass(
        dtoDir: JavaDirectoryNode,
        entity: EntityElem,
        rootPackage: String
    ) {
        val dtoDir = dtoDir.getOrAddJavaDirectory("full")
        val name = entity.name
        val className = "${name}FullDto"
        if (!dtoDir.hasFile("$className.java")) {
            val dtoClass = dtoDir.addClass(className)
                .setJavadocComment("Full dto (with logic and without synthetic fields) for {@link ${name}Dto} entity.")
                .addAnnotation(Data::class.java)
                .addAnnotation(AllArgsConstructor::class.java)
                .addAnnotation(NoArgsConstructor::class.java)
                .addImportBySimpleName("${name}Dto")
            dtoClass.implementInterface(Serializable::class.java)
            val addedFields = mutableSetOf<JavaFieldNode>()
            entity.fields.filter { it.isNotSynthetic() }.forEach { field ->
                if (field.isBaseType()) {
                    addedFields.add(dtoClass.addField(field.asJavaType(), field.name))
                } else {
                    val targetType = "${field.type}FullDto"
                    when (field.relation) {
                        EntityRelation.OneToMany, EntityRelation.ManyToMany -> {
                            addedFields.add(dtoClass.addField("List<$targetType>", field.name))
                        }

                        EntityRelation.ManyToOne, EntityRelation.OneToOne -> {
                            addedFields.add(dtoClass.addField(targetType, field.name))
                        }

                        else -> error("Unsupported relation: ${field.relation} in field: ${entity.name}.${field.name}")
                    }
                }
                addValidationAnnotations(addedFields.last(), field, dtoClass, entity)
            }

            // implement WithId<$primaryFieldType> interface
            addWithIdImplementation(dtoClass, entity, rootPackage)
            // implement Cloneable interface
            addCloneableImplementation(dtoClass, entity)
        } else {
            log.warn(FILE_ALREADY_EXISTS, "$className.java")
        }
    }

    private fun addValidationAnnotations(
        field: JavaFieldNode,
        fieldElem: FieldElem,
        clazz: JavaClassNode,
        entity: EntityElem
    ) {
        if (fieldElem.hasMetaType(MetaType.Sensitive)) {
            // @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
            clazz.addImport(JsonProperty::class.java)
            field.addAnnotation(
                "JsonProperty",
                mapOf("access" to NameExpr("JsonProperty.Access.WRITE_ONLY"))
            )
        }

        if (fieldElem.required == true && !fieldElem.hasMetaType(MetaType.CreatedAt) && !fieldElem.hasMetaType(MetaType.UpdatedAt)) {
            // createAt and updatedAt fields are not required in dto but required in db
            field.addAnnotation(NotNull::class.java)
        }
        if (fieldElem.hasMetaType(MetaType.Email)) {
            field.addAnnotation(Email::class.java)
        }
        if (fieldElem.isStringType()) {
            val dtoClassName = "${entity.name}Dto"
            val minLength = fieldElem.minLength ?: 0
            val annotationProps = mutableMapOf<String, Expression>()
            if (minLength > 0) {
                annotationProps["min"] = NameExpr(dtoClassName + "." + fieldElem.minSizeConstantName())
            }
            if (fieldElem.maxLength != null) {
                annotationProps["max"] = NameExpr(dtoClassName + "." + fieldElem.maxSizeConstantName())
            }
            clazz.addImport(Size::class.java)
            field.addAnnotation("Size", annotationProps)
        }
    }

    private fun addWithIdImplementation(dtoClass: JavaClassNode, entity: EntityElem, rootPackage: String) {
        val primaryFieldCount = entity.getPrimaryFieldCount()
        check(primaryFieldCount == 1) { "Entity ${entity.name} has unsupported count of primary keys: $primaryFieldCount" }

        val primaryField = entity.getPrimaryField()
        val primaryFieldType = primaryField.asJavaType()

        dtoClass.implementInterface("WithId<$primaryFieldType>")
        dtoClass.addImport("${rootPackage}.dto.WithId")

        // if primary key is "id" no need to add getId() method
        if (!dtoClass.hasMethod("getId")) {
            val primaryFieldName = primaryField.name
            val method = dtoClass.addMethod("getId")
                .withModifiers(Modifier.Keyword.PUBLIC)
                .setReturnType(primaryFieldType)
                .addAnnotation(Override::class.java)
                .setBody(ReturnStmt("this.$primaryFieldName"))

            if (primaryFieldName != "id") {
                method.addAnnotation(JsonIgnore::class.java)
            }
        }
    }

    private fun addCloneableImplementation(dtoClass: JavaClassNode, entity: EntityElem) {
        dtoClass.implementInterface("Cloneable")
        dtoClass.addMethod("clone")
            .withModifiers(Modifier.Keyword.PUBLIC)
            .setReturnType(dtoClass.getName())
            .addAnnotation(Override::class.java)
            .setBody("return clone(false);".parseAsStatement())

        val statements = mutableMapOf<String, String>()
        val arguments = mutableListOf<String>()
        val dtoClassName = "${entity.name}Dto"
        entity.fields.filter { it.isNotSynthetic() }.forEach { field ->
            if (ModelTypes.STRING == field.type) {
                // final String newName = adjust ? StringUtils.abbreviate(this.name, "", 100) : this.name;
                val varName = "new" + field.name.upperFirst()
                val fieldName = "this." + field.name
                val maxLength = dtoClassName + "." + field.maxSizeConstantName()
                val statement =
                    "final String $varName = adjust ? StringUtils.abbreviate($fieldName, \"\", $maxLength) : $fieldName;\n"
                statements[varName] = statement
                arguments.add(varName)
                dtoClass.addImport(StringUtils::class.java)
            } else if (field.relation == EntityRelation.OneToMany || field.relation == EntityRelation.ManyToMany) {
                // final List<PermissionFullDto> newPermissions = this.permissions.stream().map(p -> p.clone(adjust)).collect(Collectors.toList());
                val varName = "new" + field.name.upperFirst()
                val targetType = "${field.type}FullDto"
                val statement =
                    "final List<$targetType> $varName = this.${field.name}.stream().map(p -> p.clone(adjust)).collect(Collectors.toList());\n"
                statements[varName] = statement
                arguments.add(varName)
                dtoClass.addImport(Collectors::class.java)
            } else if (field.logicField == true) {
                arguments.add("this.${field.name} != null ? this.${field.name}.clone(adjust) : null")
            } else {
                arguments.add("this.${field.name}")
            }
        }

        val body =
            statements.values.joinToString("") + "return new ${entity.getFullDtoName()}(" + arguments.joinToString(", ") + ");"
        dtoClass.addMethod("clone")
            .withModifiers(Modifier.Keyword.PUBLIC)
            .setReturnType(dtoClass.getName())
            .addParameter("boolean", "adjust")
            .setBody(body.parseAsBlock())
            .setJavadocComment(
                """Clone this object and adjust fields length due to database constraints.

@param adjust if true then adjust fields length
@return cloned object"""
            )
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
        val matchTypeFile = repositoryDir.addJavaFile("MatchType.java")
        context.writeTemplate(matchTypeFile, "/templates/jooq/MatchType.java.vm", values)
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
            val values: Map<String, Any?> = mapOf(
                "package" to rootPackage,
                "name" to name,
                "uname" to name.camelCaseToSqlUppercase(),
                "idType" to entity.getPrimaryField().asJavaType(),
                "updateAt" to entity.updatedAt,
                "createdAt" to entity.createdAt
            )
            val fields = generateFields(entity)
            val templateName = if (singlePrimary)
                "/templates/jooq/JooqTargetRepository.java.vm"
            else
                "/templates/jooq/JooqTargetRepository2.java.vm"

            context.writeTemplate(repoClassFile, templateName, values + fields)
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

        if (repositoryPhysicalPath != null) {
            val customFileName = "${name}CustomRepository.java"
            val customFilePath = repositoryPhysicalPath.resolve("custom/${customFileName}")

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
        } else {
            log.warn("Physical path is not set for directory: {}", repositoryDir.getName())
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
                val javaParser = JavaParserFactory.create()
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

    private fun generateFields(entity: EntityElem): Map<String, Any?> {
        var counter = 1
        return entity.fields.filter { it.isPrimary() }
            .map { it.name }
            .map { it.camelCaseToSnakeCase() }
            .map { "field${counter++}" to it.uppercase() }
            .toMap()
    }

    private fun processModule(module: ModuleElem): ModuleElem {
        return module.copy(schema = module.schema?.copy(entities = module.schema.entities?.map { entity ->
            processEntity(
                entity
            )
        }))
    }

    private fun processEntity(entity: EntityElem): EntityElem {
        // add custom methods for unique fields
        val uniqueFields = entity.fields.filter { it.unique == true }
        if (uniqueFields.isNotEmpty()) {
            val methodComments =
                uniqueFields.map { "getBy${it.name.capitalize()}" to "Returns {@link ${entity.name}Dto} by unique field {@link ${entity.name}Dto#${it.name}}." }
                    .toTypedArray()
            val methods = methodComments.map { it.first }.toTypedArray()
            return entity.withCustomMethods(*methods).withCustomComments(*methodComments)
        }

        return entity
    }

    private fun FieldElem.maxSizeConstantName() = this.name.camelCaseToSnakeCase().uppercase() + "_MAX_LENGTH"

    private fun FieldElem.minSizeConstantName() = this.name.camelCaseToSnakeCase().uppercase() + "_MIN_LENGTH"

    private companion object {
        val log = LoggerFactory.getLogger(JooqRepositoryFeature::class.java)!!
        const val JOOQ_SINGLE_ID_REPO = "JooqRepository.java"
        const val JOOQ_TWO_ID_REPO = "JooqRepository2.java"
        const val JOOQ_BASE_REPO = "JooqBaseRepository.java"
        const val FILE_ALREADY_EXISTS = "File already exists: {}"
        const val DEFAULT_VARCHAR_LENGTH = 255
    }
}
