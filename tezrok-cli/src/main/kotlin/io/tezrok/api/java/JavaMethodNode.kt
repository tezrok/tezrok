package io.tezrok.api.java

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.comments.JavadocComment
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MemberValuePair
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.Type
import com.github.javaparser.ast.type.TypeParameter

/**
 * Node that represents a Java method
 */
open class JavaMethodNode(private val method: MethodDeclaration, private val parent: JavaClassNode) {
    fun getName(): String = method.nameAsString

    fun getTypeAsString(): String = method.typeAsString

    /**
     * Returns parent class or interface
     */
    fun getOwner(): JavaClassNode = parent

    fun setBody(body: BlockStmt): JavaMethodNode {
        method.setBody(body)
        return this
    }

    fun setBody(body: Statement): JavaMethodNode {
        return setBody(NodeList(body))
    }

    fun setBody(statements: NodeList<Statement>): JavaMethodNode {
        return setBody(BlockStmt(statements))
    }

    fun addParameter(typeName: String, name: String, isFinal: Boolean = true): JavaMethodNode {
        method.addAndGetParameter(typeName, name).setFinal(isFinal)
        return this
    }

    fun addParameter(paramClass: Class<*>, name: String, isFinal: Boolean = true): JavaMethodNode {
        method.addParameter(paramClass, name).setFinal(isFinal)
        return this
    }

    fun getParameters(): List<JavaMethodParameter> = method.parameters.map { JavaMethodParameter(it) }

    fun setReturnType(typeName: String): JavaMethodNode {
        method.setType(typeName)
        return this
    }

    fun setReturnType(clazz: Class<*>): JavaMethodNode {
        method.setType(clazz)
        return this
    }

    fun setReturnType(type: Type): JavaMethodNode {
        method.setType(type)
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

    /**
     *  Adds `return` statement of the last expression in the method body
     *
     *  Before:
     *  ```
     *  public int foo() {
     *      bar();
     *  }
     *  ```
     *
     *  After:
     *  ```
     *  public int foo() {
     *      return bar();
     *  }
     */
    fun addReturnToLastExpression(): JavaMethodNode {
        val statements = validateBody().statements

        if (statements.isNotEmpty()) {
            val lastStatement = statements.last()
            val returnStatement = ReturnStmt(lastStatement.asExpressionStmt().expression)
            statements.replace(lastStatement, returnStatement)
        }

        return this
    }

    fun withModifiers(vararg modifiers: Modifier.Keyword): JavaMethodNode {
        method.setModifiers(*modifiers)
        return this
    }

    fun addAnnotation(annotationClass: Class<out Annotation>): JavaMethodNode {
        method.addAnnotation(annotationClass)
        return this
    }

    fun addAnnotation(annotationExpr: String, pairs: Map<String, Expression> = emptyMap()): JavaMethodNode {
        val annotation = method.addAndGetAnnotation(annotationExpr)
        annotation.setPairs(NodeList(pairs.map { MemberValuePair(it.key, it.value) }))
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

    fun setJavadocComment(comment: String): JavaMethodNode {
        method.setComment(JavadocComment(comment))
        return this
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

    fun setTypeParameters(typeParameters: List<TypeParameter>): JavaMethodNode {
        method.setTypeParameters(NodeList(typeParameters))
        return this
    }

    fun getTypeParameters(): List<TypeParameter> = method.typeParameters

    fun setDefault(isDefault: Boolean): JavaMethodNode {
        method.setDefault(isDefault)
        return this
    }
}

