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
import java.util.*

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

        if (methodName.startsWith(PREFIX_FIND_BY) || methodName.startsWith(PREFIX_FIND)) {
            newMethod.setBody(generateFindByBody(entity, methodName, returnType, params))
        } else if (methodName.startsWith(PREFIX_GET_BY) || methodName.startsWith(PREFIX_GET)) {
            newMethod.setBody(generateGetByBody(entity, methodName, returnType, params))
        } else {
            error("Unsupported method name: $methodName")
        }

        if (returnType.startsWith("List<")) {
            repoClass.addImport(List::class.java)
        }
        if (returnType.startsWith("Optional<")) {
            repoClass.addImport(Optional::class.java)
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

            val expressionPart = if (methodName.startsWith(PREFIX_FIND_BY))
                methodName.substring(PREFIX_FIND_BY.length)
            else
                methodName.substring(PREFIX_FIND.length)
            val (where, orderBy, limit) = parseAsJooqExpression(entity, expressionPart, params, false)
            return ReturnStmt("dsl.selectFrom(table).where($where)$orderBy$limit.fetchInto(${dtoName}.class)")
        } catch (ex: Exception) {
            throw RuntimeException("Failed to generate body for method \"$methodName\": ${ex.message}", ex)
        }
    }

    /**
     * Generates body for Optional<DtoType> getBySomeFieldOrExpression() methods.
     */
    private fun generateGetByBody(
        entity: EntityElem,
        methodName: String,
        returnType: String,
        params: Map<String, String>
    ): Statement {
        val name = entity.name
        val dtoName = "${name}Dto"

        try {
            val isOptionalReturn = returnType == "Optional<$dtoName>"
            check(returnType == dtoName || isOptionalReturn) { "Unsupported return type: '$returnType', expected '$dtoName' or 'Optional<$dtoName>'" }

            val expressionPart = if (methodName.startsWith(PREFIX_GET_BY))
                methodName.substring(PREFIX_GET_BY.length)
            else
                methodName.substring(PREFIX_GET.length)
            val (where, orderBy, limit) = parseAsJooqExpression(entity, expressionPart, params, true)
            val finalMethod = if (isOptionalReturn) "fetchOptionalInto" else "fetchOneInto"
            return ReturnStmt("dsl.selectFrom(table).where($where)$orderBy$limit.$finalMethod(${dtoName}.class)")
        } catch (ex: Exception) {
            throw RuntimeException("Failed to generate body for method \"$methodName\": ${ex.message}", ex)
        }
    }

    private fun parseAsJooqExpression(
        entity: EntityElem,
        expressionPart: String,
        params: Map<String, String>,
        singleResult: Boolean
    ): JooqExpression {
        val name = entity.name
        val tableName = name.camelCaseToSnakeCase().uppercase()
        val names = MethodExpressionParser.parse(expressionPart)
        val sb = StringBuilder()
        val paramNames = params.keys.toList()
        var orderBy = ""
        var limit = ""
        var paramIndex = -1
        var namesCount = 0

        names.forEachIndexed { index, token ->
            when (token) {
                is MethodExpressionParser.And,
                is MethodExpressionParser.Or -> {
                    val operator = token.name.lowercase()
                    sb.append(".$operator(")
                }

                is MethodExpressionParser.Name -> {
                    val field = getFieldByName(entity, token.name)
                    val fieldName = token.name.camelCaseToSnakeCase().uppercase()
                    val nextOp = if (index + 1 < names.size) names[index + 1] else null
                    val isOp = nextOp is MethodExpressionParser.Is || nextOp is MethodExpressionParser.IsNot
                    namesCount++

                    if (isOp && index + 2 < names.size && names[index + 2] is MethodExpressionParser.Null) {
                        sb.append("Tables.${tableName}.${fieldName}")
                        if (nextOp is MethodExpressionParser.Is) {
                            sb.append(".isNull()")
                        } else {
                            sb.append(".isNotNull()")
                        }
                    } else {
                        paramIndex++
                        check(paramIndex < params.size) { "Parameter index of '${token.name}' out of bounds (${params.size})" }
                        val paramName = paramNames[paramIndex]
                        val paramType = params[paramName] ?: error("Parameter type not found: $paramName")

                        check(field.asJavaType() == paramType) { "Field type (${field.asJavaType()}) and parameter type ($paramType) mismatch" }

                        sb.append("Tables.${tableName}.${fieldName}.eq($paramName)")
                        if (nextOp is MethodExpressionParser.IsNot) {
                            sb.append(".not()")
                        }
                    }
                    if (namesCount > 1) {
                        // if param is not the first one, we should close previously opened operator
                        sb.append(")")
                    }
                }

                is MethodExpressionParser.OrderBy -> orderBy = ".orderBy("
                is MethodExpressionParser.SortName -> {
                    // TODO: support several fields in order by
                    val field = getFieldByName(entity, token.name)
                    val fieldName = field.name.camelCaseToSnakeCase().uppercase()
                    orderBy += "Tables.${tableName}.${fieldName}"

                    if (token.sort == MethodExpressionParser.Sort.Asc) {
                        orderBy += ".asc()"
                    } else if (token.sort == MethodExpressionParser.Sort.Desc) {
                        orderBy += ".desc()"
                    }
                    orderBy += ")"
                }

                is MethodExpressionParser.Top -> {
                    check(!singleResult || token.limit == 1) { "Top limit should be 1 for single result method" }
                    limit = ".limit(${token.limit})"
                }
            }
        }

        paramIndex++
        if (paramIndex < params.size) {
            val notUsedParams = paramNames.subList(paramIndex, params.size).joinToString(", ")
            error("Not all parameters used in method: $notUsedParams")
        }

        if (singleResult && orderBy.isNotEmpty() && limit.isEmpty()) {
            error("Single result method with OrderBy should use top 1")
        }

        return JooqExpression(where = sb.toString(), orderBy = orderBy, limit = limit)
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

    private data class JooqExpression(val where: String, val orderBy: String, val limit: String)


    private companion object {
        const val PREFIX_FIND_BY = "findBy"
        const val PREFIX_FIND = "find"
        const val PREFIX_GET_BY = "getBy"
        const val PREFIX_GET = "get"
    }
}
