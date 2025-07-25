package io.tezrok.core.input

import io.tezrok.api.ProcessModelPhase
import io.tezrok.api.input.*
import io.tezrok.json.schema.Definition
import io.tezrok.json.schema.Schema
import io.tezrok.json.schema.SchemaLoader
import io.tezrok.util.JsonUtil
import io.tezrok.util.ModelTypes
import io.tezrok.util.toURL
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.isRegularFile

internal class ProjectElemRepository {
    fun load(
        projectPath: Path,
        projectModifier: (ProjectElem, ProcessModelPhase) -> ProjectElem = { project: ProjectElem, _: ProcessModelPhase -> project }
    ): ProjectElem {
        log.info("Loading project from {}", projectPath)

        check(projectPath.exists()) { "Project file not found: $projectPath" }
        check(projectPath.isRegularFile()) { "Project file is not a regular file: $projectPath" }
        check(projectPath.fileSize() > 0) { "Project file is empty: $projectPath" }

        val rawProject = JsonUtil.mapper.readValue(projectPath.toURL(), ProjectElem::class.java)
        val jsonProject =
            rawProject.copy(modules = rawProject.modules.map { module -> tryLoadModuleFromJson(module, projectPath) })
        val preProject = projectModifier(jsonProject, ProcessModelPhase.PreProcess)
        val preProjectFinal = preProject.copy(modules = preProject.modules.map { module -> normalizeModule(module) })
        val processProject = projectModifier(preProjectFinal, ProcessModelPhase.Process)
        val postProcessProject = projectModifier(processProject, ProcessModelPhase.PostProcess)

        validateProject(postProcessProject)

        return postProcessProject
    }

    private fun normalizeModule(module: ModuleElem): ModuleElem {
        if (module.schema == null) {
            return module
        }

        return module.copy(schema = normalizeSchema(module.schema!!))
    }

    private fun tryLoadModuleFromJson(module: ModuleElem, projectPath: Path): ModuleElem {
        validateDuplicateEntities(module)

        return module.copy(schema = tryLoadSchemaFromJson(module.schema, projectPath))
    }

    private fun tryLoadSchemaFromJson(schema: SchemaElem?, projectPath: Path): SchemaElem? {
        return if (schema != null) {
            if (schema.importSchema?.isNotBlank() == true) {
                // TODO: import schema from json via TezrokFeature.processModel
                log.debug("Loading schema from {}", schema.importSchema)
                val schemaPath = projectPath.parent.resolve(schema.importSchema!!)
                val schemaLoader = SchemaLoader()
                val jsonSchema = schemaLoader.load(schemaPath)
                schemaFromJson(jsonSchema, schema, normalize = false)
            } else schema
        } else
            null
    }

    private fun validateProject(project: ProjectElem) {
        project.modules.forEach { module -> validateModule(module) }
    }

    private fun validateModule(module: ModuleElem) {
        validateDuplicateEntities(module)

        module.schema?.entities?.forEach { entity -> validateEntity(entity) }
    }

    private fun validateEntity(entity: EntityElem) {
        entity.fields.groupBy { it.name }
            .map { it.key to it.value.size }
            .find { it.second > 1 }?.let { error("Found duplicate field '${it.first}' in entity '${entity.name}'") }

        entity.fields.forEach { field -> validateField(field, entity) }
    }

    private fun validateDuplicateEntities(module: ModuleElem) {
        module.schema?.entities?.groupBy { it.name }
            ?.map { it.key to it.value.size }
            ?.find { it.second > 1 }?.let { error("Found duplicate entity '${it.first}' in module '${module.name}'") }
    }

    private fun validateField(field: FieldElem, entity: EntityElem) {
        check(field.name.isNotBlank()) { "Field name cannot be blank in entity '${entity.name}'" }
        val fieldName = "${entity.name}.${field.name}"
        check(field.type?.isNotBlank() == true) { "Field '$fieldName' type is not defined" }

        if (field.name == "password" || field.name == "pwd") {
            check(field.hasMetaType(MetaType.Sensitive)) { "Field '$fieldName' must have meta type 'Sensitive'" }
        }

        if (field.isPrimary()) {
            if (field.primaryIdFrom != null) {
                check(field.primaryIdFrom >= 1) { "Field '$fieldName' primaryIdFrom must be greater than 0" }
                check(field.type == ModelTypes.LONG || field.type == ModelTypes.INTEGER)
                { "In field '$fieldName' used primaryIdFrom but type is not Long or Integer but: ${field.type}" }
                check(entity.isSinglePrimary)
                { "Entity '${entity.name}' must have only one primary field if primaryIdFrom is used" }
            }
        } else {
            check(field.primaryIdFrom == null)
            { "Field '$fieldName' primaryIdFrom must be null but found: ${field.primaryIdFrom}" }
        }
    }

