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
open class JavaFileNode(name: String, parent: Node? = null) : FileNode(if (name.contains('.')) name else "$name.java", parent), JavaClassOrPackage {
    private var compilationUnit: CompilationUnit = CompilationUnit()

    // TODO: support package declaration

    init {
        // add default public class
        compilationUnit.addClass(name.substringBeforeLast("."))
        updatePackage()
    }

    /**
     * Returns root class/interface of the file
     */
    fun getRootClass(): JavaClassNode = JavaClassNode(compilationUnit.getRootClass(), this)

    fun addClass(name: String): JavaClassNode = JavaClassNode(compilationUnit.addClass(name), this)

    override fun getInputStream(): InputStream = compilationUnit.toString().byteInputStream()

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
        val packagePath = getParentPackage()
        if (packagePath.isNotBlank())
            compilationUnit.setPackageDeclaration(packagePath)
        else
            compilationUnit.setPackageDeclaration(null as PackageDeclaration?)
    }
}
