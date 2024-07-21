package io.tezrok.api.java

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.comments.JavadocComment
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.TypeParameter
import io.tezrok.util.addImportsByType
import java.util.stream.Stream
import kotlin.streams.asStream

/**
 * Node that represents a Java class or interface
 */
open class JavaClassNode(private val clazz: ClassOrInterfaceDeclaration, private val javaFile: JavaFileNode) {
    fun getName(): String = clazz.nameAsString

    fun getFullName(): String = clazz.fullyQualifiedName.orElseGet { getName() }

    fun getParent(): CompilationUnit = clazz.findAncestor(CompilationUnit::class.java)
        .orElseThrow { IllegalStateException("Compilation unit not found for class: " + getName()) }

    fun getMethod(name: String): JavaMethodNode? = clazz.methods.filter { it.nameAsString == name }
        .map { JavaMethodNode(it, this) }
        .firstOrNull()

    fun addMethod(name: String): JavaMethodNode = JavaMethodNode(clazz.addMethod(name), this)

    fun getOrAddMethod(name: String): JavaMethodNode = getMethod(name) ?: addMethod(name)

    fun hasMethod(name: String): Boolean = clazz.methods.any { it.nameAsString == name }

    fun getMethods(): Stream<JavaMethodNode> = clazz.methods.asSequence().map { JavaMethodNode(it, this) }.asStream()

    fun getConstructors(): Stream<JavaConstructorNode> = clazz.constructors.asSequence().map { JavaConstructorNode(it, this) }.asStream()

    fun getJavaFile(): JavaFileNode = javaFile

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

    fun addAnnotation(annotationClass: Class<out Annotation>, vararg fields: Pair<String, Expression>): JavaClassNode {
        return addAnnotation(annotationClass, fields.toMap())
    }

    fun addAnnotation(annotationClass: Class<out Annotation>, fields: Map<String, Expression>): JavaClassNode {
        if (fields.isNotEmpty()) {
            addImport(annotationClass)
            val annotation = clazz.addAndGetAnnotation(annotationClass.simpleName)
            annotation.setPairs(NodeList(fields.map { MemberValuePair(it.key, it.value) }))
        } else {
            clazz.addAnnotation(annotationClass)
        }
        return this
    }

    fun addAnnotation(annotationClass: Class<out Annotation>, expression: Expression): JavaClassNode {
        addImport(annotationClass)
        clazz.addAnnotation(SingleMemberAnnotationExpr(Name(annotationClass.simpleName), expression))
        return this
    }

    fun addAnnotation(annotationClass: Class<out Annotation>, expression: String): JavaClassNode {
        return addAnnotation(annotationClass, StringLiteralExpr(expression))
    }

    fun addAnnotation(annotationExp: String): JavaClassNode {
        clazz.addAnnotation(annotationExp)
        return this
    }

    fun addAnnotation(annotationExpr: NormalAnnotationExpr): JavaClassNode {
        clazz.addAnnotation(annotationExpr)
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

    /**
     * Find class by simple name and add import into this file.
     */
    fun addImportBySimpleName(simpleTypeName: String): JavaClassNode {
        try {
            // first check this type
            if (getName() == simpleTypeName) {
                addImport(javaFile.getPackageWithSuffix() + simpleTypeName)
                return this
            }
            // search in all classes by simple name
            addImportBySimpleName(javaFile.getJavaRoot()!!, simpleTypeName)
            return this
        } catch (e: Exception) {
            throw IllegalStateException("Failed to add import for simple name: $simpleTypeName", e)
        }
    }

    private fun addImportBySimpleName(javaDir: JavaDirectoryNode, simpleTypeName: String): Boolean {
        val javaFileName = "$simpleTypeName.java"
        javaDir.getJavaFiles().forEach { javaFile ->
            if (javaFile.getName() == javaFileName) {
                addImport(javaFile.getParentPackageWithSuffix() + simpleTypeName)
                return true
            }
        }

        javaDir.getJavaDirectories().forEach {
            if (addImportBySimpleName(it, simpleTypeName)) {
                return true
            }
        }

        return false
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

    fun addConstructor(): JavaConstructorNode = JavaConstructorNode(clazz.addConstructor(), this)

    fun isInterface(): Boolean = clazz.isInterface

    /**
     * Init field in constructor.
     */
    fun initInConstructor(field: JavaFieldNode): JavaClassNode {
        val constructor = getConstructors().findFirst().orElseGet(this::addConstructor)
            .withModifiers(Modifier.Keyword.PUBLIC)
            .addParameter(field.getType(), field.getName())

        val name = field.getName()
        val thisFieldExp = FieldAccessExpr(ThisExpr(), name)
        val paramName = NameExpr(name)
        val statement = ExpressionStmt(
            AssignExpr(
                thisFieldExp,
                paramName,
                AssignExpr.Operator.ASSIGN
            )
        )
        val allStatements = ArrayList<Statement>(constructor.getBody().childNodes.map { it as Statement })
        constructor.setBody(BlockStmt(NodeList(allStatements + statement)))

        return this
    }

    fun setInterface(isInterface: Boolean): JavaClassNode {
        clazz.isInterface = isInterface
        return this
    }

    fun addField(typeName: String, name: String, initInConstructor: Boolean = false): JavaFieldNode {
        addImportsByType(typeName)
        addImportBySimpleName(typeName)
        val field = JavaFieldNode(clazz.addField(typeName, name, Modifier.Keyword.PRIVATE))
        if (initInConstructor) {
            initInConstructor(field)
        }
        return field
    }

    /**
     * Adds a field with a getter and a setter.
     */
    fun addProperty(typeName: String, name: String): JavaFieldNode {
        val field = addField(typeName, name)
        addGetter(field)
        addSetter(field)

        return field
    }

    /***
     * Add setter for field.
     */
    fun addSetter(field: JavaFieldNode): JavaMethodNode {
        val name = field.getName()
        val thisFieldExp = FieldAccessExpr(ThisExpr(), name)
        val paramName = NameExpr(name)
        val capitalizedName = name.capitalize()
        return addMethod("set$capitalizedName")
            .withModifiers(Modifier.Keyword.PUBLIC)
            .addParameter(field.getType(), name)
            .setBody(
                BlockStmt(
                    NodeList(
                        ExpressionStmt(
                            AssignExpr(
                                thisFieldExp,
                                paramName,
                                AssignExpr.Operator.ASSIGN
                            )
                        )
                    )
                )
            )
    }

    /**
     * Add getter for field.
     */
    fun addGetter(field: JavaFieldNode): JavaMethodNode {
        val name = field.getName()
        val capitalizedName = name.capitalize()
        return addMethod("get$capitalizedName")
            .withModifiers(Modifier.Keyword.PUBLIC)
            .setReturnType(field.getType())
            .setBody(ReturnStmt("this.$name"))
    }

    /**
     * Adds comment to the class.
     */
    fun setJavadocComment(comment: String): JavaClassNode {
        clazz.setComment(JavadocComment(comment))
        return this
    }

    fun addInnerClass(className: String): JavaClassNode {
        val innerClass = ClassOrInterfaceDeclaration().setName("InnerContext")
        // Create a new inner class
        clazz.addMember(innerClass)
        return JavaClassNode(innerClass, javaFile)
    }

    override fun toString(): String = clazz.toString()
}
