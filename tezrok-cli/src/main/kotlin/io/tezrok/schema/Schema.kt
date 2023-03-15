package io.tezrok.schema

import com.fasterxml.jackson.annotation.JsonProperty

data class Schema(
    val id: String?,
    @JsonProperty("\$schema")
    val schema: String?,
    val title: String?,
    val description: String?,
    val type: String,
    val properties: Map<String, Definition>?,
    val definitions: Map<String, Definition>?,
    val required: List<String>? = null
)

data class Definition(
    val type: String,
    val title: String?,
    val description: String?,
    val pattern: String?,
    val additionalProperties: Boolean?,
    val properties: Map<String, Definition>? = null,
    val required: List<String>? = null
)
