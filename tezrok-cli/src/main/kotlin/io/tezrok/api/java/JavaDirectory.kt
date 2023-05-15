package io.tezrok.api.java

interface JavaDirectory {
    fun isJavaRoot(): Boolean = false

    fun getJavaFiles(): List<JavaFileNode>

    fun getJavaFile(name: String): JavaFileNode?

    fun addJavaFile(name: String): JavaFileNode

    fun getOrCreateJavaFile(name: String): JavaFileNode = getJavaFile(name) ?: addJavaFile(name)

    fun getJavaDirectories(): List<JavaDirectoryNode>

    fun getJavaDirectory(name: String): JavaDirectoryNode?

    fun addJavaDirectory(name: String): JavaDirectoryNode

    fun getOrCreateJavaDirectory(name: String): JavaDirectoryNode = getJavaDirectory(name) ?: addJavaDirectory(name)

    /**
     * Creates all directories in the path and returns the last directory
     */
    fun makeDirectories(path: String): JavaDirectoryNode
}
