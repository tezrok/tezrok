package io.tezrok.api.java

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MemberValuePair

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

    fun addAnnotation(annotationExpr: String, pairs: Map<String, Expression> = emptyMap()): JavaFieldNode {
        val annotation = field.addAndGetAnnotation(annotationExpr)
        annotation.setPairs(NodeList(pairs.map { MemberValuePair(it.key, it.value) }))
        return this
    }

    fun withModifiers(vararg modifiers: Modifier.Keyword): JavaFieldNode {
        val oldModifiers = field.modifiers.map { it.keyword } + modifiers
        field.setModifiers(*oldModifiers.distinct().toTypedArray())
        return this
    }

    /**
     * Sets new modifiers to the class
     */
    fun setModifiers(vararg modifiers: Modifier.Keyword): JavaFieldNode {
        field.setModifiers(*modifiers)
        return this
    }

    fun setInitializer(initializer: String): JavaFieldNode {
        field.getVariable(0).setInitializer(initializer)
        return this
    }
}