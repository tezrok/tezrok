package io.tezrok.core.service

import io.tezrok.api.ExecuteContext
import io.tezrok.api.builder.JavaClassBuilder
import io.tezrok.api.builder.JavaEnumBuilder
import io.tezrok.api.builder.type.Type
import io.tezrok.api.service.CodeService
import io.tezrok.core.util.VelocityUtil
import org.apache.velocity.Template

class CodeServiceImpl(private val context: ExecuteContext) : CodeService {
    override fun createClass(type: Type, mod: Int): JavaClassBuilder {
        return JavaClassBuilderImpl(type, mod, context)
    }

    override fun createEnum(type: Type): JavaEnumBuilder {
        return JavaEnumBuilderImpl(type, context)
    }
}


class JavaClassBuilderImpl(type: Type, mod: Int, context: ExecuteContext)
    : JavaClassBuilder(type, mod, context) {
    override fun getTemplate(): Template = JavaClassBuilderImpl.template

    companion object {
        val template: Template = VelocityUtil.getTemplate("templates/javaClass.vm")
    }
}

class JavaEnumBuilderImpl(type: Type, context: ExecuteContext) : JavaEnumBuilder(type, context) {
    override fun getTemplate(): Template = JavaEnumBuilderImpl.template

    companion object {
        val template: Template = VelocityUtil.getTemplate("templates/javaEnum.vm")
    }
}
