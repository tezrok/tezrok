package com.tezrok.api.tree

import com.tezrok.BaseTest
import com.tezrok.api.tree.repo.file.FileNodeElem
import com.tezrok.api.tree.repo.file.FileNodeRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class NodeManagerTest : BaseTest() {

    @Test
    fun getRootNodeWhenFileNotExists() {
        val repository = FileNodeRepository(file)
        val manager = NodeManagerImpl(repository)
        val root = manager.getRootNode()

        assertEquals(FileNodeElem.ROOT_NAME, root.getName())
        assertEquals(1000L, root.getId())
        assertEquals(0, root.getChildrenSize())
        assertEmpty(root.getChildren().toList())
        assertEquals(NodeType.Root, root.getType())
        assertEquals("/", root.getPath())
        assertNull(root.getParent())

        val properties = root.getProperties()
        assertEquals(3, properties.getPropertiesNames().size)
        assertFalse(file.exists()) { "File must not exist: $file" }
    }

    @Test
    fun saveSingleRootTest() {
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
    }

    @Test
    fun testAddSingleNode() {
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
        assertEquals("/test", node.getPath())
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
    }

    @Test
    fun testAddSingleNodeWithChild() {
        val repository = FileNodeRepository(file)
        val manager = NodeManagerImpl(repository)
        val root = manager.getRootNode()
        val child = root.add("test", NodeType.Directory)
        val node = child.add("child", NodeType.Item)

        assertEquals(1, child.getChildrenSize())
        assertEquals(1, root.getChildrenSize())
        assertEquals(child, node.getParent())
        assertEquals(root, child.getParent())

        assertEquals("child", node.getName())
        assertEquals(1002L, node.getId())
        assertEquals(0, node.getChildrenSize())
        assertEmpty(node.getChildren().toList())
        assertEquals(NodeType.Item, node.getType())
        assertEquals("/test/child", node.getRef().getPath())
        assertEquals("/test/child", node.getPath())
        assertEquals(node, node.getRef().getNode())
        assertEquals(node.getRef(), node.getRef())

        manager.save()

        assertTrue(file.exists())

        val repository2 = FileNodeRepository(file)
        val manager2 = NodeManagerImpl(repository2)
        val root2 = manager2.getRootNode()

        assertEquals(root, root2)
        assertEquals(ROOT_WITH_SINGLE_CHILD_AND_CHILD, file.readText())
    }

    @Test
    fun testFindNodeByPath() {
        val repository = FileNodeRepository(file)
        val manager = NodeManagerImpl(repository)
        val root = manager.getRootNode()
        val child = root.add("child", NodeType.Directory)
        val node = child.add("node", NodeType.Item)

        assertEquals(root, manager.findNodeByPath("/"))
        assertEquals(child, manager.findNodeByPath("/child"))
        assertEquals(child, manager.findNodeByPath("/child/"))
        assertEquals(node, manager.findNodeByPath("/child/node"))
        assertEquals(node, manager.findNodeByPath("/child/node/"))
        assertNull(manager.findNodeByPath("/child/node//"))
        assertNull(manager.findNodeByPath("/child/node/node"))
        assertNull(manager.findNodeByPath("/child/node/node/"))
        assertNull(manager.findNodeByPath("//"))
        assertNull(manager.findNodeByPath("/child//node"))
        assertNull(manager.findNodeByPath(""))
        assertNull(manager.findNodeByPath("  "))
        assertNull(manager.findNodeByPath("child"))
        assertNull(manager.findNodeByPath("child/node"))

        assertEquals(root, manager.findNodeByPath(root.getPath()))
        assertEquals(child, manager.findNodeByPath(child.getPath()))
        assertEquals(node, manager.findNodeByPath(node.getPath()))
    }

    @Test
    fun testFindChild() {
        val repository = FileNodeRepository(file)
        val manager = NodeManagerImpl(repository)
        val root = manager.getRootNode()
        val dir = root.add("Dir", NodeType.Directory)
        val item = dir.add("Item", NodeType.Item)

        assertEquals(root, root.findNodeByPath("/"))
        assertEquals(dir, root.findNodeByPath("/Dir"))
        assertEquals(dir, root.findNodeByPath("/Dir/"))
        assertEquals(item, dir.findNodeByPath("/Item"))
        assertEquals(item, dir.findNodeByPath("/Item/"))
        assertEquals(item, root.findNodeByPath("/Dir/Item"))
        assertEquals(item, root.findNodeByPath("/Dir/Item/"))
        assertNull(root.findNodeByPath("Item"))
        assertNull(root.findNodeByPath("/Item"))
        assertNull(dir.findNodeByPath("Dir"))
        assertNull(root.findNodeByPath("//"))
        assertNull(root.findNodeByPath(""))
        assertNull(dir.findNodeByPath(""))
        assertNull(item.findNodeByPath(""))
        assertNull(dir.findNodeByPath("/Dir/Item"))
    }

    private companion object {
        const val SINGLE_ROOT = """{
  "id" : 1000,
  "props" : {
    "name" : "Root",
    "type" : "Root"
  }
}"""

        const val ROOT_WITH_SINGLE_CHILD = """{
  "id" : 1000,
  "props" : {
    "name" : "Root",
    "type" : "Root"
  },
  "items" : [ {
    "id" : 1001,
    "props" : {
      "name" : "test",
      "type" : "Directory"
    }
  } ]
}"""

        const val ROOT_WITH_SINGLE_CHILD_AND_CHILD = """{
  "id" : 1000,
  "props" : {
    "name" : "Root",
    "type" : "Root"
  },
  "items" : [ {
    "id" : 1001,
    "props" : {
      "name" : "test",
      "type" : "Directory"
    },
    "items" : [ {
      "id" : 1002,
      "props" : {
        "name" : "child",
        "type" : "Item"
      }
    } ]
  } ]
}"""
    }
}
