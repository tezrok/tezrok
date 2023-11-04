package io.tezrok.api.java

import io.tezrok.api.node.Node

/**
 * Common interface for java classes and packages
 */
interface JavaClassOrPackage : Node {
    fun getParentDirectory(): JavaDirectoryNode? = getParent() as? JavaDirectoryNode

    /**
     * Returns the path of the class/package relative to the Java root
     */
    fun getPackagePath(): String = getPathTo(getJavaRoot())

    /**
     * Returns the package of class/package itself
     */
    fun getPackage(): String = getPackagePath().substring(1).replace("/", ".")

    /**
     * Returns the path of the parent relative to the Java root
     */
    fun getParentPackagePath(): String = getParentDirectory()?.getPathTo(getJavaRoot()) ?: "/"

    /**
     * Returns the package whe file is located in
     */
    fun getParentPackage(): String = getParentPackagePath().substring(1).replace("/", ".")

    fun getJavaRoot(): JavaRootNode? = getFirstAncestor { it is JavaRootNode } as? JavaRootNode
}
