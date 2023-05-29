package io.tezrok.api.java

interface JavaDirectory {
    fun isJavaRoot(): Boolean = false

    fun getJavaFiles(): List<JavaFileNode>

    fun getJavaFile(name: String): JavaFileNode?

    fun addJavaFile(name: String): JavaFileNode

    fun getOrAddJavaFile(name: String): JavaFileNode = getJavaFile(name) ?: addJavaFile(name)

    fun getClass(name: String): JavaClassNode? = getJavaFile(name)?.getRootClass()

    fun addClass(name: String): JavaClassNode = addJavaFile(name).getRootClass()

    fun getOrAddClass(name: String): JavaClassNode = getClass(name) ?: addClass(name)

    fun getJavaDirectories(): List<JavaDirectoryNode>

    fun getJavaDirectory(name: String): JavaDirectoryNode?

    fun addJavaDirectory(name: String): JavaDirectoryNode

    fun getOrAddJavaDirectory(name: String): JavaDirectoryNode = getJavaDirectory(name) ?: addJavaDirectory(name)

    /**
     * Creates all directories (if not exist) in the path and returns the last directory
     */
    fun makeDirectories(path: String): JavaDirectoryNode
}
