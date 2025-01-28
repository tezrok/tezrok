package io.tezrok.util

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.Statement

/**
 * Get main class of the compilation unit
 */
fun CompilationUnit.getRootClass(): ClassOrInterfaceDeclaration =
    this.types.filterIsInstance<ClassOrInterfaceDeclaration>().firstOrNull()
        ?: throw IllegalStateException("Class not found in compilation unit: $this")


fun Expression.assignStatement(value: Expression, operator: AssignExpr.Operator): Statement =
    ExpressionStmt(
        AssignExpr(
            this,
            value,
            operator
        )
    )

fun Expression.assignToAsStatement(variable: Expression, operator: AssignExpr.Operator = AssignExpr.Operator.ASSIGN): Statement =
    variable.assignStatement(this, operator)

fun Expression.assignToAsStatement(name: String, operator: AssignExpr.Operator = AssignExpr.Operator.ASSIGN): Statement =
    NameExpr(name).assignStatement(this, operator)

fun Expression.asStatement(): Statement = ExpressionStmt(this)

fun Statement.asBlock(): BlockStmt = BlockStmt(NodeList(this))

fun Statement.withLineComment(comment: String): Statement = this.apply { setLineComment(comment) }

fun MethodDeclaration.nameWithParams(): String =
    "${this.nameAsString}${this.parameters.joinToString(", ", "(", ")") { it.typeAsString }}"

fun String.asNameExpr(): NameExpr = NameExpr(this)

fun String.asNameExprList(): NodeList<Expression> = NodeList(NameExpr(this))

fun String.asSimpleName(): SimpleName = SimpleName(this)

fun String.parseAsStatement(): Statement {
    val parser = JavaParserFactory.create()
    val result = parser.parseStatement(this)

    if (result.isSuccessful) {
        return result.result.get()
    } else {
        val problems = result.problems.map { it.message }.joinToString("\n\n") { it.toString() }
        error("Failed to parse statement: $this\n$problems")
    }
}

fun String.parseAsStatements(): List<Statement> {
    val isBlock = this.startsWith("{")
    val fixedCode = if (isBlock) this else "{$this}"
    val block = fixedCode.parseAsBlock()
    return if (isBlock) listOf(block) else block.statements
}

fun String.parseAsBlock(): BlockStmt {
    val parser = JavaParserFactory.create()
    val result = parser.parseBlock(if (this.startsWith("{")) this else "{$this}")

    if (result.isSuccessful) {
        return result.result.get()
    } else {
        val problems = result.problems.map { it.message }.joinToString("\n\n") { it.toString() }
        error("Failed to parse block: $this\n$problems")
    }
}

object JavaParserFactory {
    fun create(): JavaParser {
        val config = ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
        return JavaParser(config)
    }
}
