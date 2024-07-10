package io.tezrok.api.java

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.expr.*

open class JavaMethodParameter(private val parameter: Parameter, private val parent: JavaMethodNode) {
    fun getName(): String = parameter.nameAsString

    fun getTypeAsString(): String = parameter.typeAsString


    fun addAnnotation(
        annotationClass: Class<out Annotation>,
        vararg fields: Pair<String, Expression>
    ): JavaMethodParameter {
        return addAnnotation(annotationClass, fields.toMap())
    }

    fun addAnnotation(annotationClass: Class<out Annotation>, fields: Map<String, Expression>): JavaMethodParameter {
        if (fields.isNotEmpty()) {
            parent.addImport(annotationClass)
            val annotation = parameter.addAndGetAnnotation(annotationClass.simpleName)
            annotation.setPairs(NodeList(fields.map { MemberValuePair(it.key, it.value) }))
        } else {
            addAnnotation(annotationClass)
        }
        return this
    }

    fun addAnnotation(annotationClass: Class<out Annotation>): JavaMethodParameter {
        parameter.addAnnotation(annotationClass)
        return this
    }

    fun addAnnotation(annotationExpr: String, pairs: Map<String, Expression> = emptyMap()): JavaMethodParameter {
        val annotation = parameter.addAndGetAnnotation(annotationExpr)
        annotation.setPairs(NodeList(pairs.map { MemberValuePair(it.key, it.value) }))
        return this
    }

    fun addAnnotation(annotationExpr: NormalAnnotationExpr): JavaMethodParameter {
        parameter.addAnnotation(annotationExpr)
        return this
    }

    fun addAnnotation(annotationClass: Class<out Annotation>, expression: Expression): JavaMethodParameter {
        parent.addImport(annotationClass)
        parameter.addAnnotation(SingleMemberAnnotationExpr(Name(annotationClass.simpleName), expression))
        return this
    }

    fun addAnnotation(annotationClass: Class<out Annotation>, expression: String): JavaMethodParameter {
        return addAnnotation(annotationClass, StringLiteralExpr(expression))
    }
}
