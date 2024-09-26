package io.tezrok.web

import io.tezrok.api.GeneratorContext
import io.tezrok.api.ProcessModelPhase
import io.tezrok.api.input.EntityElem
import io.tezrok.api.input.FieldElem
import io.tezrok.api.input.ModuleElem
import io.tezrok.api.input.ProjectElem
import io.tezrok.api.maven.ProjectNode
import io.tezrok.core.BaseTezrokFeature

/**
 * Create UserAgent entity.
 */
internal class UserAgentFeature : BaseTezrokFeature() {
    override fun apply(project: ProjectNode, context: GeneratorContext): Boolean {
        val module = project.getSingleModule()
        val moduleElem = context.getProject().getModule(module.getName())
        return moduleElem.isUserAgent()
    }

    override fun processModel(project: ProjectElem, phase: ProcessModelPhase): ProjectElem {
        if (phase != ProcessModelPhase.PreProcess) {
            return project
        }

        return project.copy(modules = project.modules.map { processModule(it) })
    }

    private fun processModule(module: ModuleElem): ModuleElem {
        if (module.isUserAgent()) {
            val schema = module.schema
            if (schema != null) {
                val entities = schema.entities?.associate { it.name to it }?.toMutableMap() ?: mutableMapOf()
                entities[ENTITY_NAME] = createEntityElem(entities[ENTITY_NAME])

                return module.copy(schema = schema.copy(entities = entities.values.toList()))
            }
        }

        return module
    }

    private fun createEntityElem(inheritEntity: EntityElem?): EntityElem {
        return EntityElem(
            name = ENTITY_NAME,
            description = "Immutable entity which stores UserAgent",
            createdAt = true,
            updatedAt = false, // UserAgent is immutable entity
            skipService = inheritEntity?.skipService ?: false,
            skipController = inheritEntity?.skipController ?: false,
            fields = createEntityFields(inheritEntity),
            init = inheritEntity?.init,
            methods = inheritEntity?.methods ?: emptySet(),
            stdMethodProps = applyAdminRole(inheritEntity?.stdMethodProps)
        )
    }

    private fun createEntityFields(inheritEntity: EntityElem?): List<FieldElem> {
        return mergeFields(
            inheritEntity, listOf(
                FieldElem(name = "id", type = "Long", primary = true),
                FieldElem(
                    name = "value",
                    type = "String",
                    description = "Value of UserAgent",
                    required = true,
                    unique = true,
                    maxLength = VALUE_MAX,
                    minLength = VALUE_MIN
                )
            )
        )
    }

    private companion object {
        const val ENTITY_NAME = "UserAgent"
        const val VALUE_MAX = 512
        const val VALUE_MIN = 1
    }
}
