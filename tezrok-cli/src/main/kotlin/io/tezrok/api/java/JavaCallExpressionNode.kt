package io.tezrok.api.java

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.nodeTypes.NodeWithArguments
import com.github.javaparser.ast.stmt.Statement
import io.tezrok.util.assignToStatement

/**
 * Represents a call of a method or constructor.
 */
open class JavaCallExpressionNode<N>(private val methodCallExpr: NodeWithArguments<N>) where N : Node {
    fun asMethodCallExpr(): MethodCallExpr = methodCallExpr as MethodCallExpr

    fun addStringArgument(argument: String): JavaCallExpressionNode<N> {
        methodCallExpr.addArgument(StringLiteralExpr(argument))
        return this
    }

    fun addNameArgument(name: String): JavaCallExpressionNode<N> {
        methodCallExpr.addArgument(NameExpr(name))
        return this
    }

    fun assignToAsStatement(
        name: String,
        operator: AssignExpr.Operator = AssignExpr.Operator.ASSIGN
    ): Statement = asMethodCallExpr().assignToStatement(name, operator)

    fun assignToAsStatement(
        expression: Expression,
        operator: AssignExpr.Operator = AssignExpr.Operator.ASSIGN
    ): Statement = asMethodCallExpr().assignToStatement(expression, operator)

    companion object {
        fun ofMethodCall(expression: String): JavaCallExpressionNode<MethodCallExpr> =
            JavaCallExpressionNode(MethodCallExpr(expression))
    }
}
