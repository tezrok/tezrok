package io.tezrok.api.model.node

/**
 * Field of an entity
 */
class FieldNode(name: String,
                val type: String,
                parent: EntityNode,
                val primary: Boolean = false,
                val isNullable: Boolean = true,
                val unique: Boolean = false,
                val max: Long? = null,
                val min: Long? = null,
                val relation: RelationType = RelationType.None,
                description: String = "") : Node(name, KIND, description, parent) {

    override val parent: EntityNode
        get() = super.parent as EntityNode

    fun hasRelation(): Boolean = relation != RelationType.None

    fun isLazy(): Boolean = properties().any { it.isLazy() }

    fun isTransient(): Boolean = properties().any { it.isTransient() }

    companion object {
        const val KIND = "Field"
    }
}

/**
 * Transient field won't be saved into db
 */
fun Property.isTransient() = "Field.Transient" == name && true == value

fun Property.isLazy() = "Field.Transient" == name && true == value

enum class RelationType {
    None,

    OneToOne,

    OneToMany,

    ManyToOne,

    ManyToMany
}