    /**
     * Converts json schema to a list of entities
     *
     * inheritSchema used to customize entities with options which not supported by json schema
     *
     * @param jsonSchema json schema
     * @param inheritSchema schema inherited from parent module
     */
    fun schemaFromJson(jsonSchema: Schema, inheritSchema: SchemaElem? = null, normalize: Boolean = true): SchemaElem {
        val entitiesFromJson = entitiesFromSchema(jsonSchema)
        val jsonEntitiesNames = entitiesFromJson.map { it.name }.toSet()
        // add entities from custom defined schema
        val entities =
            entitiesFromJson + (inheritSchema?.entities ?: emptyList()).filter { !jsonEntitiesNames.contains(it.name) }
        val enums = enumsFromSchema(jsonSchema)
        val schema = SchemaElem(
            schemaName = inheritSchema?.schemaName ?: "public",
            importSchema = inheritSchema?.importSchema,
            entities = entities.map { entity ->
                processEntity(
                    entity,
                    inheritSchema?.entities?.find { it.name == entity.name })
            },
            enums = enums
        )

        return if (normalize) normalizeSchema(schema) else schema
    }

    /**
     * Creates primary fields if not defined.
     * Creates synthetic fields and entities for relations.
     */
    private fun normalizeSchema(schema: SchemaElem): SchemaElem {
        val schema = addPrimaryFields(schema)
        return addSyntheticFields(schema)
    }

    /**
     * Adds synthetic fields and entities for relations.
     */
    private fun addSyntheticFields(schema: SchemaElem): SchemaElem {
        val entities = schema.entities ?: return schema
        val entityMap = entities.associateBy { it.name }.toMutableMap()
        val syntheticFields = mutableListOf<Pair<String, FieldElem>>() // entity name, field
        entities.forEach { entity ->
            entityMap[entity.name] = entity.copy(fields = entity.fields.flatMap { field ->
                addSyntheticFields(
                    field,
                    entity,
                    entityMap,
                    syntheticFields
                )
            })
        }

        syntheticFields.forEach { syntheticField ->
            val entityName = syntheticField.first
            val entity = entityMap[entityName] ?: error("Entity \"$entityName\" not found")
            // add synthetic field to entity
            entityMap[entityName] = entity.copy(fields = entity.fields + syntheticField.second)
        }

        // keep order of entities
        return schema.copy(entities = entityMap.values.toList())
    }

