package io.tezrok.api.java

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.comments.JavadocComment
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.stmt.BlockStmt

/**
 * Node that represents a Java method
 */
open class JavaMethodNode(private val method: MethodDeclaration) {
    fun getName(): String = method.nameAsString

    fun setBody(body: BlockStmt): JavaMethodNode {
        method.setBody(body)
        return this
    }

    fun addParameter(typeName: String, name: String): JavaMethodNode {
        method.addParameter(typeName, name)
        return this
    }

    fun addParameter(paramClass: Class<*>, name: String): JavaMethodNode {
        method.addParameter(paramClass, name)
        return this
    }

    fun setReturnType(typeName: String): JavaMethodNode {
        method.setType(typeName)
        return this
    }

    fun setReturnType(clazz: Class<*>): JavaMethodNode {
        method.setType(clazz)
        return this
    }

    /**
     * Adds a method call expression to the method body
     */
    fun addCallExpression(name: String): JavaCallExpressionNode<MethodCallExpr> {
        val methodCallExpr = MethodCallExpr(name)
        validateBody().addStatement(methodCallExpr)
        return JavaCallExpressionNode(methodCallExpr)
    }

    fun withModifiers(vararg modifiers: Modifier.Keyword): JavaMethodNode {
        method.setModifiers(*modifiers)
        return this
    }

    fun addAnnotation(annotationClass: Class<out Annotation>): JavaMethodNode {
        method.addAnnotation(annotationClass)
        return this
    }

    fun clearBody(): JavaMethodNode {
        method.body.ifPresent { it.statements.clear() }
        return this
    }

    fun removeBody(): JavaMethodNode {
        method.removeBody()
        return this
    }

    fun setJavadocComment(comment: String) {
        method.setComment(JavadocComment(comment))
    }

    fun isPublic(): Boolean = method.isPublic

    fun isPrivate(): Boolean = method.isPrivate

    fun isProtected(): Boolean = method.isProtected

    fun isStatic(): Boolean = method.isStatic

    fun isAbstract(): Boolean = method.isAbstract

    fun isFinal(): Boolean = method.isFinal

    fun isNative(): Boolean = method.isNative

    fun isSynchronized(): Boolean = method.isSynchronized

    fun isDefault(): Boolean = method.isDefault

    fun isStrictfp(): Boolean = method.isStrictfp

    private fun validateBody(): BlockStmt {
        if (method.body.isEmpty) {
            method.setBody(BlockStmt())
        }
        return method.body.get()
    }

    override fun toString(): String {
        return "JavaMethodNode: $method"
    }
}
