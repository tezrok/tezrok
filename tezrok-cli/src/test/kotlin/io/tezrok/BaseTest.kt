package io.tezrok

import io.tezrok.api.node.BaseFileNode
import io.tezrok.api.node.DirectoryNode
import io.tezrok.api.node.FileNode
import io.tezrok.util.JsonUtil
import io.tezrok.util.resourceAsString
import org.junit.jupiter.api.Assertions.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Base class for all tests
 */
internal abstract class BaseTest {
    protected fun getFixedClock(timeStr: String = "2023-03-19T14:32:54.00Z"): Clock =
        Clock.fixed(Instant.parse(timeStr), ZoneId.systemDefault())

    /**
     * Asserts that two JSONs are equal (semantically, keys order is ignored)
     */
    protected fun assertJsonEquals(expected: String, actual: String) {
        if (!JsonUtil.compareJsons(expected, actual)) {
            assertEquals(expected, actual)
        }
    }

    /**
     * Asserts that string from a resource is equal to a string
     */
    protected fun assertResourceEquals(expectedResourcePath: String, actual: String) {
        assertEquals(expectedResourcePath.resourceAsString(), actual)
    }

    protected fun assertFile(node: BaseFileNode, name: String): FileNode {
        assertEquals(name, node.getName())
        assertTrue(node.isFile()) { "Node should be file: " + node.getName() }
        assertEquals(node.isEmpty(), node.getSize() == 0L) { "Node isEmpty and getSize should be synced: " + node.getName() }
        return node as FileNode
    }

    protected fun assertDirectory(node: BaseFileNode, name: String, childrenSize: Int): DirectoryNode {
        assertEquals(name, node.getName())
        assertTrue(node.isDirectory()) { "Node should be directory: " + node.getName() }
        assertEquals(childrenSize, node.getFilesSize()) { "Node should have $childrenSize children: " + node.getName() }
        assertInstanceOf(
            DirectoryNode::class.java,
            node
        ) { "Node should be instance of DirectoryNode: " + node.getName() }
        return node as DirectoryNode
    }
}
