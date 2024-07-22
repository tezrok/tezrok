package io.tezrok.api.java

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.comments.JavadocComment
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.Type
import com.github.javaparser.ast.type.TypeParameter
import io.tezrok.util.addImportsByType
import org.springframework.web.bind.annotation.RequestMethod

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
        parent.addImportsByType(typeName)
        method.addAndGetParameter(typeName, name).setFinal(isFinal)
        return this
    }

    fun addParameter(paramClass: Class<*>, name: String, isFinal: Boolean = true): JavaMethodNode {
        method.addAndGetParameter(paramClass, name).setFinal(isFinal)
        return this
    }

    fun getParameters(): List<JavaMethodParameter> = method.parameters.map { JavaMethodParameter(it, this) }

    fun getLastParameter(): JavaMethodParameter {
        return method.parameters.lastOrNull()?.let { JavaMethodParameter(it, this) } ?: error("Method has no parameters")
    }

    fun setReturnType(typeName: String): JavaMethodNode {
        parent.addImportsByType(typeName)
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

    fun addAnnotation(annotationClass: Class<out Annotation>, vararg fields: Pair<String, Expression>): JavaMethodNode {
        return addAnnotation(annotationClass, fields.toMap())
    }

    fun addAnnotation(annotationClass: Class<out Annotation>, fields: Map<String, Expression>): JavaMethodNode {
        if (fields.isNotEmpty()) {
            parent.addImport(annotationClass)
            val annotation = method.addAndGetAnnotation(annotationClass.simpleName)
            annotation.setPairs(NodeList(fields.map { MemberValuePair(it.key, it.value) }))
        } else {
            addAnnotation(annotationClass)
        }
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

    fun addAnnotation(annotationExpr: NormalAnnotationExpr): JavaMethodNode {
        method.addAnnotation(annotationExpr)
        return this
    }

    fun addAnnotation(annotationClass: Class<out Annotation>, expression: Expression): JavaMethodNode {
        parent.addImport(annotationClass)
        method.addAnnotation(SingleMemberAnnotationExpr(Name(annotationClass.simpleName), expression))
        return this
    }

    fun addAnnotation(annotationClass: Class<out Annotation>, expression: String): JavaMethodNode {
        return addAnnotation(annotationClass, StringLiteralExpr(expression))
    }

    /**
     * Adds an annotation with an array initializer
     *
     * Example:  @Secured({ "ROLE_USER", "ROLE_ADMIN" })
     */
    fun addAnnotation(annotationClass: Class<out Annotation>, arrayInit: List<Expression>): JavaMethodNode {
        val arrayExpr = ArrayInitializerExpr()
        arrayExpr.values = NodeList(arrayInit)
        return addAnnotation(annotationClass, arrayExpr)
    }

    fun addImport(importClass: Class<*>) {
        parent.addImport(importClass)
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

    fun setStatic(isStatic: Boolean): JavaMethodNode {
        method.setStatic(isStatic)
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

