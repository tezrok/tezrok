package io.tezrok.util

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.Statement

/**
 * Get main class of the compilation unit
 */
fun CompilationUnit.getRootClass(): ClassOrInterfaceDeclaration =
    this.types.filterIsInstance<ClassOrInterfaceDeclaration>().firstOrNull()
        ?: throw IllegalStateException("Class not found in compilation unit")


fun Expression.assignStatement(value: Expression, operator: AssignExpr.Operator): Statement =
    ExpressionStmt(
        AssignExpr(
            this,
            value,
            operator
        )
    )

fun Expression.assignToStatement(variable: Expression, operator: AssignExpr.Operator): Statement =
    variable.assignStatement(this, operator)

fun Expression.assignToStatement(name: String, operator: AssignExpr.Operator): Statement =
    NameExpr(name).assignStatement(this, operator)

object JavaParserFactory {
    fun create(): JavaParser {
        val config = ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
        return JavaParser(config)
    }
}
