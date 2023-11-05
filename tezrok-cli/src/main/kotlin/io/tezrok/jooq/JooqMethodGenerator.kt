package io.tezrok.jooq

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.TypeParameter
import io.tezrok.api.input.EntityElem
import io.tezrok.api.input.EntityRelation
import io.tezrok.api.input.FieldElem
import io.tezrok.api.java.JavaClassNode
import io.tezrok.api.java.JavaMethodNode
import io.tezrok.util.addImportsByType
import io.tezrok.util.asJavaType
import io.tezrok.util.camelCaseToSnakeCase
import io.tezrok.util.camelCaseToSqlUppercase

/**
 * Generates jooq methods for repository classes.
 */
internal class JooqMethodGenerator(
    private val entity: EntityElem,
    private val repoClass: JavaClassNode,
    private val entities: Map<String, EntityElem>,
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
        val params = method.parameters.associate { it.nameAsString to it.typeAsString }.toMutableMap()
        params.forEach { param -> newMethod.addParameter(param.value, param.key) }

        when {
            methodName.startsWith(PREFIX_FIND) ->
                newMethod.setBody(generateFindByBody(methodName, returnType, params))

            methodName.startsWith(PREFIX_GET) ->
                newMethod.setBody(generateGetByBody(methodName, returnType, params))

            methodName.startsWith(PREFIX_COUNT) ->
                newMethod.setBody(generateCountByBody(methodName, returnType, params))

            else -> error("Unsupported method name: $methodName")
        }

        repoClass.addImportsByType(returnType)
        params.values.toSet().forEach { type -> repoClass.addImportsByType(type) }
        addCustomMethodsComments(newMethod)
    }

    /**
     * Generates methods for repository classes by method name only without params.
     */
    fun generateByOnlyName(methodName: String) {
        val newMethod = repoClass.addMethod(methodName)
            .withModifiers(Modifier.Keyword.PUBLIC)
        val bodyGen = when {
            methodName.startsWith(PREFIX_FIND) -> generateFindByBodyOnlyByName(methodName)

            methodName.startsWith(PREFIX_GET) -> generateGetByBodyOnlyByName(methodName)

            methodName.startsWith(PREFIX_COUNT) -> TODO("support count methods")

            else -> error("Unsupported method name: $methodName")
        }
        newMethod.setBody(bodyGen.body)
        bodyGen.params.forEach { param -> newMethod.addParameter(param.value, param.key) }
        newMethod.setReturnType(bodyGen.returnType)
        if (bodyGen.returnType == GENERIC_LIST) {
            newMethod.setTypeParameters(listOf(TypeParameter("T")))
        }
        repoClass.addImportsByType(bodyGen.returnType)
        bodyGen.params.values.toSet().forEach { type -> repoClass.addImportsByType(type) }
        addCustomMethodsComments(newMethod)
    }

    private fun addCustomMethodsComments(method: JavaMethodNode) {
        entity.customComments?.get(method.getName())?.let { comment ->
            val packagePath = repoClass.getJavaFile().getJavaRoot()?.applicationPackageRoot?.getPackage() ?: error("Package not found")
            method.setJavadocComment(comment)
            repoClass.addImportsByType(comment, entities, packagePath)
        }
    }

    private fun getMethodPrefix(methodName: String): String {
        val index = methodName.indexOf("By")

        return if (index > 0) {
            methodName.substring(0, index)
        } else {
            ""
        }
    }

    private fun getReturnTypeByOnlyName(methodName: String, dtoName: String) = when {
        methodName.startsWith(PREFIX_FIND) -> "List<$dtoName>"

        methodName.startsWith(PREFIX_GET) -> dtoName

        methodName.startsWith(PREFIX_COUNT) -> "int"

        else -> error("Unsupported method name: $methodName")
    }

    /**
     * Generates body for List<DtoType> findBySomeFieldOrExpression() methods.
     */
    private fun generateFindByBody(
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
            val supportedTypes = setOf(dtoList, recordList, dtoPage, recordPage, GENERIC_LIST)
            check(supportedTypes.contains(returnType)) { "Unsupported return type: '$returnType', expected: $supportedTypes" }

            val pageableRequest = returnType == dtoPage || returnType == recordPage
            val (params, lastParam) = when (returnType) {
                dtoPage, recordPage -> processParamsWithLastParam(params, "Pageable")
                GENERIC_LIST -> processParamsWithLastParam(params, GENERIC_CLASS)
                else -> params to ""
            }
            val expressionPart = removeFindByPrefix(methodName, "${entity.name}s")
            val (where, orderBy, limit, distinct) = parseAsJooqExpression(expressionPart, params, false)

            check(limit.isEmpty() || !pageableRequest) { "Top and Pageable cannot be used together" }
            check(orderBy.isEmpty() || !pageableRequest) { "OrderBy and Pageable cannot be used together" }
            check(!distinct) { DISTINCT_CANNOT_BE_USED_WHOLE_TABLE }

            return when (returnType) {
                dtoList -> ReturnStmt("dsl.selectFrom(table).where($where)$orderBy$limit.fetchInto(${dtoName}.class)")
                recordList -> ReturnStmt("dsl.selectFrom(table).where($where)$orderBy$limit.fetch()")
                dtoPage -> ReturnStmt("findPage($where, $lastParam, ${dtoName}.class)")
                recordPage -> ReturnStmt("findPage($where, $lastParam, $recordName.class)")
                GENERIC_LIST -> ReturnStmt("dsl.selectFrom(table).where($where)$orderBy$limit.fetchInto($lastParam)")
                else -> error("Unsupported return type: $returnType")
            }
        } catch (ex: Exception) {
            throw RuntimeException("Failed to generate body for method \"$methodName\": ${ex.message}", ex)
        }
    }

    /**
     * Generates body for method by expression like "findBySomeFieldOrExpression" without params.
     */
    private fun generateFindByBodyOnlyByName(
        methodName: String
    ): MethodGen {
        try {
            val params = mutableMapOf<String, String>()
            val dtoName = "${entity.name}Dto"
            val defaultReturnType = getReturnTypeByOnlyName(methodName, dtoName)
            val methodName = methodName.removePrefix(PREFIX_FIND)
            val methodPrefix = getMethodPrefix(methodName)
            val relTables = getRelatedTables(methodPrefix)
            val selectedColumns = parseFields(methodPrefix, relTables)
            val returnType = getReturnTypeBySelectedColumns(selectedColumns, defaultReturnType)
            val dtoList = "List<$dtoName>"
            if (selectedColumns.isEmpty()) {
                val supportedTypes = setOf(dtoList)
                check(supportedTypes.contains(returnType)) { "Unsupported return type: '$returnType', expected: $supportedTypes" }
            }
            val expressionPart = removeFindByPrefix(methodName, methodPrefix)
            val (where, orderBy, limit, distinct, paramsOut) = parseAsJooqExpression(
                expressionPart,
                emptyMap(),
                singleResult = false,
                relTables,
                extractParams = true
            )
            // got params from expression
            paramsOut.forEach(params::put)

            check(!distinct) { DISTINCT_CANNOT_BE_USED_WHOLE_TABLE }

            if (relTables == null) {
                if (selectedColumns.isNotEmpty()) {
                    val tableName = entity.name.camelCaseToSqlUppercase()
                    val selectFields = selectedColumns.map { field -> field.name.camelCaseToSqlUppercase() }
                        .map { fieldName -> "Tables.${tableName}.${fieldName}" }
                        .joinToString(separator = ", ")

                    if (selectedColumns.size == 1) {
                        val fieldJavaType = selectedColumns.first().asJavaType()
                        return MethodGen(
                            params = params,
                            returnType = returnType,
                            ReturnStmt("dsl.select($selectFields).where($where)$orderBy$limit.fetch(0, ${fieldJavaType}.class)")
                        )
                    }

                    // if selected columns more than one, we return custom dto and need to pass Class<T> as last param
                    val lastParamName = makeSafeParamName(params)
                    params[lastParamName] = GENERIC_CLASS
                    return MethodGen(
                        params = params,
                        returnType = returnType,
                        ReturnStmt("dsl.select($selectFields).where($where)$orderBy$limit.fetchInto($lastParamName)")
                    )
                }

                return when (returnType) {
                    dtoList -> MethodGen(
                        params = params,
                        returnType = returnType,
                        ReturnStmt("dsl.selectFrom(table).where($where)$orderBy$limit.fetchInto(${dtoName}.class)")
                    )

                    else -> error("Unsupported return type: $returnType")
                }
            } else {
                val tableName = entity.name.camelCaseToSqlUppercase()
                val primaryField = entity.fields.first { it.primary == true }.name.camelCaseToSqlUppercase()

                val from = if (relTables.relTable != null) {
                    val relTableName = relTables.relTable.name.camelCaseToSqlUppercase()
                    val targetTableName = relTables.target.name.camelCaseToSqlUppercase()
                    val primaryTargetField =
                        relTables.target.fields.first { it.primary == true }.name.camelCaseToSqlUppercase()
                    val relField1 = relTables.relTable.fields[0].name.camelCaseToSqlUppercase()
                    val relField2 = relTables.relTable.fields[1].name.camelCaseToSqlUppercase()

                    // TODO: optimize query when condition only by primary keys
                    """select(Tables.$tableName.fields()).from(Tables.$tableName)
                    |                .join(Tables.$relTableName).on(Tables.$tableName.$primaryField.eq(Tables.$relTableName.$relField2))
                    |                .join(Tables.$targetTableName).on(Tables.$targetTableName.$primaryTargetField.eq(Tables.$relTableName.$relField1))"""
                        .trimMargin()
                } else {
                    "selectFrom(table)"
                }

                return when (returnType) {
                    dtoList -> MethodGen(
                        params = params,
                        returnType = returnType,
                        ReturnStmt("dsl.$from.where($where)$orderBy$limit.fetchInto(${dtoName}.class)")
                    )

                    else -> error("Unsupported return type: $returnType")
                }
            }
        } catch (ex: Exception) {
            throw RuntimeException("Failed to generate body for method \"$methodName\": ${ex.message}", ex)
        }
    }

    /**
     * Generates body for Optional<DtoType> getBySomeFieldOrExpression() methods.
     */
    private fun generateGetByBody(
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

            val (params, lastParam) = when (returnType) {
                genericDto -> processParamsWithLastParam(params, GENERIC_CLASS)
                else -> params to ""
            }
            val expressionPart = removeGetByPrefix(methodName, entity.name)
            val (where, orderBy, limit, distinct) = parseAsJooqExpression(expressionPart, params, true)

            check(!distinct) { DISTINCT_CANNOT_BE_USED_WHOLE_TABLE }

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

    /**
     * Generates body for method by expression like "getBySomeFieldOrExpression" without params.
     */
    private fun generateGetByBodyOnlyByName(
        methodName: String
    ): MethodGen {
        try {
            val dtoName = "${entity.name}Dto"
            val returnType = getReturnTypeByOnlyName(methodName, dtoName)
            val params = mutableMapOf<String, String>()
            val methodName = methodName.removePrefix(PREFIX_GET)
            val supportedTypes = setOf(dtoName)
            check(supportedTypes.contains(returnType)) { "Unsupported return type: '$returnType', expected: $supportedTypes" }

            val methodPrefix = getMethodPrefix(methodName)
            val relTables = getRelatedTables(methodPrefix)
            val expressionPart = removeGetByPrefix(methodName, entity.name)
            val (where, orderBy, limit, distinct, paramsOut) = parseAsJooqExpression(
                expressionPart,
                params,
                singleResult = true,
                relTables,
                extractParams = true
            )
            // got params from expression
            paramsOut.forEach(params::put)

            check(!distinct) { DISTINCT_CANNOT_BE_USED_WHOLE_TABLE }

            return when (returnType) {
                dtoName -> return MethodGen(
                    params = params,
                    returnType = returnType,
                    ReturnStmt("dsl.selectFrom(table).where($where)$orderBy$limit.fetchOneInto(${dtoName}.class)")
                )

                else -> error("Unsupported return type: $returnType")
            }
        } catch (ex: Exception) {
            throw RuntimeException("Failed to generate body for method \"$methodName\": ${ex.message}", ex)
        }
    }

    private fun makeSafeParamName(params: Map<String, String>): String {
        var index = 0
        var paramName = "type"
        while (params.containsKey(paramName)) {
            paramName = "type${++index}"
        }
        return paramName
    }

    private fun getReturnTypeBySelectedColumns(
        selectedColumns: List<FieldElem>,
        defaultReturnType: String
    ): String {
        return if (selectedColumns.isEmpty()) {
            defaultReturnType
        } else if (selectedColumns.size == 1) {
            "List<${selectedColumns.first().asJavaType()}>"
        } else {
            // when we have several columns, we return custom dto
            GENERIC_LIST
        }
    }

    private fun parseFields(
        methodPrefix: String,
        relTables: RelationTables?
    ): List<FieldElem> {
        if (relTables != null || methodPrefix.isEmpty()) {
            return emptyList()
        }

        val singleField = tryGetFieldByName(methodPrefix.decapitalize())
        if (singleField != null) {
            return listOf(singleField)
        }

        return findFieldsByParts(methodPrefix.camelCaseToSnakeCase().split("_"))
    }

    private fun findFieldsByParts(parts: List<String>): List<FieldElem> {
        val result = mutableListOf<FieldElem>()
        val fields = entity.fields.filter { it.logicField != true }.associateBy { it.name }
        var index = 0

        while (index < parts.size) {
            var found = false;
            var curFieldName = ""
            for (i in index until parts.size) {
                curFieldName += parts[i]
                if (index == i) {
                    // field name starts from lower case
                    curFieldName = curFieldName.decapitalize()
                }
                val field = fields[curFieldName]
                if (field != null) {
                    result.add(field)
                    index = i + 1
                    found = true
                    break
                }
            }

            if (!found) {
                error("Field not found: $curFieldName in entity: ${entity.name}")
            }
        }

        return result
    }

    /**
     * Generates body for int countBySomeFieldOrExpression() methods.
     */
    private fun generateCountByBody(
        methodName: String,
        returnType: String,
        params: Map<String, String>
    ): Statement {
        try {
            val supportedTypes = setOf("int", "Integer")
            check(supportedTypes.contains(returnType)) { "Unsupported return type: '$returnType', expected: $supportedTypes" }

            val expressionPart = removeCountByPrefix(methodName, entity.name)
            val (where, orderBy, limit, distinct) = parseAsJooqExpression(expressionPart, params, false)

            check(orderBy.isEmpty()) { "OrderBy cannot be used with count methods" }
            check(limit.isEmpty()) { "Top cannot be used with count methods" }
            check(!distinct) { DISTINCT_CANNOT_BE_USED_WHOLE_TABLE }

            return ReturnStmt("dsl.fetchCount(table, $where)")

        } catch (ex: Exception) {
            throw RuntimeException("Failed to generate body for method \"$methodName\": ${ex.message}", ex)
        }
    }

    /**
     * By method prefix like "OrderOtherItems" find target entity (Order), it's field (otherItems) and relation table (OrderItemOtherItems).
     */
    private fun getRelatedTables(methodPrefix: String): RelationTables? {
        val parts = methodPrefix.camelCaseToSnakeCase().split("_")
        if (parts.size < 2) {
            return null
        }

        val (targetEntity, fieldName) = findTargetEntityAndField(parts) ?: return null
        val field = targetEntity.fields.find { it.name == fieldName }
            ?: error("Field not found: $fieldName in entity: ${targetEntity.name}")
        check(field.type == entity.name) { "Field type (${field.type}) and entity name (${entity.name}) mismatch" }

        return when (field.relation) {
            EntityRelation.ManyToMany -> {
                val fullName = "${targetEntity.name}.${fieldName}"
                val relTable = entities.values.find { it.syntheticTo == fullName }
                    ?: error("Relation table not found: $fullName")

                RelationTables(targetEntity, field, relTable)
            }

            EntityRelation.OneToMany -> RelationTables(targetEntity, field, null)
            else -> error("Unsupported relation: ${field.relation}")
        }
    }

    private fun findTargetEntityAndField(parts: List<String>): Pair<EntityElem, String>? {
        var index = -1
        var entityName = ""

        while (++index < parts.size) {
            entityName += parts[index]
            val entity = entities[entityName]
            if (entity != null) {
                return entity to parts.subList(index + 1, parts.size).joinToString(separator = "").decapitalize()
            }
        }

        return null
    }

    private fun removeFindByPrefix(methodName: String, entityPrefix: String) =
        methodName.removePrefix(PREFIX_FIND).removePrefix(entityPrefix).removePrefix(PREFIX_BY)

    private fun removeGetByPrefix(methodName: String, entityPrefix: String) =
        methodName.removePrefix(PREFIX_GET).removePrefix(entityPrefix).removePrefix(PREFIX_BY)

    private fun removeCountByPrefix(methodName: String, entityPrefix: String) =
        methodName.removePrefix(PREFIX_COUNT).removePrefix(entityPrefix).removePrefix(PREFIX_BY)

    private fun parseAsJooqExpression(
        expressionPart: String,
        params: Map<String, String>,
        singleResult: Boolean,
        relTables: RelationTables? = null,
        extractParams: Boolean = false
    ): JooqExpression {
        val (names, distinct) = parseTokensAndDistinct(MethodExpressionParser.parse(expressionPart))
        val sb = StringBuilder()
        var orderBy = ""
        var limit = ""
        var paramIndex = -1
        var namesCount = 0
        var ignoreCaseSkipped = 0
        val paramsOut = mutableMapOf<String, String>()

        names.forEachIndexed { index, token ->
            when (token) {
                is Token.And,
                is Token.Or -> {
                    val operator = token.name.lowercase()
                    sb.append(".$operator(")
                }

                is Token.Name -> {
                    val fieldFull = getFieldByName(token.name, relTables)
                    val tableName = fieldFull.entity.name.camelCaseToSqlUppercase()
                    val field = fieldFull.field
                    val fieldName = field.name.camelCaseToSqlUppercase()
                    val nextOp = if (index + 1 < names.size) names[index + 1] else null
                    val nextNextOp = if (index + 2 < names.size) names[index + 2] else null
                    val isOp = nextOp is Token.Is || nextOp is Token.IsNot
                    namesCount++

                    if (isOp && nextNextOp is Token.Null) {
                        sb.append("Tables.${tableName}.${fieldName}")
                        if (nextOp is Token.Is) {
                            sb.append(".isNull()")
                        } else {
                            sb.append(".isNotNull()")
                        }
                    } else {
                        val isCollection = nextOp is Token.In || nextOp is Token.Not && nextNextOp is Token.In
                        paramIndex++
                        val (paramName, paramType) = extractParam(
                            token.name,
                            paramIndex,
                            isCollection,
                            params,
                            field,
                            extractParams
                        )
                        paramsOut[paramName] = paramType

                        val ignoreCase = token.ignoreCase && paramType == "String"

                        if (token.ignoreCase && !ignoreCase) {
                            ignoreCaseSkipped++
                        }

                        when (nextOp) {
                            is Token.StartingWith -> {
                                check(paramType == "String") { "Parameter type ($paramType) of parameter \"$paramName\" should be String for StartingWith method" }
                                val likeOp = if (ignoreCase) "likeIgnoreCase" else "like"
                                sb.append("Tables.${tableName}.${fieldName}.$likeOp($paramName + \"%\")")
                            }

                            is Token.Containing -> {
                                check(paramType == "String") { "Parameter type ($paramType) of parameter \"$paramName\" should be String for Containing method" }
                                val likeOp = if (ignoreCase) "likeIgnoreCase" else "like"
                                sb.append("Tables.${tableName}.${fieldName}.$likeOp(\"%\" + $paramName + \"%\")")
                            }

                            is Token.EndingWith -> {
                                check(paramType == "String") { "Parameter type ($paramType) of parameter \"$paramName\" should be String for EndingWith method" }
                                val likeOp = if (ignoreCase) "likeIgnoreCase" else "like"
                                sb.append("Tables.${tableName}.${fieldName}.$likeOp(\"%\" + $paramName)")
                            }

                            is Token.Like -> {
                                check(paramType == "String") { "Parameter type ($paramType) of parameter \"$paramName\" should be String for Like method" }
                                val likeOp = if (ignoreCase) "likeIgnoreCase" else "like"
                                sb.append("Tables.${tableName}.${fieldName}.$likeOp($paramName)")
                            }

                            is Token.GreaterThan -> {
                                sb.append("Tables.${tableName}.${fieldName}.greaterThan($paramName)")
                            }

                            is Token.GreaterThanEqual -> {
                                sb.append("Tables.${tableName}.${fieldName}.greaterOrEqual($paramName)")
                            }

                            is Token.LessThanEqual -> {
                                sb.append("Tables.${tableName}.${fieldName}.lessOrEqual($paramName)")
                            }

                            is Token.LessThan -> {
                                sb.append("Tables.${tableName}.${fieldName}.lessThan($paramName)")
                            }

                            is Token.Before -> {
                                check(paramType == "LocalDateTime") { "Parameter type ($paramType) of parameter \"$paramName\" should be LocalDateTime for Before method" }
                                sb.append("Tables.${tableName}.${fieldName}.lessThan($paramName)")
                            }

                            is Token.After -> {
                                check(paramType == "LocalDateTime") { "Parameter type ($paramType) of parameter \"$paramName\" should be LocalDateTime for After method" }
                                sb.append("Tables.${tableName}.${fieldName}.greaterThan($paramName)")
                            }

                            is Token.In -> {
                                sb.append("Tables.${tableName}.${fieldName}.in($paramName)")
                            }

                            is Token.Between -> {
                                paramIndex++
                                val (paramName2, paramType2) = extractParam(
                                    token.name,
                                    paramIndex,
                                    isCollection,
                                    params,
                                    field,
                                    extractParams,
                                    "Operator \"Between\" requires two parameters"
                                )
                                paramsOut[paramName2] = paramType2

                                sb.append("Tables.${tableName}.${fieldName}.between($paramName, $paramName2)")
                            }

                            is Token.Not -> {

                                when (nextNextOp) {
                                    is Token.In -> {
                                        sb.append("Tables.${tableName}.${fieldName}.notIn($paramName)")
                                    }

                                    is Token.Between -> {
                                        paramIndex++
                                        val (paramName2, paramType2) = extractParam(
                                            token.name,
                                            paramIndex,
                                            isCollection,
                                            params,
                                            field,
                                            extractParams,
                                            "Operator \"Between\" requires two parameters"
                                        )
                                        paramsOut[paramName2] = paramType2

                                        sb.append("Tables.${tableName}.${fieldName}.notBetween($paramName, $paramName2)")
                                    }

                                    is Token.StartingWith -> {
                                        check(paramType == "String") { "Parameter type ($paramType) of parameter \"$paramName\" should be String for method: $nextNextOp" }
                                        val notLikeOp = if (ignoreCase) "notLikeIgnoreCase" else "notLike"
                                        sb.append("Tables.${tableName}.${fieldName}.$notLikeOp($paramName + \"%\")")
                                    }

                                    is Token.Containing -> {
                                        check(paramType == "String") { "Parameter type ($paramType) of parameter \"$paramName\" should be String for method: $nextNextOp" }
                                        val notLikeOp = if (ignoreCase) "notLikeIgnoreCase" else "notLike"
                                        sb.append("Tables.${tableName}.${fieldName}.$notLikeOp(\"%\" + $paramName + \"%\")")
                                    }

                                    is Token.EndingWith -> {
                                        check(paramType == "String") { "Parameter type ($paramType) of parameter \"$paramName\" should be String for method: $nextNextOp" }
                                        val notLikeOp = if (ignoreCase) "notLikeIgnoreCase" else "notLike"
                                        sb.append("Tables.${tableName}.${fieldName}.$notLikeOp(\"%\" + $paramName)")
                                    }

                                    is Token.Like -> {
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
                                if (nextOp is Token.IsNot) {
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

                is Token.OrderBy -> orderBy = ".orderBy("
                is Token.SortName -> {
                    // TODO: support several fields in order by
                    val fieldFull = getFieldByName(token.name, relTables)
                    val field = fieldFull.field
                    val fieldName = field.name.camelCaseToSqlUppercase()
                    val tableName = fieldFull.entity.name.camelCaseToSqlUppercase()
                    orderBy += "Tables.${tableName}.${fieldName}"

                    if (token.sort == Token.Sort.Asc) {
                        orderBy += ".asc()"
                    } else if (token.sort == Token.Sort.Desc) {
                        orderBy += ".desc()"
                    }
                    orderBy += ")"
                }

                is Token.Top -> {
                    check(!singleResult || token.limit == 1) { "Top limit should be 1 for single result method" }
                    limit = ".limit(${token.limit})"
                }
            }
        }

        paramIndex++
        if (params.isNotEmpty()) {
            if (paramIndex < params.size) {
                val paramNames = params.keys.toList()
                val notUsedParams = paramNames.subList(paramIndex, params.size).joinToString(", ")
                error("Not all parameters used in method: $notUsedParams")
            }
        }

        if (singleResult && orderBy.isNotEmpty() && limit.isEmpty()) {
            error("Single result method with OrderBy should use top 1")
        }

        if (paramIndex > 0 && paramIndex == ignoreCaseSkipped) {
            error("At least one parameter should be String to use AllIgnoreCase")
        }

        return JooqExpression(
            where = sb.toString(),
            orderBy = orderBy,
            limit = limit,
            distinct = distinct,
            params = paramsOut
        )
    }

    private fun extractParam(
        tokenName: String,
        paramIndex: Int,
        isCollection: Boolean,
        params: Map<String, String>,
        field: FieldElem,
        extractParam: Boolean,
        customMessage: String? = null
    ): Pair<String, String> {
        if (params.isNotEmpty() || !extractParam) {
            check(paramIndex < params.size) {
                customMessage ?: "Parameter index of '$tokenName' out of bounds (${params.size})"
            }
            val paramNames = params.keys.toList()
            val paramName = paramNames[paramIndex]
            val paramType = params[paramName] ?: error("Parameter type not found: $paramName")
            check(typesEqual(field, paramType, isCollection))
            { "Field type (${field.asJavaType()}) and type ($paramType) of parameter \"$paramName\" mismatch" }

            return paramName to paramType
        } else {
            return if (isCollection) {
                tokenName + "s" to "Collection<${field.asJavaType()}>"
            } else {
                tokenName to field.asJavaType(tryPrimitive = true)
            }
        }
    }

    private fun parseTokensAndDistinct(names: List<Token>): Pair<List<Token>, Boolean> {
        val distinct: Boolean = names.firstOrNull() == Token.Distinct
        return if (distinct)
            names.subList(1, names.size) to true
        else
            names to false
    }

    private fun typesEqual(field: FieldElem, paramType: String, supportCollection: Boolean = false): Boolean =
        when (field.asJavaType()) {
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
    private fun processParamsWithLastParam(
        params: Map<String, String>,
        reqType: String
    ): Pair<Map<String, String>, String> {
        check(params.isNotEmpty()) { "Last parameter required to be: $reqType" }
        val lastParam = params.keys.last()
        check(params[lastParam] == reqType) { "Last parameter required to be '$reqType', but found: " + params[lastParam] }
        return params.filterKeys { it != lastParam } to lastParam
    }

    /**
     * Get field by name from entity or related entity.
     *
     * Note: when we have related tables, we should refer to field with table prefix.
     */
    private fun getFieldByName(name: String, relTables: RelationTables?): EntityField {
        if (relTables != null) {
            val fieldName = name.capitalize()
            val targetEntity = relTables.target.name
            val targetField =
                relTables.target.fields.find { field -> "${targetEntity}${field.name.capitalize()}" == fieldName }

            if (targetField != null) {
                return EntityField(relTables.target, targetField)
            }

            val sourceField = entity.fields.find { field -> "${entity.name}${field.name.capitalize()}" == fieldName }
            if (sourceField != null) {
                return EntityField(entity, sourceField)
            }

            error("Field ($name) not found in entity (${entity.name}) or related entity (${relTables.target.name})")
        }

        val field = entity.getField(name)
        if (field.logicField == true) {
            // TODO: support logic fields in method name expressions
            error("Logic field (${field.name}) cannot be used in method expression, use instead related real field")
        }

        return EntityField(entity, field)
    }

    private fun tryGetFieldByName(name: String): FieldElem? {
        val field = entity.tryGetField(name) ?: return null
        if (field.logicField == true) {
            // TODO: support logic fields in method name expressions
            error("Logic field (${field.name}) cannot be used in method expression, use instead related real field")
        }

        return field
    }

    private data class JooqExpression(
        val where: String,
        val orderBy: String,
        val limit: String,
        val distinct: Boolean,
        val params: Map<String, String>
    )

    /**
     * Used when source table related with target table via relation table.
     */
    private data class RelationTables(val target: EntityElem, val field: FieldElem, val relTable: EntityElem?)

    private data class EntityField(val entity: EntityElem, val field: FieldElem)

    private data class MethodGen(val params: Map<String, String>, val returnType: String, val body: Statement)

    private companion object {
        const val PREFIX_FIND = "find"
        const val PREFIX_GET = "get"
        const val PREFIX_COUNT = "count"
        const val PREFIX_BY = "By"
        const val DISTINCT_CANNOT_BE_USED_WHOLE_TABLE = "Distinct cannot be used whole table select"
        const val GENERIC_LIST = "List<T>"
        const val GENERIC_CLASS = "Class<T>"
    }
}
