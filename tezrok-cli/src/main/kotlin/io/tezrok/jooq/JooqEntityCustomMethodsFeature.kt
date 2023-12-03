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
        val entities = EntitiesMap.from(schema.entities ?: emptyList())
        entities.entities.forEach { entity -> processEntity(entity, entities) }

        return module.copy(schema = schema.copy(entities = entities.entities))
    }

    /**
     * Add custom methods by entity relations.
     */
    private fun processEntity(entity: EntityElem, entities: EntitiesMap) {
        for (field in entity.fields.filter { it.relation == EntityRelation.ManyToMany }) {
            val refEntity = entities[field.type!!]
            val methods = entities.getMethodByField(
                entity,
                field,
                ManyToManyMethod.FindRefEntitiesByPrimaryField,
                ManyToManyMethod.FindRefEntitiesByPrimaryFieldIn
            )
            entities[refEntity.name] = refEntity
                .withCustomMethods(*methods.keys.toTypedArray())
                .withCustomComments(*methods.map { it.key to it.value }.toTypedArray())
        }

        for (field in entity.fields.filter { it.relation == EntityRelation.OneToMany }) {
            // TODO: add index for foreign key!!!
            val refEntity = entities[field.type!!]
            val methods = entities.getMethodByField(
                entity,
                field,
                OneToManyMethod.FindEntitiesByRefSyntheticField,
                OneToManyMethod.FindRefPrimaryFieldByRefSyntheticField,
                OneToManyMethod.FindRefIdFieldsByRefSyntheticFields
            )
            entities[refEntity.name] = refEntity
                .withCustomMethods(*methods.keys.toTypedArray())
                .withCustomComments(*methods.map { it.key to it.value }.toTypedArray())
        }

        if (entity.isNotSynthetic()) {
            // make helper methods for EntityGraphLoader
            val methods = entities.getIdFieldsMethods(entity)
            entities[entity.name] = entities[entity.name]
                .withCustomMethods(*methods.keys.toTypedArray())
                .withCustomComments(*methods.map { it.key to it.value }.toTypedArray())
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(JooqEntityCustomMethodsFeature::class.java)!!
    }
}