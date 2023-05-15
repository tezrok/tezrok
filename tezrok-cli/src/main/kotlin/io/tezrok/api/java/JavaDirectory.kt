package io.tezrok.api.java

interface JavaDirectory {
    fun getJavaFiles(): List<JavaFileNode>

    fun getDirectories(): List<JavaDirectory>

    fun getJavaFile(name: String): JavaFileNode?

    fun addJavaFile(name: String): JavaFileNode

    fun getOrCreateJavaFile(name: String): JavaFileNode = getJavaFile(name) ?: addJavaFile(name)
}
