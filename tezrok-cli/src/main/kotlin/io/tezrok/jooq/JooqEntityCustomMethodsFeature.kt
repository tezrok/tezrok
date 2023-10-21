package io.tezrok.jooq

import io.tezrok.api.GeneratorContext
import io.tezrok.api.ProcessModelPhase
import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.*
import io.tezrok.api.maven.ProjectNode
import org.slf4j.LoggerFactory

/**
 * Add custom methods to entities with relations.
 *
 * Fill [EntityElem#customMethods].
 */
internal class JooqEntityCustomMethodsFeature : TezrokFeature {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        log.error("Not yet implemented")
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
        val primaryFieldName = lazy {
            val primaryFields = entity.fields.filter { it.primary == true }
            check(primaryFields.size == 1) { "Entity ${entity.name} expected have exactly one primary field" }
            entity.name.capitalize() + primaryFields.first().name.capitalize()
        }

        for (field in entity.fields.filter { it.relation == EntityRelation.ManyToMany }) {
            val refEntity = entities[field.type] ?: error("Entity ${field.type} not found")
            val methodName = "find${entity.name}" + field.name.capitalize() + "By${primaryFieldName.value}"
            entities[refEntity.name] = refEntity.withCustomMethod(methodName)
        }

        for (field in entity.fields.filter { it.relation == EntityRelation.OneToMany }) {
            val refEntity = entities[field.type] ?: error("Entity ${field.type} not found")
            val syntheticTo = entity.name + "." + field.name
            val syntheticField = refEntity.fields.find { it.syntheticTo == syntheticTo } ?: error("Synthetic field $syntheticTo not found")
            val methodName = "find${entity.name}${field.name.capitalize()}By${refEntity.name}${syntheticField.name.capitalize()}"
            entities[refEntity.name] = refEntity.withCustomMethod(methodName)
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(JooqEntityCustomMethodsFeature::class.java)!!
    }
}