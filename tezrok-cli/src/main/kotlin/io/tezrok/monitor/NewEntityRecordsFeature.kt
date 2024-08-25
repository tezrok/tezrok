package io.tezrok.monitor

import io.tezrok.api.GeneratorContext
import io.tezrok.api.ProcessModelPhase
import io.tezrok.api.input.*
import io.tezrok.api.maven.ProjectNode
import io.tezrok.core.BaseTezrokFeature

/**
 * Generates service for all entities which gets newly created entities by id or update datetime.
 */
class NewEntityRecordsFeature : BaseTezrokFeature() {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        return true
    }

    override fun processModel(project: ProjectElem, phase: ProcessModelPhase): ProjectElem {
        if (phase != ProcessModelPhase.PreProcess) {
            return project
        }

        return project.copy(modules = project.modules.map { processModule(it) })
    }

    private fun processModule(module: ModuleElem): ModuleElem {
        val schema = module.schema
        if (module.newRecords == true && schema != null) {
            val entities = schema.entities?.associate { it.name to it }?.toMutableMap() ?: mutableMapOf()
            entities[NEW_ENTITY_RECORD] = createNewEntityRecord(entities[NEW_ENTITY_RECORD])

            return module.copy(schema = schema.copy(entities = entities.values.toList()))
        }

        return module
    }

    private fun createNewEntityRecord(inheritEntity: EntityElem?): EntityElem {
        return EntityElem(
            name = NEW_ENTITY_RECORD,
            description = "Table with information about last added entities",
            createdAt = true,
            updatedAt = true,
            skipService = inheritEntity?.skipService ?: true,
            skipController = inheritEntity?.skipController ?: true,
            fields = createNewEntityRecordFields(inheritEntity),
            init = inheritEntity?.init,
            methods = inheritEntity?.methods ?: emptySet(),
        )
    }


    private fun createNewEntityRecordFields(inheritEntity: EntityElem?): List<FieldElem> {
        return mergeFields(
            inheritEntity, listOf(
                FieldElem(name = "id", type = "Long", primary = true),
                FieldElem(
                    name = "name",
                    type = "String",
                    description = "Name of the record",
                    required = true,
                    uniqueGroup = "NAME_GROUP",
                    maxLength = NAME_MAX,
                    minLength = NAME_MIN
                ),
                FieldElem(
                    name = "user",
                    type = "User",
                    description = "Owner of the record",
                    required = true,
                    uniqueGroup = "NAME_GROUP",
                    relation = EntityRelation.ManyToOne
                ),
                FieldElem(
                    name = "fullTypeName",
                    type = "String",
                    description = "Full class name of the monitored entity",
                    required = true,
                    maxLength = TYPE_NAME_MAX,
                    minLength = TYPE_NAME_MIN
                ),
                FieldElem(
                    name = "fieldName",
                    type = "String",
                    description = "Field name of the monitored entity by which the records are monitored",
                    maxLength = FIELD_NAME_MAX,
                    minLength = 0
                ),
                FieldElem(
                    name = "lastId",
                    type = "String",
                    description = "Last max id/datetime/etc of the monitored entity",
                    maxLength = 100,
                    minLength = 0
                ),
                FieldElem(
                    name = "version",
                    type = "Long",
                    description = "Version of this record (used for optimistic locking)",
                    required = true
                )
            )
        )
    }

    private companion object {
        const val NEW_ENTITY_RECORD = "NewEntityRecord"
        const val NAME_MAX = 100
        const val NAME_MIN = 1
        const val FIELD_NAME_MAX = 50
        const val TYPE_NAME_MAX = 255
        const val TYPE_NAME_MIN = 1
    }
}
