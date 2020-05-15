package io.tezrok.model

class Entity(name: String,
             description: String? = null,
             val fields: List<Field>? = null) : BaseEntity(name, description, "Entity")