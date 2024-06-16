package io.tezrok.api.java

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.expr.*

/**
 * Node that represents a Java field
 */
open class JavaFieldNode(private val field: FieldDeclaration) {

    fun getName(): String = field.variables.first().nameAsString

    fun getType(): String = field.commonType.toString()

    fun addAnnotation(annotationClass: Class<out Annotation>, vararg fields: Pair<String, Expression>): JavaFieldNode {
        return addAnnotation(annotationClass, fields.toMap())
    }

    fun addAnnotation(annotationClass: Class<out Annotation>, fields: Map<String, Expression>): JavaFieldNode {
        if (fields.isNotEmpty()) {
            field.tryAddImportToParentCompilationUnit(annotationClass)
            val annotation = field.addAndGetAnnotation(annotationClass.simpleName)
            annotation.setPairs(NodeList(fields.map { MemberValuePair(it.key, it.value) }))
        } else {
            field.addAnnotation(annotationClass)
        }
        return this
    }

    fun addAnnotation(annotationClass: Class<out Annotation>, expression: Expression): JavaFieldNode {
        field.tryAddImportToParentCompilationUnit(annotationClass)
        field.addAnnotation(SingleMemberAnnotationExpr(Name(annotationClass.simpleName), expression))
        return this
    }

    fun addAnnotation(annotationClass: Class<out Annotation>, expression: String): JavaFieldNode {
        return addAnnotation(annotationClass, StringLiteralExpr(expression))
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