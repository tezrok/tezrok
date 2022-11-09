package com.tezrok.api.tree

/**
 * Node type definition and known types
 */
data class NodeType(val name: String) {
    companion object {
        /**
         * Node cannot contains children
         */
        val Item = of("Item")

        /**
         * Node can contains children
         */
        val Container = of("Container")

        /**
         * Node contains file reference. By default, empty file is created
         */
        val File = of("File")

        val All = listOf(Item, Container, File)

        fun of(name: String): NodeType = NodeType(name)

        fun get(name: String): NodeType? = cache[name]

        fun getOrCreate(name: String): NodeType = get(name) ?: of(name)

        private val cache: Map<String, NodeType> = All.associateBy { it.name }
    }
}
