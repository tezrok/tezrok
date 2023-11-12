package io.tezrok.api.input

data class EntitiesMap(val entities: List<EntityElem>) {
    private val entitiesMap = entities.associateBy { it.name }

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

    fun getEntity(name: String): EntityElem = entitiesMap[name] ?: error("Entity not found: $name")

    companion object {
        fun from(entities: List<EntityElem>) = EntitiesMap(entities)
    }
}
