package com.tezrok.api.tree

/**
 * Name of the property
 */
data class PropertyName(val name: String, val description: String) {
    /**
     * Returns true if property is system (known) property
     */
    fun isSystem(): Boolean = cache.containsKey(name)

    override fun toString(): String = name

    // TODO: add validation of name
    companion object {
        val Id = of("id", "Unique id of the node")
        val Name = of("name", "Name of the node")
        val Type = of("type", "Type of the node")
        val Child = of("child", "Node's child")
        val Disabled = of("disabled", "Node is disabled")
        val Deleted = of("deleted", "Node is deleted")
        val File = of("file", "File reference of the node")
        val FileHash = of("fileHash", "Hash of file content")
        val FileContentType = of("fileContentType", "File content mime-type")

        // All system properties
        val All = listOf(Id, Name, Type, Child, Disabled, Deleted, File, FileHash, FileContentType)

        fun of(name: String, description: String): PropertyName = PropertyName(name, description)

        /**
         * Return system property by name
         */
        fun get(name: String): PropertyName? = cache[name]

        /**
         * Return system property by name or creates new one
         */
        fun getOrCreate(name: String, description: String = ""): PropertyName = get(name) ?: of(name, description)

        private val cache: Map<String, PropertyName> = All.associateBy { it.name }
    }
}
