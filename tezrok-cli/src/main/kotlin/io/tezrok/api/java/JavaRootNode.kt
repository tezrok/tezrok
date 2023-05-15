package io.tezrok.api.java

import io.tezrok.api.node.BaseNode

/**
 * Represents directory: src/main/java
 */
open class JavaRootNode(parent: BaseNode?) : JavaDirectoryNode("java", parent) {
    override fun isJavaRoot(): Boolean = true
}
