package io.tezrok.jooq

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import io.tezrok.api.input.EntityElem
import io.tezrok.api.input.FieldElem
import io.tezrok.api.java.JavaClassNode
import io.tezrok.util.addImportsByType
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
        if (method.typeParameters.isNonEmpty) {
            newMethod.setTypeParameters(method.typeParameters)
        }
        // TODO: use type as fully qualified name
        val params = method.parameters.associate { it.nameAsString to it.typeAsString }
        params.forEach { param -> newMethod.addParameter(param.value, param.key) }

        if (methodName.startsWith(PREFIX_FIND)) {
            newMethod.setBody(generateFindByBody(entity, methodName, returnType, params))
        } else if (methodName.startsWith(PREFIX_GET)) {
            newMethod.setBody(generateGetByBody(entity, methodName, returnType, params))
        } else if (methodName.startsWith(PREFIX_COUNT)) {
            newMethod.setBody(generateCountByBody(entity, methodName, returnType, params))
        } else {
            error("Unsupported method name: $methodName")
        }

        repoClass.addImportsByType(returnType)
        params.values.toSet().forEach { type -> repoClass.addImportsByType(type) }
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
        try {
            val dtoName = "${entity.name}Dto"
            val recordName = "${entity.name}Record"
            val dtoList = "List<$dtoName>"
            val recordList = "List<$recordName>"
            val dtoPage = "Page<$dtoName>"
            val recordPage = "Page<$recordName>"
            val genericList = "List<T>"
            val supportedTypes = setOf(dtoList, recordList, dtoPage, recordPage, genericList)
            check(supportedTypes.contains(returnType)) { "Unsupported return type: '$returnType', expected: $supportedTypes" }

            val pageableRequest = returnType == dtoPage || returnType == recordPage
            val (params, lastParam) = when(returnType) {
                dtoPage, recordPage -> processParamsWithLastParam(params, "Pageable")
                genericList -> processParamsWithLastParam(params, "Class<T>")
                else -> params to ""
            }
            val expressionPart = removeFindByPrefix(methodName, "${entity.name}s")
            val (where, orderBy, limit, distinct) = parseAsJooqExpression(entity, expressionPart, params, false)

            check(limit.isEmpty() || !pageableRequest) { "Top and Pageable cannot be used together" }
            check(orderBy.isEmpty() || !pageableRequest) { "OrderBy and Pageable cannot be used together" }
            check(!distinct) { "Distinct cannot be used whole table select" }

            return when (returnType) {
                dtoList -> ReturnStmt("dsl.selectFrom(table).where($where)$orderBy$limit.fetchInto(${dtoName}.class)")
                recordList -> ReturnStmt("dsl.selectFrom(table).where($where)$orderBy$limit.fetch()")
                dtoPage -> ReturnStmt("findPage($where, $lastParam, ${dtoName}.class)")
                recordPage -> ReturnStmt("findPage($where, $lastParam, $recordName.class)")
                genericList -> ReturnStmt("dsl.selectFrom(table).where($where)$orderBy$limit.fetchInto($lastParam)")
                else -> error("Unsupported return type: $returnType")
            }
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
        try {
            val dtoName = "${entity.name}Dto"
            val optionalDto = "Optional<$dtoName>"
            val recordResult = "${entity.name}Record"
            val optionalRecord = "Optional<$recordResult>"
            val genericDto = "T"
            val supportedTypes = setOf(dtoName, optionalDto, recordResult, optionalRecord, genericDto)
            check(supportedTypes.contains(returnType)) { "Unsupported return type: '$returnType', expected: $supportedTypes" }

            val (params, lastParam) = when(returnType) {
                genericDto -> processParamsWithLastParam(params, "Class<T>")
                else -> params to ""
            }
            val expressionPart = removeGetByPrefix(methodName, entity.name)
            val (where, orderBy, limit, distinct) = parseAsJooqExpression(entity, expressionPart, params, true)

            check(!distinct) { "Distinct cannot be used whole table select" }

            return when (returnType) {
                dtoName -> ReturnStmt("dsl.selectFrom(table).where($where)$orderBy$limit.fetchOneInto(${dtoName}.class)")
                optionalDto -> ReturnStmt("dsl.selectFrom(table).where($where)$orderBy$limit.fetchOptionalInto(${dtoName}.class)")
                recordResult -> ReturnStmt("dsl.selectFrom(table).where($where)$orderBy$limit.fetchOne()")
                optionalRecord -> ReturnStmt("dsl.selectFrom(table).where($where)$orderBy$limit.fetchOptional()")
                genericDto -> ReturnStmt("dsl.selectFrom(table).where($where)$orderBy$limit.fetchOneInto($lastParam)")
                else -> error("Unsupported return type: $returnType")
            }
        } catch (ex: Exception) {
            throw RuntimeException("Failed to generate body for method \"$methodName\": ${ex.message}", ex)
        }
    }

    private fun generateCountByBody(
        entity: EntityElem,
        methodName: String,
        returnType: String,
        params: Map<String, String>
    ): Statement {
        try {
            val supportedTypes = setOf("int", "Integer")
            check(supportedTypes.contains(returnType)) { "Unsupported return type: '$returnType', expected: $supportedTypes" }

            val expressionPart = removeCountByPrefix(methodName, entity.name)
            val (where, orderBy, limit, distinct) = parseAsJooqExpression(entity, expressionPart, params, false)

            check(orderBy.isEmpty()) { "OrderBy cannot be used with count methods" }
            check(limit.isEmpty()) { "Top cannot be used with count methods" }
            check(!distinct) { "Distinct cannot be used whole table select" }

            return ReturnStmt("dsl.fetchCount(table, $where)")

        } catch (ex: Exception) {
            throw RuntimeException("Failed to generate body for method \"$methodName\": ${ex.message}", ex)
        }
    }

    private fun removeFindByPrefix(methodName: String, entityPrefix: String) =
        methodName.removePrefix(PREFIX_FIND).removePrefix(entityPrefix).removePrefix(PREFIX_BY)

    private fun removeGetByPrefix(methodName: String, entityPrefix: String) =
        methodName.removePrefix(PREFIX_GET).removePrefix(entityPrefix).removePrefix(PREFIX_BY)

    private fun removeCountByPrefix(methodName: String, entityPrefix: String) =
        methodName.removePrefix(PREFIX_COUNT).removePrefix(entityPrefix).removePrefix(PREFIX_BY)

    private fun parseAsJooqExpression(
        entity: EntityElem,
        expressionPart: String,
        params: Map<String, String>,
        singleResult: Boolean
    ): JooqExpression {
        val name = entity.name
        val tableName = name.camelCaseToSnakeCase().uppercase()
        val (names, distinct) = parseTokensAndDistinct(MethodExpressionParser.parse(expressionPart))
        val sb = StringBuilder()
        val paramNames = params.keys.toList()
        var orderBy = ""
        var limit = ""
        var paramIndex = -1
        var namesCount = 0
        var ignoreCaseIgnored = 0

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
                    val nextNextOp = if (index + 2 < names.size) names[index + 2] else null
                    val isOp = nextOp is MethodExpressionParser.Is || nextOp is MethodExpressionParser.IsNot
                    namesCount++

                    if (isOp && nextNextOp is MethodExpressionParser.Null) {
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
                        val isCollection = nextOp is MethodExpressionParser.In || nextOp is MethodExpressionParser.Not && nextNextOp is MethodExpressionParser.In
                        check(typesEqual(field, paramType, isCollection))
                        { "Field type (${field.asJavaType()}) and type ($paramType) of parameter \"$paramName\" mismatch" }
                        val ignoreCase = token.ignoreCase && paramType == "String"

                        if (token.ignoreCase && !ignoreCase) {
                            ignoreCaseIgnored++
                        }

                        when (nextOp) {
                            is MethodExpressionParser.StartingWith -> {
                                check(paramType == "String") { "Parameter type ($paramType) of parameter \"$paramName\" should be String for StartingWith method" }
                                val likeOp = if (ignoreCase) "likeIgnoreCase" else "like"
                                sb.append("Tables.${tableName}.${fieldName}.$likeOp($paramName + \"%\")")
                            }

                            is MethodExpressionParser.Containing -> {
                                check(paramType == "String") { "Parameter type ($paramType) of parameter \"$paramName\" should be String for Containing method" }
                                val likeOp = if (ignoreCase) "likeIgnoreCase" else "like"
                                sb.append("Tables.${tableName}.${fieldName}.$likeOp(\"%\" + $paramName + \"%\")")
                            }

                            is MethodExpressionParser.EndingWith -> {
                                check(paramType == "String") { "Parameter type ($paramType) of parameter \"$paramName\" should be String for EndingWith method" }
                                val likeOp = if (ignoreCase) "likeIgnoreCase" else "like"
                                sb.append("Tables.${tableName}.${fieldName}.$likeOp(\"%\" + $paramName)")
                            }

                            is MethodExpressionParser.Like -> {
                                check(paramType == "String") { "Parameter type ($paramType) of parameter \"$paramName\" should be String for Like method" }
                                val likeOp = if (ignoreCase) "likeIgnoreCase" else "like"
                                sb.append("Tables.${tableName}.${fieldName}.$likeOp($paramName)")
                            }

                            is MethodExpressionParser.GreaterThan -> {
                                sb.append("Tables.${tableName}.${fieldName}.greaterThan($paramName)")
                            }

                            is MethodExpressionParser.GreaterThanEqual -> {
                                sb.append("Tables.${tableName}.${fieldName}.greaterOrEqual($paramName)")
                            }

                            is MethodExpressionParser.LessThanEqual -> {
                                sb.append("Tables.${tableName}.${fieldName}.lessOrEqual($paramName)")
                            }

                            is MethodExpressionParser.LessThan -> {
                                sb.append("Tables.${tableName}.${fieldName}.lessThan($paramName)")
                            }

                            is MethodExpressionParser.Before -> {
                                check(paramType == "LocalDateTime") { "Parameter type ($paramType) of parameter \"$paramName\" should be LocalDateTime for Before method" }
                                sb.append("Tables.${tableName}.${fieldName}.lessThan($paramName)")
                            }

                            is MethodExpressionParser.After -> {
                                check(paramType == "LocalDateTime") { "Parameter type ($paramType) of parameter \"$paramName\" should be LocalDateTime for After method" }
                                sb.append("Tables.${tableName}.${fieldName}.greaterThan($paramName)")
                            }

                            is MethodExpressionParser.In -> {
                                sb.append("Tables.${tableName}.${fieldName}.in($paramName)")
                            }

                            is MethodExpressionParser.Between -> {
                                check(paramIndex + 1 < paramNames.size) { "Operator \"Between\" requires two parameters" }
                                paramIndex++
                                val paramName2 = paramNames[paramIndex]
                                val paramType2 = params[paramName]!!
                                check(typesEqual(field, paramType2))
                                { "Field type (${field.asJavaType()}) and type ($paramType2) of parameter \"$paramName2\" mismatch" }

                                sb.append("Tables.${tableName}.${fieldName}.between($paramName, $paramName2)")
                            }

                            is MethodExpressionParser.Not -> {

                                when (nextNextOp) {
                                    is MethodExpressionParser.In -> {
                                        sb.append("Tables.${tableName}.${fieldName}.notIn($paramName)")
                                    }

                                    is MethodExpressionParser.Between -> {
                                        check(paramIndex + 1 < paramNames.size) { "Operator \"Between\" requires two parameters" }
                                        paramIndex++
                                        val paramName2 = paramNames[paramIndex]
                                        val paramType2 = params[paramName]!!
                                        check(typesEqual(field, paramType2))
                                        { "Field type (${field.asJavaType()}) and type ($paramType2) of parameter \"$paramName2\" mismatch" }

                                        sb.append("Tables.${tableName}.${fieldName}.notBetween($paramName, $paramName2)")
                                    }

                                    is MethodExpressionParser.StartingWith -> {
                                        check(paramType == "String") { "Parameter type ($paramType) of parameter \"$paramName\" should be String for method: $nextNextOp" }
                                        val notLikeOp = if (ignoreCase) "notLikeIgnoreCase" else "notLike"
                                        sb.append("Tables.${tableName}.${fieldName}.$notLikeOp($paramName + \"%\")")
                                    }

                                    is MethodExpressionParser.Containing -> {
                                        check(paramType == "String") { "Parameter type ($paramType) of parameter \"$paramName\" should be String for method: $nextNextOp" }
                                        val notLikeOp = if (ignoreCase) "notLikeIgnoreCase" else "notLike"
                                        sb.append("Tables.${tableName}.${fieldName}.$notLikeOp(\"%\" + $paramName + \"%\")")
                                    }

                                    is MethodExpressionParser.EndingWith -> {
                                        check(paramType == "String") { "Parameter type ($paramType) of parameter \"$paramName\" should be String for method: $nextNextOp" }
                                        val notLikeOp = if (ignoreCase) "notLikeIgnoreCase" else "notLike"
                                        sb.append("Tables.${tableName}.${fieldName}.$notLikeOp(\"%\" + $paramName)")
                                    }

                                    is MethodExpressionParser.Like -> {
                                        check(paramType == "String") { "Parameter type ($paramType) of parameter \"$paramName\" should be String for method: $nextNextOp" }
                                        val notLikeOp = if (ignoreCase) "notLikeIgnoreCase" else "notLike"
                                        sb.append("Tables.${tableName}.${fieldName}.$notLikeOp($paramName)")
                                    }

                                    else -> {
                                        error("Unsupported operator: $nextNextOp")
                                    }
                                }
                            }

                            else -> {
                                val eqOp = if (ignoreCase) "equalIgnoreCase" else "eq"
                                sb.append("Tables.${tableName}.${fieldName}.$eqOp($paramName)")
                                if (nextOp is MethodExpressionParser.IsNot) {
                                    sb.append(".not()")
                                }
                            }
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

        if (paramIndex > 0 && paramIndex == ignoreCaseIgnored) {
            error("At least one parameter should be String to use AllIgnoreCase")
        }

        return JooqExpression(where = sb.toString(), orderBy = orderBy, limit = limit, distinct = distinct)
    }

    private fun parseTokensAndDistinct(names: List<MethodExpressionParser.Token>): Pair<List<MethodExpressionParser.Token>, Boolean> {
        val distinct: Boolean = names.firstOrNull() == MethodExpressionParser.Distinct
        return if (distinct)
            names.subList(1, names.size) to true
        else
            names to false
    }

    private fun typesEqual(field: FieldElem, paramType: String, supportCollection: Boolean = false): Boolean = when (field.asJavaType()) {
        paramType -> true
        "Integer" -> paramType == "int" || supportCollection && (paramType == "Collection<Integer>" || paramType == "List<Integer>")
        "Long" -> paramType == "long" || supportCollection && (paramType == "Collection<Long>" || paramType == "List<Long>")
        "Boolean" -> paramType == "boolean" || supportCollection && (paramType == "Collection<Boolean>" || paramType == "List<Boolean>")
        else -> error("Unsupported field type: ${field.asJavaType()}")
    }

    /**
     * Removes last parameter from params map and returns it as a separate value.
     *
     * Checks that last parameter is of required type.
     */
    private fun processParamsWithLastParam(params: Map<String, String>, reqType: String): Pair<Map<String, String>, String> {
        check(params.isNotEmpty()) { "Last parameter required to be: $reqType" }
        val lastParam = params.keys.last()
        check(params[lastParam] == reqType) { "Last parameter required to be '$reqType', but found: " + params[lastParam] }
        return params.filterKeys { it != lastParam } to lastParam
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

    private data class JooqExpression(val where: String, val orderBy: String, val limit: String, val distinct: Boolean)


    private companion object {
        const val PREFIX_FIND = "find"
        const val PREFIX_GET = "get"
        const val PREFIX_COUNT = "count"
        const val PREFIX_BY = "By"
    }
}
