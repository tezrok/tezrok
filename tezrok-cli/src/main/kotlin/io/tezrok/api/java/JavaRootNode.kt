package io.tezrok.api.java

import io.tezrok.api.node.BaseFileNode
import io.tezrok.api.node.BaseNode
import io.tezrok.api.node.DirectoryNode
import java.util.*

/**
 * Represents directory: src/main/java
 */
open class JavaRootNode(parent: BaseNode?) : DirectoryNode("java", parent), JavaDirectory {
    private val javaFiles = mutableListOf<JavaFileNode>()

    override fun getFiles(): List<BaseFileNode> = getJavaFiles()

    override fun getJavaFiles(): List<JavaFileNode> = Collections.unmodifiableList(javaFiles.toList())

    override fun getDirectories(): List<JavaDirectory> {
        TODO("Not yet implemented")
    }

    override fun getJavaFile(name: String): JavaFileNode? = javaFiles.find { it.getName() == name }

    override fun addJavaFile(name: String): JavaFileNode {
        val fileNode = JavaFileNode(name, this)
        javaFiles.add(fileNode)
        return fileNode
    }
}
