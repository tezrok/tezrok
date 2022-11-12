package com.tezrok.api.tree

/**
 * Any property of a node
 */
data class NodeProperty(val name: String) {
    // TODO: add validation of name
    companion object {
        val Id = of("_id")
        val Name = of("_name")
        val Type = of("_type")
        val Child = of("_child")
        val File = of("_file")
        val FileHash = of("_fileHash")
        val FileContentType = of("_fileContentType")

        // All known properties
        val All = listOf(Id, Name, Type, Child)

        fun of(name: String): NodeProperty = NodeProperty(name)

        fun get(name: String): NodeProperty? = cache[name]

        fun getOrCreate(name: String): NodeProperty = get(name) ?: of(name)

        private val cache: Map<String, NodeProperty> = All.associateBy { it.name }
    }
}
