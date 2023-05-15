package io.tezrok.api.java

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import io.tezrok.api.node.FileNode
import io.tezrok.api.node.Node
import java.io.InputStream
import java.io.OutputStream

/**
 * Node that represents a Java file
 */
open class JavaFileNode(name: String, parent: Node? = null) : FileNode("$name.java", parent) {
    val compilationUnit: CompilationUnit = CompilationUnit()

    // TODO: support package declaration

    init {
        // add default public class
        compilationUnit.addClass(name)
    }

    /**
     * Returns root class/interface of the file
     */
    fun getRootClass(): JavaClassNode =
        JavaClassNode(compilationUnit.types.first { it.isPublic } as ClassOrInterfaceDeclaration)

    fun addClass(name: String): JavaClassNode = JavaClassNode(compilationUnit.addClass(name))

    override fun getOutputStream(): OutputStream {
        throw UnsupportedOperationException("Java file can be edited only via object model")
    }

    override fun getInputStream(): InputStream = compilationUnit.toString().byteInputStream()
}
