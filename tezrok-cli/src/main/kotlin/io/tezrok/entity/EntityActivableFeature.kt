package io.tezrok.entity

import io.tezrok.api.GeneratorContext
import io.tezrok.api.ProcessModelPhase
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.EntityElem
import io.tezrok.api.input.FieldElem
import io.tezrok.api.input.ModuleElem
import io.tezrok.api.input.ProjectElem
import io.tezrok.api.maven.ProjectNode

/**
 * Adds "active:boolean" field to entity and related methods.
 */
internal class EntityActivableFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        // no-op
        return true
    }

    override fun processModel(project: ProjectElem, phase: ProcessModelPhase): ProjectElem {
        if (phase != ProcessModelPhase.PreProcess) {
            return project
        }

        return project.copy(modules = project.modules.map { processModule(it) })
    }

    private fun processModule(module: ModuleElem): ModuleElem {
        return module.copy(schema = module.schema?.copy(entities = module.schema.entities?.map { processEntity(it) }))
    }

    private fun processEntity(entity: EntityElem): EntityElem {
        if (entity.activable == true) {
            val activeField = entity.fields.find { it.name == DEFAULT_ACTIVE_NAME }

            if (activeField == null) {
                val newField = FieldElem(name = DEFAULT_ACTIVE_NAME, type = "boolean", required = true, defValue = DEFAULT_ACTIVE_VALUE, description = DEFAULT_ACTIVE_DESCRIPTION)
                return entity.copy(fields = entity.fields + newField)
            }

            check(activeField.type == null || activeField.type == "boolean") { "Field 'active' must be of type boolean" }
            check(activeField.required == null  || activeField.required == true) { "Field 'active' must be required" }

            return entity.copy(fields = entity.fields.map { if (it === activeField) fromField(it) else it })
        }

        return entity
    }

    private fun fromField(inheritField: FieldElem): FieldElem {
        return inheritField.copy(
            type = "boolean",
            required = true,
            defValue = inheritField.defValue ?: DEFAULT_ACTIVE_VALUE,
            description = DEFAULT_ACTIVE_DESCRIPTION
        )
    }
    private companion object {
        const val DEFAULT_ACTIVE_DESCRIPTION = "Is entity active or deleted"
        const val DEFAULT_ACTIVE_VALUE = "false"
        const val DEFAULT_ACTIVE_NAME = "active"
    }
}