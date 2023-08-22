package io.tezrok.util

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration

/**
 * Get main class of the compilation unit
 */
fun CompilationUnit.getRootClass(): ClassOrInterfaceDeclaration =
    this.types.filterIsInstance<ClassOrInterfaceDeclaration>().firstOrNull()
        ?: throw IllegalStateException("Class not found in compilation unit")
