package io.tezrok

import io.tezrok.core.common.DirectoryNode
import io.tezrok.core.common.FileNode
import io.tezrok.util.JsonUtil
import io.tezrok.util.resourceAsString
import org.junit.jupiter.api.Assertions

/**
 * Base class for all tests
 */
internal abstract class BaseTest {

    /**
     * Asserts that two JSONs are equal (semantically, keys order is ignored)
     */
    protected fun assertJsonEquals(expected: String, actual: String) {
        Assertions.assertTrue(JsonUtil.compareJsons(expected, actual)) {
            "JSONs are not equal. \nexpected: $expected, \nactual: $actual}"
        }
    }

    /**
     * Asserts that string from a resource is equal to a string
     */
    protected fun assertResourceEquals(expectedResourcePath: String, actual: String) {
        Assertions.assertEquals(expectedResourcePath.resourceAsString(), actual)
    }

    protected fun assertFile(node: FileNode, name: String): FileNode {
        Assertions.assertEquals(name, node.getName())
        Assertions.assertTrue(node.isFile()) { "Node should be file: " + node.getName() }
        Assertions.assertTrue(node.getFilesSize() == 0) { "Node should not have children: " + node.getName() }
        return node
    }

    protected fun assertDirectory(node: FileNode, name: String, childrenSize: Int): DirectoryNode {
        Assertions.assertEquals(name, node.getName())
        Assertions.assertTrue(node.isDirectory()) { "Node should be directory: " + node.getName() }
        Assertions.assertEquals(
            childrenSize,
            node.getFilesSize()
        ) { "Node should have $childrenSize children: " + node.getName() }
        Assertions.assertInstanceOf(
            DirectoryNode::class.java,
            node
        ) { "Node should be instance of DirectoryNode: " + node.getName() }

        return node as DirectoryNode
    }
}
