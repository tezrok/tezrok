package io.tezrok.api.input

import com.fasterxml.jackson.annotation.JsonProperty
import io.tezrok.schema.Schema

/**
 * Represents a model of a project loaded from a tezrok.json file
 */
open class ProjectElem {
    var name: String = ""
    var version: String = ""
    var description: String = ""

    @JsonProperty("package")
    var packagePath: String = ""
    var author: String = ""
    var modules: List<ModuleElem> = emptyList()

    override fun toString(): String {
        return "ProjectElem(name='$name')"
    }
}

open class ModuleElem {
    var name: String = ""
    var description: String = ""
    var type: String = "" // TODO: enum
    var importSchema: String = ""

    // TODO: remove schema property and use entities instead
    open var schema: Schema? = null

    // TODO: convert schema to entities
    var entities: List<EntityElem> = emptyList()
    var enums: List<EnumElem> = emptyList()

    override fun toString(): String {
        return "ModuleElem(name='$name')"
    }
}

data class EntityElem(
        val name: String,
        val fields: List<FieldElem>
)

data class EnumElem(
        val name: String,
        val values: List<String>
)

data class FieldElem(
        val name: String,
        // string, integer, long, enum or reference to another entity
        val type: String,
        val description: String? = null,
        val required: Boolean = false,
        val pattern: String? = null,
        val minLength: Int? = null,
        val maxLength: Int? = null,
        val defValue: String? = null,
        val relation: EntityRelation? = null
)

enum class EntityRelation {
    OneToOne,

    OneToMany,

    ManyToOne,

    ManyToMany,
}
