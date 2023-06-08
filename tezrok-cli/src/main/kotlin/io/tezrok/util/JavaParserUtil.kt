package io.tezrok.util

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration

fun CompilationUnit.getRootClass(): ClassOrInterfaceDeclaration = this.types.first { it.isPublic } as ClassOrInterfaceDeclaration
