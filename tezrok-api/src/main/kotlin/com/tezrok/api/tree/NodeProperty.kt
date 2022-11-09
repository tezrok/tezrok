package com.tezrok.api.tree

/**
 * Any property of a node
 */
data class NodeProperty(val name: String) {
    // TODO: add validation of name
    companion object {
        val Id = of("id")
        val Name = of("name")
        val Type = of("type")
        val Child = of("child")
        val File = of("file")
        val FileHash = of("fileHash")
        val FileContentType = of("fileContentType")

        // All known properties
        val All = listOf(Id, Name, Type, Child)

        fun of(name: String): NodeProperty = NodeProperty(name)

        fun get(name: String): NodeProperty? = cache[name]

        fun getOrCreate(name: String): NodeProperty = get(name) ?: of(name)

        private val cache: Map<String, NodeProperty> = All.associateBy { it.name }
    }
}
