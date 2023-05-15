package io.tezrok.api.java

import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.StringLiteralExpr

class JavaCallExpressionNode(private val methodCallExpr: MethodCallExpr) {
    fun addArgument(argument: String): JavaCallExpressionNode {
        methodCallExpr.addArgument(StringLiteralExpr(argument))
        return this
    }
}
