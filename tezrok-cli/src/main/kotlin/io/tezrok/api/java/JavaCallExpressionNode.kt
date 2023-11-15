package io.tezrok.api.java

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.nodeTypes.NodeWithArguments
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.Statement
import io.tezrok.util.asBlock
import io.tezrok.util.asStatement
import io.tezrok.util.assignToAsStatement

/**
 * Represents a call of a method or constructor.
 */
open class JavaCallExpressionNode<N>(private val callExpression: NodeWithArguments<N>) where N : Node {
    fun asMethodCallExpr(): MethodCallExpr = callExpression as MethodCallExpr

    fun asStatement(): Statement = (callExpression as Expression).asStatement()

    fun asBlock(): BlockStmt = asStatement().asBlock()

    fun addStringArgument(argument: String): JavaCallExpressionNode<N> {
        callExpression.addArgument(StringLiteralExpr(argument))
        return this
    }

    fun addNameArgument(name: String): JavaCallExpressionNode<N> {
        return addNameArgument(NameExpr(name))
    }
    fun addNameArgument(name: NameExpr): JavaCallExpressionNode<N> {
        callExpression.addArgument(name)
        return this
    }

    fun assignToAsStatement(
        name: String,
        operator: AssignExpr.Operator = AssignExpr.Operator.ASSIGN
    ): Statement = asMethodCallExpr().assignToAsStatement(name, operator)

    fun assignToAsStatement(
        expression: Expression,
        operator: AssignExpr.Operator = AssignExpr.Operator.ASSIGN
    ): Statement = asMethodCallExpr().assignToAsStatement(expression, operator)

    companion object {
        fun ofMethodCall(expression: String): JavaCallExpressionNode<MethodCallExpr> =
            JavaCallExpressionNode(MethodCallExpr(expression))
    }
}
