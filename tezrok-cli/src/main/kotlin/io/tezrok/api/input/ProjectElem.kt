package io.tezrok.api.input

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

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
    var schema: SchemaElem? = null
    var dependencies: List<String>? = null

    override fun toString(): String {
        return "ModuleElem(name='$name')"
    }
}

data class SchemaElem(
    var importSchema: String? = null,
    var entities: List<EntityElem>? = null,
    var enums: List<EnumElem>? = null
)

data class EntityElem(
        val name: String,
        val description: String? = null,
        val customRepository: Boolean? = null,
        val fields: List<FieldElem>
)

data class EnumElem(
        val name: String,
        val values: List<String>
)

data class FieldElem(
        val name: String,
        // string, integer, long, enum or reference to another entity
        val type: String? = null,
        // reference to field of another entity
        val foreignField: String? = null,
        val description: String? = null,
        val required: Boolean? = null,
        val serial: Boolean? = null,
        val primary: Boolean? = null,
        val pattern: String? = null,
        val minLength: Int? = null,
        val maxLength: Int? = null,
        val unique: Boolean? = null,
        val defValue: String? = null,
        // if true then field is not stored in database, mostly used for object fields
        val logicField: Boolean? = null,
        // true if field is synthetic and contains reference to another entity
        val syntheticTo: String? = null,
        val relation: EntityRelation? = null
) {
    /**
     * Returns true if field is eventually serial
     *
     * If serial not defined and field is primary, then it's serial
     */
    @JsonIgnore
    fun isSerialEffective() = this.serial ?: (this.primary ?: false)
}

/**
 * Relation between entities
 */
enum class EntityRelation {
    OneToOne,

    OneToMany,

    ManyToOne,

    ManyToMany,
}
