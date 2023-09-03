package io.tezrok.jooq

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import io.tezrok.api.input.EntityElem
import io.tezrok.api.input.FieldElem
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
            val (where, orderBy) = parseAsJooqExpression(entity, expressionPart, params)
            return ReturnStmt("dsl.selectFrom(table).where($where)$orderBy.fetchInto(${dtoName}.class)")
        } catch (ex: Exception) {
            throw RuntimeException("Failed to generate body for method \"$methodName\": ${ex.message}", ex)
        }
    }

    private fun parseAsJooqExpression(
        entity: EntityElem,
        expressionPart: String,
        params: Map<String, String>
    ): JooqExpression {
        val name = entity.name
        val tableName = name.camelCaseToSnakeCase().uppercase()
        val names = parseNameExpression(expressionPart)
        val sb = StringBuilder()
        val paramNames = params.keys.toList()
        var orderBy = ""

        names.forEachIndexed { index, token ->
            when (token) {
                is Operator -> sb.append(".${token.name}(")
                is FieldName -> {
                    check(token.index < params.size) { "Parameter index of '${token.name}' out of bounds (${params.size})" }
                    val field = getFieldByName(entity, token.name)
                    val paramName = paramNames[token.index]
                    val paramType = params[paramName]!!

                    check(field.asJavaType() == paramType) { "Field type (${field.asJavaType()}) and parameter type ($paramType) mismatch" }

                    val fieldName = token.name.camelCaseToSnakeCase().uppercase()
                    sb.append("Tables.${tableName}.${fieldName}.eq($paramName)")

                    if (index > 0) {
                        // if param is not the first one, we should close previously opened operator
                        sb.append(")")
                    }
                }

                is OrderBy -> orderBy = ".orderBy("
                is SortName -> {
                    val field = getFieldByName(entity, token.name)
                    val fieldName = field.name.camelCaseToSnakeCase().uppercase()
                    orderBy += "Tables.${tableName}.${fieldName}"

                    if (token.sort == Sort.Asc) {
                        orderBy += ".asc()"
                    } else if (token.sort == Sort.Desc) {
                        orderBy += ".desc()"
                    }
                    orderBy += ")"
                }
            }
        }

        return JooqExpression(where = sb.toString(), orderBy = orderBy)
    }


    private fun getFieldByName(entity: EntityElem, name: String): FieldElem {
        val field = entity.fields.find { fieldElem -> fieldElem.name == name }
            ?: error("Field ($name) not found in entity (${entity.name})")
        if (field.logicField == true) {
            // TODO: support logic fields in method name expressions
            error("Logic field (${field.name}) cannot be used in method expression, use instead related real field")
        }

        return field
    }

    private fun parseNameExpression(expressionName: String): List<Token> {
        val parts = OPERATOR.findAll(expressionName).toList()
        if (parts.isNotEmpty()) {
            val tokens = mutableListOf<Token>()
            var indexFrom = 0
            var lastOperatorIsOrderBy = false

            parts.forEachIndexed { index, part ->
                val indexOfOperator = part.range.first
                val operator = part.value
                val name = expressionName.substring(indexFrom, indexOfOperator)
                tokens.add(FieldName(name.decapitalize(), index))
                check(!lastOperatorIsOrderBy) { "Only one OrderBy operator is allowed" }
                lastOperatorIsOrderBy = operator == OrderBy.name
                if (lastOperatorIsOrderBy) {
                    tokens.add(OrderBy)
                } else {
                    tokens.add(Operator(operator.decapitalize()))
                }
                indexFrom = part.range.last + 1
            }

            if (indexFrom < expressionName.length) {
                val name = expressionName.substring(indexFrom)

                if (lastOperatorIsOrderBy) {
                    if (name.endsWith(SORT_ASC)) {
                        tokens.add(SortName(name.removeSuffix(SORT_ASC).decapitalize(), Sort.Asc))
                    } else if (name.endsWith(SORT_DESC)) {
                        tokens.add(SortName(name.removeSuffix(SORT_DESC).decapitalize(), Sort.Desc))
                    } else {
                        tokens.add(SortName(name.decapitalize()))
                    }
                } else {
                    tokens.add(FieldName(name.decapitalize(), parts.size))
                }
            }

            return tokens
        }

        return listOf(FieldName(expressionName.decapitalize(), 0))
    }

    private data class JooqExpression(val where: String, val orderBy: String)

    private abstract class Token(val name: String)

    private class Operator(name: String) : Token(name)

    private object OrderBy : Token("OrderBy")

    private class FieldName(name: String, val index: Int) : Token(name)

    private class SortName(name: String, val sort: Sort = Sort.Default) : Token(name)

    enum class Sort {
        Default,
        Asc,
        Desc
    }

    private companion object {
        const val PREFIX_FIND_BY = "findBy"
        const val SORT_ASC = "Asc"
        const val SORT_DESC = "Desc"
        val OPERATOR = Regex("(?<=[a-z])(And|Or|OrderBy)(?=[A-Z])")
    }
}
