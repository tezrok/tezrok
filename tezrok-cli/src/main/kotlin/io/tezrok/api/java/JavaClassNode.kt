package io.tezrok.api.java

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.type.TypeParameter

/**
 * Node that represents a Java class or interface
 */
open class JavaClassNode(private val clazz: ClassOrInterfaceDeclaration) {
    fun getName(): String = clazz.nameAsString

    fun getMethod(name: String): JavaMethodNode? = clazz.methods.filter { it.nameAsString == name }
            .map { JavaMethodNode(it) }
            .firstOrNull()

    fun addMethod(name: String): JavaMethodNode = JavaMethodNode(clazz.addMethod(name))

    fun getOrAddMethod(name: String): JavaMethodNode = getMethod(name) ?: addMethod(name)

    fun hasMethod(name: String): Boolean = clazz.methods.any { it.nameAsString == name }

    fun withModifiers(vararg modifiers: Modifier.Keyword): JavaClassNode {
        clazz.setModifiers(*modifiers)
        return this
    }

    fun addAnnotation(annotationClass: Class<out Annotation>): JavaClassNode {
        clazz.addAnnotation(annotationClass)
        return this
    }

    fun addImport(importClass: Class<*>): JavaClassNode {
        clazz.tryAddImportToParentCompilationUnit(importClass)
        return this
    }

    fun setTypeParameters(vararg params: String): JavaClassNode {
        clazz.setTypeParameters(NodeList(params.map { TypeParameter(it) }))
        return this
    }
}
