package io.tezrok.entity

import io.tezrok.api.GeneratorContext
import io.tezrok.api.ProcessModelPhase
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.*
import io.tezrok.api.maven.ProjectNode

/**
 * Adds fields "createdAt" and "updatedAt" to entity and related methods.
 */
internal class EntityCreatedUpdatedFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        return true
    }

    override fun processModel(project: ProjectElem, phase: ProcessModelPhase): ProjectElem {
        if (phase != ProcessModelPhase.Process) {
            return project
        }

        return project.copy(modules = project.modules.map { processModule(it) })
    }

    private fun processModule(module: ModuleElem): ModuleElem {
        return module.copy(schema = module.schema?.copy(entities = module.schema.entities?.map { processEntity(it) }))
    }

    private fun processEntity(entity: EntityElem): EntityElem {
        val newFields = if (entity.createdAt == true) {
            addFieldOrInherit(entity.fields, DEFAULT_CREATED_NAME, DEFAULT_CREATED_DESCRIPTION, MetaType.CreatedAt)
        } else {
            entity.fields
        }
        val newFields2 = if (entity.updatedAt == true) {
            addFieldOrInherit(newFields, DEFAULT_UPDATED_NAME, DEFAULT_UPDATED_DESCRIPTION, MetaType.UpdatedAt)
        } else {
            newFields
        }

        return entity.copy(fields = newFields2)
    }

    private fun addFieldOrInherit(fields: List<FieldElem>, name: String, description: String, metaType: MetaType?): List<FieldElem> {
        val foundField = fields.find { it.name == name }

        if (foundField == null) {
            val newField = FieldElem(
                name = name,
                type = "DateTimeTZ",
                required = true,
                defValue = DATETIME_NOW,
                description = description,
                metaType = metaType
            )
            return fields + newField
        }

        check(foundField.type == null || foundField.type == "DateTimeTZ") { "Field '$name' must be of type dateTime" }
        check(foundField.required == null || foundField.required == true) { "Field '$name' must be required" }

        return fields.map { if (it === foundField) fromField(it, description) else it }
    }

    private fun fromField(inheritField: FieldElem, description: String): FieldElem {
        return inheritField.copy(
            type = "DateTime",
            required = true,
            defValue = inheritField.defValue ?: DATETIME_NOW,
            description = inheritField.description ?: description
        )
    }

    private companion object {
        const val DEFAULT_CREATED_NAME = "createdAt"
        const val DEFAULT_CREATED_DESCRIPTION = "Date and time of entity creation"
        const val DEFAULT_UPDATED_NAME = "updatedAt"
        const val DEFAULT_UPDATED_DESCRIPTION = "Date and time of entity update"
        const val DATETIME_NOW = "now()"
    }
}
