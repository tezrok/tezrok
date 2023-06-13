package io.tezrok.api.java

import com.github.javaparser.ast.body.FieldDeclaration

/**
 * Node that represents a Java field
 */
open class JavaFieldNode(private val field: FieldDeclaration) {

    fun getName(): String = field.variables.first().nameAsString

    fun addAnnotation(annotationClass: Class<out Annotation>): JavaFieldNode {
        field.addAnnotation(annotationClass)
        return this
    }

    fun addAnnotation(annotationExp: String): JavaFieldNode {
        field.addAnnotation(annotationExp)
        return this
    }
}