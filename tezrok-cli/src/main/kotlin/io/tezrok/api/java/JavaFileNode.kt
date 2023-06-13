package io.tezrok.api.java

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.PackageDeclaration
import io.tezrok.api.node.FileNode
import io.tezrok.api.node.Node
import io.tezrok.util.getRootClass
import java.io.InputStream

/**
 * Node that represents a Java file
 */
open class JavaFileNode(name: String, parent: Node? = null) : FileNode(if (name.contains('.')) name else "$name.java", parent) {
    private var compilationUnit: CompilationUnit = CompilationUnit()

    // TODO: support package declaration

    init {
        // add default public class
        compilationUnit.addClass(name.substringBeforeLast("."))
        updatePackage()
    }

    fun getParentDirectory(): JavaDirectoryNode? = getParent() as? JavaDirectoryNode

    /**
     * Returns root class/interface of the file
     */
    fun getRootClass(): JavaClassNode = JavaClassNode(compilationUnit.getRootClass())

    fun addClass(name: String): JavaClassNode = JavaClassNode(compilationUnit.addClass(name))

    override fun getInputStream(): InputStream = compilationUnit.toString().byteInputStream()

    /**
     * Returns the path of the file relative to the Java root
     */
    fun getPackagePath(): String {
        return getParentDirectory()?.getPathTo(getJavaRoot()) ?: "/"
    }

    fun getJavaRoot(): JavaRootNode? = getFirstAncestor { it is JavaRootNode } as? JavaRootNode

    override fun setContent(content: ByteArray) {
        val content = String(content, Charsets.UTF_8)
        val parsed = JavaParser().parse(content)

        if (parsed.isSuccessful) {
            compilationUnit = parsed.result.get()
            updatePackage()
        } else {
            log.error("Failed to parse Java content")
            parsed.problems.forEach {
                log.error("Problem: $it")
            }
            log.error("Failed content:\n{}", content)
            error("Failed to parse Java content: ${getPath()}")
        }
    }

    private fun updatePackage() {
        val packagePath = getPackagePath().substring(1).replace("/", ".")
        if (packagePath.isNotBlank())
            compilationUnit.setPackageDeclaration(packagePath)
        else
            compilationUnit.setPackageDeclaration(null as PackageDeclaration?)
    }
}
