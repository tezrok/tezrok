package io.tezrok.api.java

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.FieldDeclaration

/**
 * Node that represents a Java field
 */
open class JavaFieldNode(private val field: FieldDeclaration) {

    fun getName(): String = field.variables.first().nameAsString

    fun getType(): String = field.commonType.toString()

    fun addAnnotation(annotationClass: Class<out Annotation>): JavaFieldNode {
        field.addAnnotation(annotationClass)
        return this
    }

    fun addAnnotation(annotationExp: String): JavaFieldNode {
        field.addAnnotation(annotationExp)
        return this
    }

    fun withModifiers(vararg modifiers: Modifier.Keyword): JavaFieldNode {
        val oldModifiers = field.modifiers.map { it.keyword } + modifiers
        field.setModifiers(*oldModifiers.distinct().toTypedArray())
        return this
    }
}