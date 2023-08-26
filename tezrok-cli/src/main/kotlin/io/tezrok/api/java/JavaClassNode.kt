package io.tezrok.api.java

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.type.TypeParameter
import java.util.stream.Stream
import kotlin.streams.asStream

/**
 * Node that represents a Java class or interface
 */
open class JavaClassNode(private val clazz: ClassOrInterfaceDeclaration) {
    fun getName(): String = clazz.nameAsString

    fun getFullName(): String = clazz.fullyQualifiedName.orElseGet { getName() }

    fun getParent(): CompilationUnit = clazz.findAncestor(CompilationUnit::class.java)
            .orElseThrow { IllegalStateException("Compilation unit not found for class: " + getName()) }

    fun getMethod(name: String): JavaMethodNode? = clazz.methods.filter { it.nameAsString == name }
            .map { JavaMethodNode(it) }
            .firstOrNull()

    fun addMethod(name: String): JavaMethodNode = JavaMethodNode(clazz.addMethod(name))

    fun getOrAddMethod(name: String): JavaMethodNode = getMethod(name) ?: addMethod(name)

    fun hasMethod(name: String): Boolean = clazz.methods.any { it.nameAsString == name }

    fun getMethods(): Stream<JavaMethodNode> = clazz.methods.asSequence().map { JavaMethodNode(it) }.asStream()

    fun getConstructors(): Stream<JavaConstructorNode> = clazz.constructors.asSequence().map { JavaConstructorNode(it) }.asStream()

    /**
     * Adds new modifiers to the class
     */
    fun withModifiers(vararg modifiers: Modifier.Keyword): JavaClassNode {
        val oldModifiers = clazz.modifiers.map { it.keyword } + modifiers
        clazz.setModifiers(*oldModifiers.distinct().toTypedArray())
        return this
    }

    /**
     * Sets new modifiers to the class
     */
    fun setModifiers(vararg modifiers: Modifier.Keyword): JavaClassNode {
        clazz.setModifiers(*modifiers)
        return this
    }

    fun addAnnotation(annotationClass: Class<out Annotation>): JavaClassNode {
        clazz.addAnnotation(annotationClass)
        return this
    }

    fun addAnnotation(annotationExp: String): JavaClassNode {
        clazz.addAnnotation(annotationExp)
        return this
    }

    fun removeAnnotation(annotationClass: Class<out Annotation>): JavaClassNode {
        clazz.annotations.removeIf { it.nameAsString == annotationClass.simpleName }
        return this
    }

    fun removeAnnotation(annotationName: String): JavaClassNode {
        clazz.annotations.removeIf { it.nameAsString == annotationName }
        return this
    }

    fun addImport(importClass: Class<*>): JavaClassNode {
        getParent().addImport(importClass)
        return this
    }

    fun removeImport(importClass: Class<*>): JavaClassNode {
        getParent().imports.removeIf { it.nameAsString == importClass.name }
        return this
    }

    fun addImport(importClass: String): JavaClassNode {
        getParent().addImport(importClass)
        return this
    }

    fun setTypeParameters(vararg params: String): JavaClassNode {
        clazz.setTypeParameters(NodeList(params.map { TypeParameter(it) }))
        return this
    }

    fun extendClass(className: String): JavaClassNode {
        clazz.addExtendedType(className)
        return this
    }

    fun implementInterface(name: String): JavaClassNode {
        clazz.addImplementedType(name)
        return this
    }

    fun implementInterface(interfaceClass: Class<*>): JavaClassNode {
        clazz.addImplementedType(interfaceClass)
        return this
    }

    fun addConstructor(): JavaConstructorNode = JavaConstructorNode(clazz.addConstructor())

    fun isInterface(): Boolean = clazz.isInterface

    fun setInterface(isInterface: Boolean): JavaClassNode {
        clazz.isInterface = isInterface
        return this
    }

    fun addField(typeName: String, name: String): JavaFieldNode = JavaFieldNode(clazz.addField(typeName, name, Modifier.Keyword.PRIVATE))
}
