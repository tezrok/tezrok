package com.tezrok.api.tree

/**
 * Name of the property
 */
data class PropertyName(val name: String, val description: String) {
    fun isSystem(): Boolean = name.startsWith(SYSTEM_PREFIX)

    override fun toString(): String = name

    // TODO: add validation of name
    companion object {
        val Id = system("id", "Unique id of the node")
        val Name = system("name", "Name of the node")
        val Type = system("type", "Type of the node")
        val Child = system("child", "Node's child")
        val Disabled = system("disabled", "Node is disabled")
        val Deleted = system("deleted", "Node is deleted")
        val File = system("file", "File reference of the node")
        val FileHash = system("fileHash", "Hash of file content")
        val FileContentType = system("fileContentType", "File content mime-type")

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

        private fun system(name: String, description: String) = of(SYSTEM_PREFIX + name, description)

        private val cache: Map<String, PropertyName> = All.associateBy { it.name }

        private const val SYSTEM_PREFIX = "system."
    }
}
