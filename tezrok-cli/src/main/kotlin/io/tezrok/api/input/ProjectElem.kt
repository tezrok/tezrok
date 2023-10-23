package io.tezrok.api.input

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

/**
 * Represents a model of a project loaded from a tezrok.json file.
 */
data class ProjectElem(
    val name: String = "",
    val version: String = "",
    val description: String = "",

    @JsonProperty("package")
    val packagePath: String = "",
    val author: String = "",
    val modules: List<ModuleElem> = emptyList()
) {
    override fun toString(): String {
        return "ProjectElem(name='$name')"
    }
}

data class ModuleElem(
    val name: String = "",
    val description: String = "",
    val type: String = "", // TODO: enum
    val schema: SchemaElem? = null,
    val auth: AuthElem? = null,
    val dependencies: List<String>? = null,
    // properties should be sorted by key, so we use TreeMap
    val properties: MutableMap<String, String?> = TreeMap()
) {
    override fun toString(): String {
        return "ModuleElem(name='$name')"
    }
}

data class AuthElem(val type: String)

data class SchemaElem(
    val schemaName: String = "public", // by default schema name is public
    val importSchema: String? = null,
    val entities: List<EntityElem>? = null,
    val enums: List<EnumElem>? = null
)

data class EntityElem(
    val name: String,
    val description: String? = null,
    val customRepository: Boolean? = null,
    val customMethods: Set<String>? = null,
    val syntheticTo: String? = null,
    // if true, object won't be deleted from database, but will be marked as deleted
    val activable: Boolean? = null,
    val fields: List<FieldElem>
) {
    fun withCustomMethods(vararg methods: String): EntityElem {
        methods.forEach { method ->
            check(method.isNotBlank()) { "Custom method cannot be blank" }
            check(method.first().isLetterOrDigit() && method.last().isLetterOrDigit()) {
                "Custom method should start and end with letter or digit"
            }
        }

        return this.copy(customMethods = (customMethods ?: emptySet()) + methods)
    }

    /**
     * Returns field by name or throws an exception if field is not found
     */
    fun getField(name: String): FieldElem = fields.find { it.name == name }
        ?: error("Field ($name) not found in entity (${this.name}). Expected fields: " + fields.map { it.name })
}

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
    val uniqueGroup: String? = null,
    val defValue: String? = null,
    // if true then field is not stored in database, mostly used for object fields
    val logicField: Boolean? = null,
    // true if field is synthetic and contains reference to another entity
    val syntheticTo: String? = null,
    val relation: EntityRelation? = null
)

/**
 * Relation between entities
 */
enum class EntityRelation {
    OneToOne,

    OneToMany,

    ManyToOne,

    ManyToMany,
}
