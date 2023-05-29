package io.tezrok.api.java

import io.tezrok.api.node.BaseNode

/**
 * Represents directory: src/main/java
 */
open class JavaRootNode(parent: BaseNode?) : JavaDirectoryNode("java", parent) {
    // class with main method
    var applicationClass: JavaClassNode? = null

    // root directory of application package
    var applicationPackageRoot: JavaDirectoryNode? = null

    override fun isJavaRoot(): Boolean = true
}
