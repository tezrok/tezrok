package io.tezrok.api.java

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration

/**
 * Node that represents a Java class or interface
 */
open class JavaClassNode(private val clazz: ClassOrInterfaceDeclaration) {

    fun getMethod(name: String): JavaMethodNode = JavaMethodNode(clazz.methods.first { it.nameAsString == name })

    fun addMethod(name: String): JavaMethodNode = JavaMethodNode(clazz.addMethod(name))

    fun hasMethod(name: String): Boolean = clazz.methods.any { it.nameAsString == name }

    fun withModifiers(vararg modifiers: Modifier.Keyword): JavaClassNode {
        clazz.setModifiers(*modifiers)
        return this
    }
}
