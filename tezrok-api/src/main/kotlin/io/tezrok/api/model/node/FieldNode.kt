package io.tezrok.api.model.node

/**
 * Field of an entity
 */
class FieldNode(name: String,
                val type: String,
                val value: Any?,
                val primary: Boolean = false,
                val isNullable: Boolean = true,
                val unique: Boolean = false,
                val max: Long = Long.MAX_VALUE,
                val min: Long = Long.MIN_VALUE,
                description: String = "") : Node(name, KIND, description) {
    companion object {
        const val KIND = "Field"
    }
}

/**
 * Transient field won't be saved into db
 */
fun Property.isTransient() = "Field.Transient" == name && true == value