    /**
     * Adds synthetic fields or entities for field if one refer to another entity.
     */
    private fun addSyntheticFields(
        sourceField: FieldElem,
        sourceEntity: EntityElem,
        entityMap: MutableMap<String, EntityElem>,
        syntheticFields: MutableList<Pair<String, FieldElem>>,
    ): List<FieldElem> {
        // TODO: check enum entity as well
        val targetEntity = entityMap[sourceField.type]

        if (targetEntity != null) {
            val logicField = sourceField.copy(logicField = true)
            val fullFieldName = "${sourceEntity.name}.${sourceField.name}"
            val targetPrimaryField = targetEntity.fields.first { it.isPrimary() }
            val sourcePrimaryField = sourceEntity.fields.first { it.isPrimary() }

            when (val relation =
                sourceField.relation ?: error("Relation is not defined for field \"$fullFieldName\"")) {
                EntityRelation.OneToOne,
                EntityRelation.ManyToOne -> {
                    val syntheticName = "${sourceField.name}Id"

                    // TODO: optimize
                    if (sourceEntity.fields.any { it.name == syntheticName }) {
                        throw IllegalArgumentException("Field with name \"$syntheticName\" already exists in entity \"${sourceEntity.name}\"")
                    }

                    val syntheticField = sourceField.copy(
                        name = syntheticName,
                        type = targetPrimaryField.type,
                        syntheticTo = fullFieldName,
                        unique = if (relation == EntityRelation.OneToOne) true else sourceField.unique,
                        foreignField = "${targetEntity.name}.${targetPrimaryField.name}",
                        description = "Synthetic field for \"${fullFieldName}\"",
                        relation = null
                    )

                    return listOf(logicField, syntheticField)
                }

                EntityRelation.OneToMany -> {
                    // add synthetic field to ref entity
                    val syntheticField = sourceField.copy(
                        name = "${sourceField.name}${sourceEntity.name}Id",
                        type = targetPrimaryField.type,
                        syntheticTo = fullFieldName,
                        foreignField = "${sourceEntity.name}.${sourcePrimaryField.name}",
                        description = "Synthetic field for \"$fullFieldName\"",
                        external = true,
                        relation = null
                    )
                    syntheticFields.add(targetEntity.name to syntheticField)

                    return listOf(logicField)
                }

                EntityRelation.ManyToMany -> {
                    val entityName = "${sourceEntity.name}${targetEntity.name}" + sourceField.name.capitalize()
                    require(!entityMap.containsKey(entityName)) { "Entity with name \"$entityName\" already exists" }
                    val (suffixSource, suffixTarget) = if (sourceEntity.name == targetEntity.name) "Source" to "Target" else "" to ""

                    // add synthetic table with two primary fields
                    val fieldName1 = "${sourceEntity.name}${suffixSource}Id"
                    val fieldName2 = "${targetEntity.name}${suffixTarget}Id"
                    entityMap[entityName] = EntityElem(
                        name = entityName,
                        syntheticTo = fullFieldName,
                        description = "Synthetic entity of many-to-many relation for field \"$fullFieldName\"",
                        methods = setOf(
                            MethodElem("findBy$fieldName1"),
                            MethodElem("findBy$fieldName2"),
                            MethodElem("findBy${fieldName1}In"),
                            MethodElem("findBy${fieldName2}In")
                        ),
                        fields = listOf(
                            FieldElem(
                                fieldName1.decapitalize(),
                                sourcePrimaryField.type,
                                primary = true,
                                syntheticTo = fullFieldName,
                                foreignField = "${sourceEntity.name}.${sourcePrimaryField.name}",
                            ),
                            FieldElem(
                                fieldName2.decapitalize(),
                                targetPrimaryField.type,
                                primary = true,
                                syntheticTo = fullFieldName,
                                foreignField = "${targetEntity.name}.${targetPrimaryField.name}",
                            )
                        )
                    )
                    return listOf(logicField)
                }

                else -> TODO("relation type: $relation")
            }
        }

        return listOf(sourceField)
    }

    private fun addPrimaryFields(schema: SchemaElem): SchemaElem {
        return schema.copy(entities = schema.entities?.map { entity -> addPrimaryFields(entity) })
    }

    private fun addPrimaryFields(entity: EntityElem): EntityElem {
        val fields = entity.fields.toMutableList()

        if (fields.none { it.isPrimary() }) {
            // if no primary key is defined, add default primary key or use existing id field as primary key
            val idx = fields.indexOfFirst { it.name == PRIMARY_FIELD_NAME }

            if (idx >= 0) {
                fields[idx] = fields[idx].copy(primary = true)
            } else {
                // add default primary key at the beginning
                fields.add(0, FieldElem(PRIMARY_FIELD_NAME, ModelTypes.LONG, primary = true))
            }
        }

        return entity.copy(fields = fields)
    }

    private fun processEntity(entity: EntityElem, inheritEntity: EntityElem?): EntityElem {
        return entity.copy(
            description = inheritEntity?.description ?: entity.description,
            customRepository = inheritEntity?.customRepository ?: entity.customRepository,
            methods = inheritEntity?.methods ?: entity.methods,
            stdMethodProps = inheritEntity?.stdMethodProps ?: entity.stdMethodProps,
            activable = inheritEntity?.activable ?: entity.activable,
            createdAt = inheritEntity?.createdAt ?: entity.createdAt,
            updatedAt = inheritEntity?.updatedAt ?: entity.updatedAt,
            fields = processFields(entity, inheritEntity)
        )
    }

