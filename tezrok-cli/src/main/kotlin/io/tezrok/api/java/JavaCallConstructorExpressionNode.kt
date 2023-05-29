package io.tezrok.api.java

import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt

/**
 * Represents a call of this() or super() in a constructor.
 */
open class JavaCallConstructorExpressionNode(private val invocationStmt: ExplicitConstructorInvocationStmt) {
    fun addStringArgument(argument: String): JavaCallConstructorExpressionNode {
        invocationStmt.addArgument(StringLiteralExpr(argument))
        return this
    }

    fun addNameArgument(name: String): JavaCallConstructorExpressionNode {
        invocationStmt.addArgument(NameExpr(name))
        return this
    }
}
