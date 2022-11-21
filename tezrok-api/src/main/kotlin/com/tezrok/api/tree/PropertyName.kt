package com.tezrok.api.tree

/**
 * Name of the property
 */
data class PropertyName(val name: String) {
    // TODO: add validation of name
    companion object {
        val Id = of("_id")
        val Name = of("_name")
        val Type = of("_type")
        val Child = of("_child")
        val Disabled = of("_disabled")
        val Deleted = of("_deleted")
        val File = of("_file")
        val FileHash = of("_fileHash")
        val FileContentType = of("_fileContentType")

        // All known properties
        val All = listOf(Id, Name, Type, Child, Disabled, Deleted, File, FileHash, FileContentType)

        fun of(name: String): PropertyName = PropertyName(name)

        fun get(name: String): PropertyName? = cache[name]

        fun getOrCreate(name: String): PropertyName = get(name) ?: of(name)

        private val cache: Map<String, PropertyName> = All.associateBy { it.name }
    }
}
