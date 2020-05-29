package io.tezrok.api.service

import io.tezrok.api.builder.JMod
import io.tezrok.api.builder.JavaClassBuilder
import io.tezrok.api.builder.JavaEnumBuilder
import io.tezrok.api.builder.type.Type

interface CodeService : Service {
    fun createClass(type: Type, mod: Int): JavaClassBuilder

    fun createClass(type: Type): JavaClassBuilder {
        return createClass(type, JMod.EMPTY)
    }

    fun createEnum(type: Type): JavaEnumBuilder
}
