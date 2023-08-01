package io.tezrok.core.input

import io.tezrok.api.input.*
import io.tezrok.json.schema.Definition
import io.tezrok.json.schema.Schema
import io.tezrok.json.schema.SchemaLoader
import io.tezrok.util.JsonUtil
import io.tezrok.util.toURL
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.*

internal class ProjectElemRepository {
    fun load(projectPath: Path): ProjectElem {
        log.debug("Loading project from {}", projectPath)
        val project = JsonUtil.mapper.readValue(projectPath.toURL(), ProjectElem::class.java)

        for (module in project.modules) {
            module.schema?.let { schema ->
                if (schema.importSchema?.isNotBlank() == true) {
                    log.debug("Loading schema from {}", schema.importSchema)
                    val schemaPath = projectPath.parent.resolve(schema.importSchema!!)
                    val schemaLoader = SchemaLoader()
                    val jsonSchema = schemaLoader.load(schemaPath)
                    module.schema = schemaFromJson(jsonSchema, module.schema)
                }
            }
        }

        return project
    }

    fun schemaFromJson(jsonSchema: Schema, inheritSchema: SchemaElem? = null): SchemaElem {
        val entities = entitiesFromSchema(jsonSchema)
        val enums = enumsFromSchema(jsonSchema)
        val entitiesMap = entities.associateBy { it.name }
        val enumsMap = enums.associateBy { it.name }

        return SchemaElem(
                importSchema = inheritSchema?.importSchema,
                entities = entities.map { entity -> processEntity(entity, entitiesMap, enumsMap, inheritSchema?.entities?.find { it.name == entity.name }) },
                enums = enums
        )
    }

    private fun processEntity(entity: EntityElem, entitiesMap: Map<String, EntityElem>, enumsMap: Map<String, EnumElem>, inheritEntity: EntityElem?): EntityElem {
        return entity.copy(fields = processFields(entity, entitiesMap, enumsMap, inheritEntity))
                .copy(customRepository = inheritEntity?.customRepository)
    }

    private fun processFields(
        entity: EntityElem,
        entitiesMap: Map<String, EntityElem>,
        enumsMap: Map<String, EnumElem>,
        inheritEntity: EntityElem?
    ): List<FieldElem> {
        val fields = entity.fields.map { field ->
            processField(
                field,
                entitiesMap,
                enumsMap,
                inheritEntity?.fields?.find { it.name == field.name })
        }.toMutableList()

        if (fields.none { it.primary == true }) {
            // if no primary key is defined, add default primary key or use existing id field as primary key
            val idx = fields.indexOfFirst { it.name == "id" }

            if (idx >= 0) {
                fields[idx] = fields[idx].copy(primary = true)
            } else {
                // add default primary key at the beginning
                fields.add(0, FieldElem("id", "long", primary = true))
            }
        }

        return fields
    }

    private fun processField(field: FieldElem, entitiesMap: Map<String, EntityElem>, enumsMap: Map<String, EnumElem>, inheritField: FieldElem?): FieldElem {
        return field.copy(refEntity = (entitiesMap[field.type] ?: enumsMap[field.type]) != null)
                .copy(primary = inheritField?.primary ?: field.primary)
                .copy(type = inheritField?.type ?: field.type)
    }

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
                    fields = definition.properties?.map { fieldFromProperty(it.key, it.value, definition.required?.contains(it.key) == true) }
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
            return "dateTime"
        } else if (definition.format == "date") {
            return "date"
        }

        return definition.type ?: parseRef(definition.ref, "Type must be defined")
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
        else if (definition.ref?.isNotBlank() == true) EntityRelation.OneToOne
        else null

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ProjectElemRepository::class.java)
    }
}
