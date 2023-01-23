package com.tezrok.api.tree

class NodeAction private constructor(val name: String) {
    companion object {
        /**
         * Action of adding or creating
         */
        val Add = of("Add")

        /**
         * Action of updating
         */
        val Edit = of("Edit")

        /**
         * Action of removing
         */
        val Delete = of("Delete")

        val All = listOf(Add, Edit, Delete)

        fun of(name: String): NodeAction = NodeAction(name)

        fun get(name: String): NodeAction? = cache[name]

        fun getOrCreate(name: String): NodeAction = get(name) ?: of(name)

        private val cache: Map<String, NodeAction> = All.associateBy { it.name }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NodeAction) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int = name.hashCode()
}
