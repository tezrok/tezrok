package io.tezrok.api.schema

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents a JSON schema
 */
class Schema(
    val id: String?,
    @JsonProperty("\$schema")
    val schema: String?,
    val definitions: Map<String, Definition>?,
) : Definition()

/**
 * Represents a JSON schema definition
 */
open class Definition(
    val type: String? = null,
    val primary: Boolean? = null,
    val serial: Boolean? = null,
    val format: String? = null,
    val title: String? = null,
    val description: String? = null,
    val pattern: String? = null,
    val additionalProperties: Boolean? = null,
    val properties: Map<String, Definition>? = null,
    val required: List<String>? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val items: Definition? = null,
    val enum: List<String>? = null,
    @JsonProperty("\$ref")
    val ref: String? = null,
    val relation: EntityRelation? = null,
) {
    @JsonIgnore
    fun isArray(): Boolean = type == "array"
}

enum class EntityRelation {
    OneToOne,

    OneToMany,

    ManyToOne,

    ManyToMany,
}

