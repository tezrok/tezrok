package io.tezrok.api.java

import com.github.javaparser.ast.body.Parameter

open class JavaMethodParameter(private val parameter: Parameter) {
    fun getName(): String = parameter.nameAsString

    fun getTypeAsString(): String = parameter.typeAsString
}
