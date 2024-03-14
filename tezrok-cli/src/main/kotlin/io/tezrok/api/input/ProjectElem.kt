package io.tezrok.api.input

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.tezrok.util.NameUtil
import java.util.*

/**
 * Represents a model of a project loaded from a tezrok.json file.
 */
data class ProjectElem(
    val name: String = "",
    val version: String = "",
    val description: String = "",
    val frontend: Boolean? = null,

    @JsonProperty("package")
    val packagePath: String = "",
    val author: String = "",
    val modules: List<ModuleElem> = emptyList(),
    val git: GitElem? = null,
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
    val task: TaskElem? = null,
    val dependencies: List<String>? = null,
    // properties should be sorted by key, so we use TreeMap
    val properties: MutableMap<String, String?> = TreeMap()
) {
    override fun toString(): String {
        return "ModuleElem(name='$name')"
    }
}

data class TaskElem(val enable: Boolean?)

data class AuthElem(
    val type: String,
    // if true, then standard user and roles will be created (user admin and ADMIN, USER roles)
    val stdInit: Boolean? = null
)

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
    val syntheticTo: String? = null,
    // if true, object won't be deleted from database, but will be marked as deleted
    val activable: Boolean? = null,
    // if true, createdAt field will be added to entity
    val createdAt: Boolean? = null,
    // if true, updatedAt field will be added to entity
    val updatedAt: Boolean? = null,
    val fields: List<FieldElem>,
    val customMethods: Set<String>? = null,
    val customComments: Map<String, String>? = null,
    // initial data for entity in csv format
    val init: String? = null,
) {
    init {
        check(name.isNotBlank()) { "Entity name cannot be blank" }
        val firstChar = name.first()
        check(firstChar.isLetter()) {
            "Entity name should start with letter. Entity name: $name"
        }
        check(firstChar.isUpperCase()) {
            "Entity name should start with upper case letter. Entity name: $name"
        }
    }

    fun withCustomMethods(vararg methods: String): EntityElem {
        methods.forEach { method ->
            check(method.isNotBlank()) { "Custom method cannot be blank" }
            check(method.first().isLetterOrDigit() && method.last().isLetterOrDigit()) {
                "Custom method should start and end with letter or digit"
            }
        }

        return this.copy(customMethods = (customMethods ?: emptySet()) + methods)
    }

    fun withCustomComments(vararg comments: Pair<String, String>): EntityElem {
        val newComments = TreeMap(customComments ?: emptyMap())
        newComments.putAll(comments)
        return this.copy(customComments = newComments)
    }

    /**
     * Returns field by name or throws an exception if field is not found
     */
    fun getField(name: String): FieldElem = tryGetField(name)
        ?: error("Field ($name) not found in entity (${this.name}). Expected fields: " + fields.map { it.name })

    /**
     * Returns field by name or null if field is not found
     */
    fun tryGetField(name: String): FieldElem? = fields.find { it.name == name }

    /**
     * Returns first primary field or throws an exception if primary field is not found
     */
    @JsonIgnore
    fun getPrimaryField(): FieldElem =
        fields.find { it.primary == true } ?: error("Primary field not found in entity $name")

    /**
     * Returns all primary and synthetic fields (but not logic ones)
     */
    @JsonIgnore
    fun getIdFields(): List<FieldElem> =
        fields.filter { field -> field.primary == true || field.logicField != true && field.isSynthetic() }

    @JsonIgnore
    fun getPrimaryFieldCount(): Int = fields.count { it.primary == true }

    @JsonIgnore
    fun isSynthetic(): Boolean = syntheticTo?.isNotEmpty() == true

    @JsonIgnore
    fun isNotSynthetic(): Boolean = !isSynthetic()

    fun hasRelations(vararg relations: EntityRelation): Boolean =
        fields.any { it.isLogic() && it.hasRelations(*relations) }
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
    val relation: EntityRelation? = null,
    // used for known fields like createdAt, updatedAt
    val metaType: MetaType? = null,
    // if true, then this field is synthetic and needed by other entity (field is added by user)
    val external: Boolean? = null
) {
    init {
        check(name.isNotBlank()) { "Field name cannot be blank" }
        val firstChar = name.first()
        check(firstChar.isLetter()) {
            "Field name should start with letter. Field name: $name"
        }
        check(firstChar.isLowerCase()) {
            "Field name should start with lower case letter. Field name: $name"
        }
        if (uniqueGroup != null) {
            check(uniqueGroup.isNotBlank()) { "Field unique group cannot be blank" }
            NameUtil.validateWhitespaces(uniqueGroup)
            check(unique != true) {
                "Field $name: uniqueGroup cannot be used with unique=true"
            }
        }
    }

    @JsonIgnore
    fun isPrimaryField(): Boolean = primary == true

    @JsonIgnore
    fun isNotPrimaryField(): Boolean = !isPrimaryField()

    @JsonIgnore
    fun isSynthetic(): Boolean = syntheticTo?.isNotEmpty() == true

    @JsonIgnore
    fun isNotSynthetic(): Boolean = !isSynthetic()

    @JsonIgnore
    fun isLogic(): Boolean = logicField == true

    @JsonIgnore
    fun isNotLogic(): Boolean = !isLogic()

    @JsonIgnore
    fun hasUniqueGroup(): Boolean = uniqueGroup != null

    fun hasRelations(vararg relations: EntityRelation): Boolean = relation in relations
}

data class GitElem(
    // list of files to ignore
    val ignores: List<String>? = null,
    // list of files to exclude from std ignore list
    val excludes: List<String>? = null
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

enum class MetaType {
    CreatedAt,

    UpdatedAt,

    Email
}
