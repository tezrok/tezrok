package io.tezrok.jooq

import io.tezrok.api.GeneratorContext
import io.tezrok.api.ProcessModelPhase
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.*
import io.tezrok.api.maven.ProjectNode
import io.tezrok.util.getFindAllIdFieldsByPrimaryIdInMethodName
import org.slf4j.LoggerFactory

/**
 * Add custom methods to entities with relations.
 *
 * Fill [EntityElem#customMethods].
 */
internal class JooqEntityCustomMethodsFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        return true
    }

    /**
     * Fill [EntityElem#customMethods].
     */
    override fun processModel(project: ProjectElem, phase: ProcessModelPhase): ProjectElem {
        if (phase != ProcessModelPhase.Process) {
            // synthetic fields should be added before, so we need Process phase
            return project
        }

        return project.copy(modules = project.modules.map { processModule(it) })
    }

    private fun processModule(module: ModuleElem): ModuleElem {
        val schema = module.schema ?: SchemaElem()
        val entities = (schema.entities ?: emptyList()).associateBy { it.name }.toMutableMap()
        entities.values.forEach { entity -> processEntity(entity, entities) }

        return module.copy(schema = schema.copy(entities = entities.values.toList()))
    }

    /**
     * Add custom methods by entity relations.
     */
    private fun processEntity(entity: EntityElem, entities: MutableMap<String, EntityElem>) {
        val primaryField = lazy {
            val primaryFields = entity.fields.filter { it.primary == true }
            check(primaryFields.size == 1) { "Entity ${entity.name} expected have exactly one primary field" }
            primaryFields[0]
        }
        val idFields = lazy { entity.getIdFields() }

        for (field in entity.fields.filter { it.relation == EntityRelation.ManyToMany }) {
            val refEntity = entities[field.type] ?: error("Entity ${field.type} not found")
            val primaryFieldName = entity.name.capitalize() + primaryField.value.name.capitalize()
            val methodName = "find${entity.name}" + field.name.capitalize() + "By${primaryFieldName}"
            val methodName2 = "find${entity.name}" + field.name.capitalize() + "By${primaryFieldName}In"
            entities[refEntity.name] = refEntity.withCustomMethods(methodName, methodName2)
                .withCustomComments(methodName to "Returns list of {@link ${refEntity.name}Dto} to support ManyToMany relation for field {@link ${entity.name}FullDto#${field.name}}.",
                    methodName2 to "Returns list of primary field of {@link ${refEntity.name}Dto} to support ManyToMany relation for field {@link ${entity.name}FullDto#${field.name}}.")
        }

        for (field in entity.fields.filter { it.relation == EntityRelation.OneToMany }) {
            // TODO: add index for foreign key!!!
            val refEntity = entities[field.type] ?: error("Entity ${field.type} not found")
            val syntheticTo = entity.name + "." + field.name
            val syntheticField = refEntity.fields.find { it.syntheticTo == syntheticTo }
                ?: error("Synthetic field $syntheticTo not found")
            val syntheticFieldName = syntheticField.name?.capitalize()
            val methodName = "find${entity.name}${field.name.capitalize()}By${refEntity.name}${syntheticFieldName}"
            val refPrimaryField = refEntity.getPrimaryField()
            val refEntityIdFields = refEntity.getIdFields()
            val allIds = refEntityIdFields.joinToString("") { it.name.capitalize() }
            val allIdsJavaDoc = refEntityIdFields.joinToString(", ") { it.name }
            val methodName2 = "find${refPrimaryField.name.capitalize()}By$syntheticFieldName"
            val methodName3 = "find${allIds}By${syntheticFieldName}In"
            val toSupport = "to support OneToMany relation for field {@link ${entity.name}FullDto#${field.name}}"
            entities[refEntity.name] = refEntity.withCustomMethods(methodName, methodName2, methodName3)
                .withCustomComments(methodName to "Returns list of {@link ${refEntity.name}Dto} $toSupport.",
                    methodName2 to "Returns list of primary field of {@link ${refEntity.name}Dto} $toSupport.",
                    methodName3 to "Returns specified fields ($allIdsJavaDoc) of {@link ${refEntity.name}Dto} into custom class $toSupport")
        }

        if (entity.isNotSynthetic()) {
            // make helper methods for EntityGraphLoader
            // findAllIdsByPrimaryIdIn(Collection<ID> ids, Class<T> type)
            if (idFields.value.size > 1) {
                val entity = entities[entity.name] ?: error("Entity ${entity.name} not found")
                val allIdsJavaDoc = entity.getIdFields().joinToString(", ") { it.name }
                val methodName = entity.getFindAllIdFieldsByPrimaryIdInMethodName()
                entities[entity.name] = entity.withCustomMethods(methodName)
                    .withCustomComments(methodName to "Returns ID fields ($allIdsJavaDoc) of {@link ${entity.name}Dto} into custom class.")
            }
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(JooqEntityCustomMethodsFeature::class.java)!!
    }
}