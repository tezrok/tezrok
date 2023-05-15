package io.tezrok.api.java

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.stmt.BlockStmt

/**
 * Node that represents a Java method
 */
open class JavaMethodNode(private val method: MethodDeclaration) {
    fun setBody(body: BlockStmt) {
        method.setBody(body)
    }

    fun addParameter(typeName: String, name: String): JavaMethodNode {
        method.addParameter(typeName, name)
        return this
    }

    /**
     * Adds a method call expression to the method body
     */
    fun addCallExpression(name: String): JavaCallExpressionNode {
        val methodCallExpr = MethodCallExpr(name)
        validateBody().addStatement(methodCallExpr)
        return JavaCallExpressionNode(methodCallExpr)
    }

    private fun validateBody(): BlockStmt {
        if (method.body.isEmpty) {
            method.setBody(BlockStmt())
        }
        return method.body.get()
    }
}
