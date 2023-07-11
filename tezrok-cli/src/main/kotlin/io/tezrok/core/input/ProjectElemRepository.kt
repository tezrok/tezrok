package io.tezrok.core.input

import io.tezrok.api.input.EntityElem
import io.tezrok.api.input.EnumElem
import io.tezrok.api.input.FieldElem
import io.tezrok.api.input.ProjectElem
import io.tezrok.schema.Definition
import io.tezrok.schema.Schema
import io.tezrok.schema.SchemaLoader
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
            if (module.importSchema.isNotBlank()) {
                log.debug("Loading schema from {}", module.importSchema)
                val schemaPath = projectPath.parent.resolve(module.importSchema)
                val schemaLoader = SchemaLoader()
                val schema = schemaLoader.load(schemaPath)
                // TODO: remove schema property and use entities instead
                module.schema = schema
                module.entities = entitiesFromSchema(schema)
                module.enums = enumsFromSchema(schema)
                // TODO: validate entities
            }
        }

        return project
    }

    private fun entitiesFromSchema(schema: Schema): List<EntityElem> =
            schema.definitions?.map { entityFromDefinition(it.key, it.value) } ?: emptyList()

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
                    relation = definition.relation
            )

    private fun typeFromDefinition(definition: Definition, name: String): String {
        if (definition.enum?.isNotEmpty() == true) {
            // if enum is defined, then it will be separate enum EnumElem
            return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }

        if (definition.isArray()) {
            val ref = definition.items?.ref ?: ""

            if (ref.isNotBlank()) {
                // parse string like '#/definitions/Item'
                val index = ref.lastIndexOf('/')
                if (index > 0) {
                    return ref.substring(index + 1)
                }
            }

            throw IllegalArgumentException("Array type must have ref property")
        }

        return definition.type ?: throw IllegalArgumentException("Type must be defined")
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ProjectElemRepository::class.java)
    }
}
