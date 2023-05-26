package io.tezrok.api.java

import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.StringLiteralExpr

class JavaCallExpressionNode(private val methodCallExpr: MethodCallExpr) {
    fun addStringArgument(argument: String): JavaCallExpressionNode {
        methodCallExpr.addArgument(StringLiteralExpr(argument))
        return this
    }

    fun addNameArgument(name: String): JavaCallExpressionNode {
        methodCallExpr.addArgument(NameExpr(name))
        return this
    }
}
