package io.tezrok.api.java

import com.github.javaparser.ast.CompilationUnit
import io.tezrok.api.node.FileNode
import io.tezrok.api.node.Node
import io.tezrok.util.getRootClass
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
        compilationUnit.setPackageDeclaration(getPackagePath().replace("/", "."))
    }

    fun getParentDirectory(): JavaDirectoryNode = getParent() as? JavaDirectoryNode
        ?: throw IllegalStateException("Java file must be a child of Java directory")

    /**
     * Returns root class/interface of the file
     */
    fun getRootClass(): JavaClassNode = JavaClassNode(compilationUnit.getRootClass())

    fun addClass(name: String): JavaClassNode = JavaClassNode(compilationUnit.addClass(name))

    override fun getOutputStream(): OutputStream {
        throw UnsupportedOperationException("Java file can be edited only via object model")
    }

    override fun getInputStream(): InputStream = compilationUnit.toString().byteInputStream()

    /**
     * Returns the path of the file relative to the Java root
     *
     * Example: "src/main/java/com/example/Foo.java" -> "com/example"
     */
    fun getPackagePath(): String  {
        // remove leading slash
        return getParentDirectory().getPathTo(getJavaRoot()).substring(1)
    }

    fun getJavaRoot(): JavaRootNode {
        var parent = getParent()
        while (parent != null) {
            if (parent is JavaRootNode) {
                return parent
            }
            parent = parent.getParent()
        }
        throw IllegalStateException("Java file must be a child of Java root")
    }
}
