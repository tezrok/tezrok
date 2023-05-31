package io.tezrok.api.java

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.nodeTypes.NodeWithArguments

/**
 * Represents a call of a method or constructor.
 */
open class JavaCallExpressionNode<N>(private val methodCallExpr: NodeWithArguments<N>) where N : Node {
    fun addStringArgument(argument: String): JavaCallExpressionNode<N> {
        methodCallExpr.addArgument(StringLiteralExpr(argument))
        return this
    }

    fun addNameArgument(name: String): JavaCallExpressionNode<N> {
        methodCallExpr.addArgument(NameExpr(name))
        return this
    }
}
