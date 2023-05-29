package io.tezrok.api.java

import io.tezrok.api.node.BaseFileNode
import io.tezrok.api.node.BaseNode
import io.tezrok.api.node.DirectoryNode
import java.util.*

/**
 * Represents any directory with java files
 */
open class JavaDirectoryNode(name: String, parent: BaseNode?) : DirectoryNode(name, parent), JavaDirectory {
    private val javaFiles = mutableListOf<JavaFileNode>()
    private val javaDirs = mutableListOf<JavaDirectoryNode>()

    override fun getFiles(): List<BaseFileNode> = Collections.unmodifiableList(javaFiles + javaDirs)

    override fun getJavaFiles(): List<JavaFileNode> = Collections.unmodifiableList(javaFiles.toList())

    override fun getJavaFile(name: String): JavaFileNode? = javaFiles.find { it.getName() == name }

    override fun addJavaFile(name: String): JavaFileNode {
        val fileNode = JavaFileNode(name, this)
        javaFiles.add(fileNode)
        return fileNode
    }

    override fun getJavaDirectories(): List<JavaDirectoryNode> = Collections.unmodifiableList(javaDirs.toList())

    override fun getJavaDirectory(name: String): JavaDirectoryNode? = javaDirs.find { it.getName() == name }

    override fun addJavaDirectory(name: String): JavaDirectoryNode {
        val directoryNode = JavaDirectoryNode(name, this)
        javaDirs.add(directoryNode)
        return directoryNode
    }

    override fun makeDirectories(path: String): JavaDirectoryNode {
        val parts = path.split("/").filter { it.isNotBlank() }
        var current = this

        parts.forEach { name ->
            current = current.getOrAddJavaDirectory(name)
        }

        return current
    }
}
