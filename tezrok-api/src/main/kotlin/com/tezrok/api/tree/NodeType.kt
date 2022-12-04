package com.tezrok.api.tree

/**
 * Node type definition and known types
 */
data class NodeType(val name: String, val description: String) {
    companion object {
        val Root = of("Root", "Root of the tree")

        /**
         * Node cannot contains children
         */
        val Item = of("Item", "Ordinal node without children")

        /**
         * Node can contains children
         */
        val Directory = of("Directory", "Node with probably children")

        /**
         * Node contains file reference. By default, empty file is created
         */
        val File = of("File", "Node is file reference")

        val All = listOf(Root, Item, Directory, File)

        fun of(name: String, description: String): NodeType = NodeType(name, description)

        fun get(name: String): NodeType? = cache[name]

        fun getOrCreate(name: String, description: String = ""): NodeType = get(name) ?: of(name, description)

        private val cache: Map<String, NodeType> = All.associateBy { it.name }
    }
}
