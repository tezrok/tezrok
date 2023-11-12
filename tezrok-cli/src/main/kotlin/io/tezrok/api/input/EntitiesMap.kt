package io.tezrok.api.input

import io.tezrok.util.upperFirst

/**
 * Encapsulates entities list and provides convenient access and modification methods.
 */
data class EntitiesMap(private val entitiesIn: List<EntityElem>) {
    private val entitiesMap = entitiesIn.associateBy { it.name }.toMutableMap()

    val entities: List<EntityElem> get() = entitiesMap.values.toList()

    fun getRefField(field: FieldElem): FieldElem {
        if (field.logicField == true) {
            return field
        }
        // find field referred in syntheticTo
        val syntheticTo = field.syntheticTo ?: error("syntheticTo property expected in field ${field.name}")
        val parts = syntheticTo.split(".")
        if (parts.size != 2) {
            error("Invalid syntheticTo property in field ${field.name}")
        }
        val refEntity = entitiesMap[parts[0]]
            ?: error("Entity not found: ${parts[0]} referred in syntheticTo property of field ${field.name}")
        return refEntity.getField(parts[1])
    }

    fun getSyntheticField(entity: EntityElem, field: FieldElem): FieldElem {
        val refEntity = this[field.type!!]
        val syntheticTo = entity.name + "." + field.name
        return refEntity.fields.find { it.syntheticTo == syntheticTo }
            ?: error("Synthetic field $syntheticTo not found")
    }

    fun getMethodByField(entity: EntityElem, field: FieldElem, vararg types: ManyToManyMethod): Map<String, String> {
        check(entity.getPrimaryFieldCount() == 1) { "Entity ${entity.name} expected have exactly one primary field" }

        val primaryField = entity.getPrimaryField()
        val refEntity = this[field.type!!]
        val primaryFieldName = entity.name.upperFirst() + primaryField.name.upperFirst()
        val result = mutableMapOf<String, String>()
        val methodName = "find${entity.name}" + field.name.upperFirst() + "By${primaryFieldName}"

        types.forEach { type ->
            when (type) {
                ManyToManyMethod.FindRefEntitiesByPrimaryField -> {
                    result[methodName] =
                        "Returns list of {@link ${refEntity.name}Dto} by primary field to support ManyToMany relation for field {@link ${entity.name}FullDto#${field.name}}."
                }

                ManyToManyMethod.FindRefEntitiesByPrimaryFieldIn -> {
                    result[methodName + "In"] =
                        "Returns list of {@link ${refEntity.name}Dto} by primary fields to support ManyToMany relation for field {@link ${entity.name}FullDto#${field.name}}."
                }
            }
        }

        return result
    }

    /**
     * Returns map of method name to JavaDoc related to OneToMany relation.
     */
    fun getMethodByField(entity: EntityElem, field: FieldElem, vararg types: OneToManyMethod): Map<String, String> {
        val refEntity = this[field.type!!]
        val syntheticFieldName = getSyntheticField(entity, field).name.upperFirst()
        val toSupport = "to support OneToMany relation for field {@link ${entity.name}FullDto#${field.name}}"
        val result = mutableMapOf<String, String>()

        types.forEach { type ->
            when (type) {
                OneToManyMethod.FindEntitiesByRefSyntheticField -> {
                    val methodName =
                        "find${entity.name}${field.name.upperFirst()}By${refEntity.name}${syntheticFieldName}"
                    result[methodName] = "Returns list of {@link ${refEntity.name}Dto} $toSupport."
                }

                OneToManyMethod.FindRefPrimaryFieldByRefSyntheticField -> {
                    val refPrimaryField = refEntity.getPrimaryField()
                    val methodName = "find${refPrimaryField.name.upperFirst()}By$syntheticFieldName"
                    result[methodName] = "Returns list of primary field of {@link ${refEntity.name}Dto} $toSupport."
                }

                OneToManyMethod.FindRefIdFieldsByRefSyntheticFields -> {
                    val refEntityIdFields = refEntity.getIdFields()
                    val allIds = refEntityIdFields.joinToString("") { it.name.upperFirst() }
                    val allIdsJavaDoc = refEntityIdFields.joinToString(", ") { it.name }
                    val methodName = "find${allIds}By${syntheticFieldName}In"
                    result[methodName] =
                        "Returns specified fields ($allIdsJavaDoc) of {@link ${refEntity.name}Dto} into custom class $toSupport"
                }
            }
        }

        return result
    }

    fun getRelationRepository(entity: EntityElem, field: FieldElem): String =
        getRelationEntity(entity, field).name + "Repository"

    fun getRelationEntity(entity: EntityElem, field: FieldElem): EntityElem {
        val fieldRefName = "${entity.name}.${field.name}"
        return entitiesMap.values.find { it.syntheticTo == fieldRefName }
            ?: error("Relation entity not found for field $fieldRefName")
    }

    operator fun get(name: String): EntityElem = entitiesMap[name] ?: error("Entity not found: $name")

    operator fun set(name: String, value: EntityElem) {
        entitiesMap[name] = value
    }

    companion object {
        fun from(entities: List<EntityElem>) = EntitiesMap(entities)
    }
}

enum class OneToManyMethod {
    FindEntitiesByRefSyntheticField,

    FindRefPrimaryFieldByRefSyntheticField,

    FindRefIdFieldsByRefSyntheticFields
}

enum class ManyToManyMethod {
    FindRefEntitiesByPrimaryField,

    FindRefEntitiesByPrimaryFieldIn
}
