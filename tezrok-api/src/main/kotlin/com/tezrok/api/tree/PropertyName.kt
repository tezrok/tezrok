package com.tezrok.api.tree

/**
 * Name of the property
 *
 * If property starts with underscore, it is considered as hidden and not shown in UI
 */
data class PropertyName(val name: String, val description: String) {
    /**
     * Returns true if property is system (known) property
     */
    fun isSystem(): Boolean = cache.containsKey(name)

    override fun toString(): String = name

    // TODO: add validation of name
    companion object {
        @JvmField
        val Id = of("id", "Unique id of the node")

        @JvmField
        val Name = of("name", "Name of the node")

        @JvmField
        val Type = of("type", "Type of the node")

        @JvmField
        val Child = of("child", "Node's child")

        @JvmField
        val Disabled = of("disabled", "Node is disabled")

        @JvmField
        val Deleted = of("deleted", "Node is deleted")

        @JvmField
        val Author = of("_createdBy", "Author of the node")

        @JvmField
        val Created = of("_created", "Creation datetime of the node")

        @JvmField
        val File = of("file", "File reference of the node")

        @JvmField
        val FileHash = of("fileHash", "Hash of file content")

        @JvmField
        val FileContentType = of("fileContentType", "File content mime-type")

        /**
         * Boolean property. If true, this node can contain children with duplicate names
         *
         * By default, property is false
         */
        @JvmField
        val DuplicateNode: PropertyName = of("DuplicateNode", "Node can contain children with duplicate names")

        /**
         * Can be microservice, lib, desktop, web, etc
         */
        @JvmField
        val ModuleType = of("moduleType", "Module type")

        @JvmField
        val Infinite = of("infinite", "Node contains infinite children")

        @JvmField
        val Readonly = of("readonly", "Node properties and it's children cannot be edited")

        @JvmField
        val Transient = of("transient", "This node cannot be saved")

        /**
         * All known properties
         */
        @JvmField
        val All: Set<PropertyName> = hashSetOf(
            Id, Name, Type, Child, Disabled, Deleted, File, FileHash, Readonly, Transient,
            FileContentType, ModuleType, Author, Created, DuplicateNode, Infinite
        )

        /**
         * Creates new [PropertyName]
         */
        fun of(name: String, description: String): PropertyName = PropertyName(name, description)

        /**
         * Return system property by name if exists
         */
        fun get(name: String): PropertyName? = cache[name]

        /**
         * Return system property by name or creates new one
         */
        fun getOrCreate(name: String, description: String = ""): PropertyName = get(name) ?: of(name, description)

        private val cache: Map<String, PropertyName> = All.associateBy { it.name }
    }
}
