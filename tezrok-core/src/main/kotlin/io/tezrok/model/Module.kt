package io.tezrok.model

class Module(name: String,
             description: String? = null,
             val entities: List<Entity>? = null) : BaseEntity(name, description, "Moudle")
