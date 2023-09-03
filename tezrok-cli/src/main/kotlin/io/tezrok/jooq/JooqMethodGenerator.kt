package io.tezrok.jooq

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import io.tezrok.api.input.EntityElem
import io.tezrok.api.java.JavaClassNode
import io.tezrok.util.asJavaType
import io.tezrok.util.camelCaseToSnakeCase

/**
 * Generates jooq methods for repository classes.
 */
internal class JooqMethodGenerator(
    private val entity: EntityElem,
    private val repoClass: JavaClassNode,
) {
    fun generateByName(methodName: String, method: MethodDeclaration) {
        check(!repoClass.hasMethod(methodName)) { "Method already exists: $methodName" }

        val returnType = method.typeAsString!!
        val newMethod = repoClass.addMethod(methodName)
            .withModifiers(Modifier.Keyword.PUBLIC)
            .setReturnType(returnType)
        // TODO: use type as fully qualified name
        val params = method.parameters.associate { it.nameAsString to it.typeAsString }
        params.forEach { param -> newMethod.addParameter(param.value, param.key) }

        if (methodName.startsWith(PREFIX_FIND_BY)) {
            newMethod.setBody(generateFindByBody(entity, methodName, returnType, params))
        } else {
            error("Unsupported method name: $methodName")
        }

        if (returnType.startsWith("List<")) {
            repoClass.addImport(List::class.java)
        }
    }

    /**
     * Generates body for List<DtoType> findBySomeFieldOrExpression() methods.
     */
    private fun generateFindByBody(
        entity: EntityElem,
        methodName: String,
        returnType: String,
        params: Map<String, String>
    ): Statement {
        val name = entity.name
        val dtoName = "${name}Dto"

        try {
            check(returnType == "List<$dtoName>") { "Unsupported return type: '$returnType', expected 'List<$dtoName>'" }

            val expressionPart = methodName.substring(PREFIX_FIND_BY.length)
            val jooqExpression = composeWhereExpression(entity, expressionPart, params)
            return ReturnStmt("dsl.selectFrom(table).where($jooqExpression).fetchInto(${dtoName}.class)")
        } catch (ex: Exception) {
            throw RuntimeException("Failed to generate body for method \"$methodName\": ${ex.message}", ex)
        }
    }

    private fun composeWhereExpression(
        entity: EntityElem,
        expressionPart: String,
        params: Map<String, String>
    ): String {
        val name = entity.name
        val tableName = name.camelCaseToSnakeCase().uppercase()
        val names = parseNameExpression(expressionPart)
        val sb = StringBuilder()
        val paramNames = params.keys.toList()

        names.forEachIndexed { index, token ->
            if (token.operator) {
                sb.append(".${token.name}(")
            } else {
                val field = entity.fields.find { fieldElem -> fieldElem.name == token.name }
                    ?: error("Field (${token.name}) not found in entity (${entity.name})")
                check(token.index < params.size) { "Parameter index of '${token.name}' out of bounds (${params.size})" }
                val paramName = paramNames[token.index]
                val paramType = params[paramName]!!

                if (field.logicField == true) {
                    // TODO: support logic fields in method name expressions
                    error("Logic field (${field.name}) cannot be used in where expression, use instead related real field")
                }

                check(field.asJavaType() == paramType) { "Field type (${field.asJavaType()}) and parameter type ($paramType) mismatch" }

                val fieldName = token.name.camelCaseToSnakeCase().uppercase()
                sb.append("Tables.${tableName}.${fieldName}.eq($paramName)")

                if (index > 0) {
                    // if param is not the first one, we should close previously opened operator
                    sb.append(")")
                }
            }
        }

        return sb.toString()
    }

    private fun parseNameExpression(expressionName: String): List<Token> {
        val parts = OPERATOR.findAll(expressionName).toList()
        if (parts.isNotEmpty()) {
            val tokens = mutableListOf<Token>()
            var indexFrom = 0

            parts.forEachIndexed { index, part ->
                val indexOfOperator = part.range.first
                val operator = part.value
                val name = expressionName.substring(indexFrom, indexOfOperator)
                tokens.add(Token(name.decapitalize(), index))
                tokens.add(Token(operator.decapitalize(), -1, true))
                indexFrom = part.range.last + 1
            }

            if (indexFrom < expressionName.length) {
                val name = expressionName.substring(indexFrom)
                tokens.add(Token(name.decapitalize(), parts.size))
            }

            return tokens
        }

        return listOf(Token(expressionName.decapitalize(), 0))
    }

    private data class Token(val name: String, val index: Int, val operator: Boolean = false)

    private companion object {
        const val PREFIX_FIND_BY = "findBy"
        val OPERATOR = Regex("(?<=[a-z])(And|Or)(?=[A-Z])")
    }
}
