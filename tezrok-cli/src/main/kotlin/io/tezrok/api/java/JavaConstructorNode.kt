package io.tezrok.api.java

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.comments.JavadocComment
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt

open class JavaConstructorNode(private val constructor: ConstructorDeclaration) {
    fun setBody(body: BlockStmt): JavaConstructorNode {
        constructor.setBody(body)
        return this
    }

    fun addParameter(typeName: String, name: String): JavaConstructorNode {
        constructor.addParameter(typeName, name)
        return this
    }

    fun addParameter(paramClass: Class<*>, name: String): JavaConstructorNode {
        constructor.addParameter(paramClass, name)
        return this
    }

    /**
     * Adds new modifiers to the constructor
     */
    fun withModifiers(vararg modifiers: Modifier.Keyword): JavaConstructorNode {
        val oldModifiers = constructor.modifiers.map { it.keyword } + modifiers
        constructor.setModifiers(*oldModifiers.distinct().toTypedArray())
        return this
    }

    /**
     * Sets new modifiers to the constructor
     */
    fun setModifiers(vararg modifiers: Modifier.Keyword): JavaConstructorNode {
        constructor.setModifiers(*modifiers)
        return this
    }

    fun addAnnotation(annotationClass: Class<out Annotation>): JavaConstructorNode {
        constructor.addAnnotation(annotationClass)
        return this
    }

    fun addCallSuperExpression(): JavaCallExpressionNode<ExplicitConstructorInvocationStmt> {
        return getCallInternal(false)
    }

    fun addCallThisExpression(): JavaCallExpressionNode<ExplicitConstructorInvocationStmt> {
        return getCallInternal(true)
    }

    fun clearBody() {
        constructor.body.statements.clear()
    }

    fun setJavadocComment(comment: String) {
        constructor.setComment(JavadocComment(comment))
    }

    private fun getCallInternal(isThis: Boolean): JavaCallExpressionNode<ExplicitConstructorInvocationStmt> {
        val invocationStmt = ExplicitConstructorInvocationStmt()
        invocationStmt.setThis(isThis)
        constructor.body.addStatement(invocationStmt)
        return JavaCallExpressionNode(invocationStmt)
    }
}
