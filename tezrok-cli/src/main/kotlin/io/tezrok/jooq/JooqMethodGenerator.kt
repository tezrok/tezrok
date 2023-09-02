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
     * Generates body for List<DtoType> findBySomeField() methods.
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
            check(returnType == "List<$dtoName>") { "Unsupported return type: $returnType" }

            val fieldName = methodName.substring(PREFIX_FIND_BY.length).decapitalize()
            val field = entity.fields.find { it.name == fieldName }
                ?: error("Field ($fieldName) not found in entity (${entity.name})")
            val paramName = params.keys.first()
            val paramType = params[paramName] ?: error("Parameter type not found for parameter: $paramName")

            check(field.asJavaType() == paramType) { "Field type (${field.asJavaType()}) and parameter type ($paramType) mismatch" }

            val uName = name.camelCaseToSnakeCase().uppercase()
            val uField = fieldName.camelCaseToSnakeCase().uppercase()
            return ReturnStmt("dsl.selectFrom(table).where(Tables.${uName}.${uField}.eq($paramName)).fetchInto(${dtoName}.class)")
        } catch (ex: Exception) {
            throw RuntimeException("Failed to generate body for method \"$methodName\": ${ex.message}", ex)
        }
    }

    private companion object {
        const val PREFIX_FIND_BY = "findBy"
    }
}
