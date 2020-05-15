package io.tezrok.model

class Field(name: String,
            val type: String,
            description: String? = null,
            val primary: Boolean? = null,
            val max: Int? = null,
            val min: Int? = null,
            val nullable: Boolean? = null,
            val lazy: Boolean? = null) : BaseEntity(name, description, "Field")