    private fun processFields(
        entity: EntityElem,
        inheritEntity: EntityElem?
    ): List<FieldElem> {
        val inheritFields = inheritEntity?.fields?.associateBy { it.name }?.toMutableMap() ?: mutableMapOf()
        val fields = entity.fields.map { field -> processField(field, inheritFields.remove(field.name)) }
        // add fields which not defined in imported entity but defined in custom entity
        return fields + inheritFields.values
    }

    private fun processField(field: FieldElem, inheritField: FieldElem?): FieldElem =
        field.copy(
            primary = inheritField?.primary ?: field.primary,
            type = inheritField?.type ?: field.type,
            required = inheritField?.required ?: field.required,
            unique = inheritField?.unique ?: field.unique,
            uniqueGroup = inheritField?.uniqueGroup ?: field.uniqueGroup,
            description = inheritField?.description ?: field.description,
            defValue = inheritField?.defValue ?: field.defValue,
            relation = inheritField?.relation ?: field.relation
        )

    private fun entitiesFromSchema(schema: Schema): List<EntityElem> {
        if (schema.title?.isNotBlank() == true) {
            // when schema is a single entity
            return listOf(entityFromDefinition(schema.title, schema))
        }

        return schema.definitions?.map { entityFromDefinition(it.key, it.value) } ?: emptyList()
    }

    private fun enumsFromSchema(schema: Schema): List<EnumElem> =
        schema.definitions?.map { enumFromDefinition(it.key, it.value) }?.filterNotNull() ?: emptyList()

    private fun enumFromDefinition(name: String, definition: Definition): EnumElem? =
        if (definition.enum?.isNotEmpty() == true) {
            EnumElem(
                name = typeFromDefinition(definition, name),
                values = definition.enum
            )
        } else {
            null
        }

    private fun entityFromDefinition(name: String, definition: Definition): EntityElem =
        EntityElem(
            name = name,
            description = definition.description,
            fields = definition.properties?.map {
                fieldFromProperty(
                    it.key,
                    it.value,
                    definition.required?.contains(it.key) == true
                )
            }
                ?: emptyList()
        )

    private fun fieldFromProperty(name: String, definition: Definition, required: Boolean): FieldElem =
        FieldElem(
            name = name,
            type = typeFromDefinition(definition, name),
            description = definition.description,
            required = required,
            pattern = definition.pattern,
            minLength = definition.minLength,
            maxLength = definition.maxLength,
            relation = relationFromDefinition(definition)
        )

    private fun typeFromDefinition(definition: Definition, name: String): String {
        if (definition.enum?.isNotEmpty() == true) {
            // if enum is defined, then it will be separate enum EnumElem
            return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }

        if (definition.isArray()) {
            return parseRef(definition.items?.ref, "Array type must have ref property")
        }

        if (definition.format == "date-time") {
            return ModelTypes.DATETIME
        } else if (definition.format == "date") {
            return ModelTypes.DATE
        }

        return when (definition.type) {
            "string" -> ModelTypes.STRING
            "integer" -> ModelTypes.INTEGER
            "long" -> ModelTypes.LONG
            "number" -> ModelTypes.DOUBLE
            "boolean" -> ModelTypes.BOOLEAN
            null -> parseRef(definition.ref, "Type must be defined")
            else -> error("Unknown type: ${definition.type}")
        }
    }

    /**
     *  Parses string like '#/definitions/Item' and get 'Item' part
     *
     *  Throws IllegalArgumentException if ref is not valid
     */
    private fun parseRef(ref: String?, msg: String): String {
        // TODO: proper parse
        if (ref?.isNotBlank() == true && ref.startsWith("#/definitions/")) {
            val index = ref.lastIndexOf('/')
            if (index > 0) {
                return ref.substring(index + 1)
            }
        }

        throw IllegalArgumentException(msg)
    }

    private fun relationFromDefinition(definition: Definition): EntityRelation? =
        if (definition.isArray()) EntityRelation.OneToMany
        else if (definition.ref?.isNotBlank() == true) EntityRelation.ManyToOne
        else null

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ProjectElemRepository::class.java)!!
        const val PRIMARY_FIELD_NAME = "id"
    }
}
