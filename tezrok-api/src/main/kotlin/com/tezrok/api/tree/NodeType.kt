package com.tezrok.api.tree

/**
 * Node type definition and known types
 */
data class NodeType(val name: String, val description: String) {
    companion object {
        @JvmField
        val Any = of("Any", "Any node")

        /**
         * Root node contains only Module nodes and common properties
         */
        @JvmField
        val Root = of("Root", "Root of the tree")

        /**
         * Node cannot contains children
         */
        @JvmField
        val Item = of("Item", "Ordinal node without children")

        /**
         * Node can contains children
         */
        @JvmField
        val Directory = of("Directory", "Node with probably children")

        /**
         * Module atomic entity and can contain other module related nodes
         */
        @JvmField
        val Module = of("Module", "Module directory")

        /**
         * Module source code. Module can contain only one CodeRoot node
         */
        @JvmField
        val CodeRoot = of("CodeRoot", "Module code directory")

        /**
         * Node contains file reference. By default, empty file is created
         */
        @JvmField
        val File = of("File", "Node is file reference")

        /**
         * All known types
         */
        @JvmField
        val All: Set<NodeType> = hashSetOf(Any, Root, Module, CodeRoot, Item, Directory, File)

        /**
         * Creates new [NodeType]
         */
        fun of(name: String, description: String): NodeType = NodeType(name, description)

        /**
         * Return type by name if exists
         */
        fun get(name: String): NodeType? = cache[name]

        /**
         * Return type by name or creates new one
         */
        fun getOrCreate(name: String, description: String = ""): NodeType = get(name) ?: of(name, description)

        private val cache: Map<String, NodeType> = All.associateBy { it.name }
    }
}
