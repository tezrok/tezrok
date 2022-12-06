package com.tezrok.api.tree

import com.tezrok.BaseTest
import com.tezrok.api.tree.repo.file.FileNodeElem
import com.tezrok.api.tree.repo.file.FileNodeRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

class NodeManagerTest : BaseTest() {

    @Test
    fun getRootNodeWhenFileNotExists() {
        val file = File(tempDir, UUID.randomUUID().toString())
        val repository = FileNodeRepository(file)
        val manager = NodeManagerImpl(repository)
        val root = manager.getRootNode()

        assertEquals(FileNodeElem.ROOT_NAME, root.getName())
        assertEquals(1000L, root.getId())
        assertEquals(0, root.getChildrenSize())
        assertEmpty(root.getChildren().toList())

        val properties = root.getProperties()
        assertEquals(3, properties.getPropertiesNames().size)
        assertFalse(file.exists()) { "File must not exist: $file" }
    }

    @Test
    fun saveSingleRootTest() {
        val file = File(tempDir, UUID.randomUUID().toString())

        try {
            val repository = FileNodeRepository(file)
            val manager = NodeManagerImpl(repository)
            val root = manager.getRootNode()
            manager.save()

            assertTrue(file.exists())

            val repository2 = FileNodeRepository(file)
            val manager2 = NodeManagerImpl(repository2)
            val root2 = manager2.getRootNode()

            assertEquals(root, root2)
            assertEquals(SINGLE_ROOT, file.readText())
        } finally {
            file.delete()
        }
    }

    @Test
    fun testAddSingleNode() {
        val file = File(tempDir, UUID.randomUUID().toString())

        try {
            val repository = FileNodeRepository(file)
            val manager = NodeManagerImpl(repository)
            val root = manager.getRootNode()
            val node = root.add("test", NodeType.Directory)

            assertEquals("test", node.getName())
            assertEquals(1001L, node.getId())
            assertEquals(0, node.getChildrenSize())
            assertEmpty(node.getChildren().toList())
            assertEquals(root, node.getParent())
            assertEquals(NodeType.Directory, node.getType())
            assertEquals("/test", node.getRef().getPath())
            assertEquals(node, node.getRef().getNode())
            assertEquals(node.getRef(), node.getRef())

            manager.save()

            assertTrue(file.exists())

            val repository2 = FileNodeRepository(file)
            val manager2 = NodeManagerImpl(repository2)
            val root2 = manager2.getRootNode()

            assertEquals(root, root2)
            assertEquals(ROOT_WITH_SINGLE_CHILD, file.readText())
        } finally {
            file.delete()
        }
    }


    private companion object {
        const val SINGLE_ROOT = """{
  "id" : 1000,
  "properties" : {
    "system.type" : "Root",
    "system.name" : "Root"
  }
}"""

        const val ROOT_WITH_SINGLE_CHILD = """{
  "id" : 1000,
  "properties" : {
    "system.type" : "Root",
    "system.name" : "Root"
  },
  "items" : [ {
    "id" : 1001,
    "properties" : {
      "system.type" : "Directory",
      "system.name" : "test"
    }
  } ]
}"""
    }
}
