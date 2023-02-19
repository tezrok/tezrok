package com.tezrok.core.node

import com.tezrok.api.error.NodeAlreadyExistsException
import com.tezrok.api.node.NodeType
import com.tezrok.core.BaseTest
import com.tezrok.core.node.repo.file.FileNodeElem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class NodeManagerTest : BaseTest() {
    var manager: NodeManagerImpl? = null
    var operation: NodeOperation? = null

    @BeforeEach
    override fun setUp() {
        super.setUp()
        manager = nodeManagerFromFile(file)
        operation = manager!!.startOperation(AuthorType.User, "UserBar")
    }

    @AfterEach
    override fun tearDown() {
        operation!!.stop()
        manager = null
        super.tearDown()
    }

    @Test
    fun getRootNodeWhenFileNotExists() {
        val root = manager!!.getRootNode()

        assertEquals(FileNodeElem.ROOT_NAME, root.getName())
        assertEquals(1000L, root.getId())
        assertEquals(0, root.getChildrenSize())
        assertEmpty(root.getChildren().toList())
        assertEquals(NodeType.Root, root.getType())
        assertEquals("/", root.getPath())
        assertNull(root.getParent())

        val properties = root.getProperties()
        assertEquals(7, properties.getPropertiesNames().size)
        assertEquals(STD_PROPERTIES, properties.getPropertiesNames().map { it.name }.toSet())
        assertFalse(file.exists()) { "File must not exist: $file" }
    }

    @Test
    fun saveSingleRootTest() {
        val root = manager!!.getRootNode()
        manager!!.save()

        assertTrue(file.exists())

        val manager2 = nodeManagerFromFile(file)
        val root2 = manager2.getRootNode()

        assertEquals(root, root2)
        assertEquals(STD_PROPERTIES, root2.getProperties().getPropertiesNames().map { it.name }.toSet())
    }

    @Test
    fun testAddSingleNode() {
        val root = manager!!.getRootNode()
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

        manager!!.save()

        assertTrue(file.exists())

        val manager2 = nodeManagerFromFile(file)
        val root2 = manager2.getRootNode()

        assertEquals(root, root2)
        assertEquals(STD_PROPERTIES, root2.getProperties().getPropertiesNames().map { it.name }.toSet())
    }

    @Test
    fun testAddSingleNodeWithChild() {
        val root = manager!!.getRootNode()
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

        manager!!.save()

        assertTrue(file.exists())

        val manager2 = nodeManagerFromFile(file)
        val root2 = manager2.getRootNode()

        assertEquals(root, root2)
        assertEquals(STD_PROPERTIES, root2.getProperties().getPropertiesNames().map { it.name }.toSet())
    }

    @Test
    fun testFindNodeByPath() {
        val manager = this.manager!!
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
        val root = manager!!.getRootNode()
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

    @Test
    fun testRemoveChildren() {
        val root = manager!!.getRootNode()
        val child = root.add("child", NodeType.Directory)
        val node = child.add("node", NodeType.Item)

        assertFalse(root.remove(listOf(node)))
        assertFalse(node.remove(listOf(child)))
        assertFalse(node.remove(listOf(node)))
        assertFalse(child.remove(listOf(root)))
        assertEquals(1, root.getChildrenSize())

        assertEquals(1, child.getChildrenSize())
        assertTrue(child.remove(listOf(node)))
        assertEquals(0, child.getChildrenSize())
        assertFalse(child.remove(listOf(node)))

        assertEquals(1, root.getChildrenSize())
        assertTrue(root.remove(listOf(child)))
        assertEquals(0, root.getChildrenSize())
        assertFalse(root.remove(listOf(child)))
    }

    @Test
    fun testSaveAfterRemoveChildren() {
        val root = manager!!.getRootNode()
        val child = root.add("child", NodeType.Directory)
        val node = child.add("node", NodeType.Item)

        assertTrue(child.remove(listOf(node)))
        assertTrue(root.remove(listOf(child)))

        manager!!.save()

        val manager2 = nodeManagerFromFile(file)
        val root2 = manager2.getRootNode()

        assertEquals(root, root2)
    }

    @Test
    fun testRemoveChildrenAfterSave() {
        val root = manager!!.getRootNode()
        val child = root.add("child", NodeType.Directory)
        val node = child.add("node", NodeType.Item)

        manager!!.save()

        val manager2 = nodeManagerFromFile(file)
        val operation = manager2.startOperation(AuthorType.User, "UserBar")
        val root2 = manager2.getRootNode()
        val child2 = root2.findNodeByPath("/child")!!
        val node2 = child2.findNodeByPath("/node")!!

        assertEquals(1, root2.getChildrenSize())
        assertEquals(1, child2.getChildrenSize())
        assertEquals(0, node2.getChildrenSize())
        assertTrue(child2.remove(listOf(node2)))
        assertTrue(root2.remove(listOf(child2)))
        assertEquals(0, root2.getChildrenSize())
        operation.stop()

        manager2.save()

        val manager3 = nodeManagerFromFile(file)
        val root3 = manager3.getRootNode()

        assertEquals(root2, root3)
    }

    @Test
    fun testRemoveReturnTrueIfAnyChildRemoved() {
        val root = manager!!.getRootNode()
        val item = root.add("Item", NodeType.Item)
        val child = root.add("child", NodeType.Directory)
        val node1 = child.add("node1", NodeType.Item)
        val node2 = child.add("node2", NodeType.Item)

        assertEquals(2, root.getChildrenSize())
        assertEquals(2, child.getChildrenSize())

        assertTrue(child.remove(listOf(node1, item)))
        assertEquals(1, child.getChildrenSize())
        assertFalse(child.remove(listOf(node1, item)))
        assertEquals(1, child.getChildrenSize())
        assertEquals(2, root.getChildrenSize())
        assertEquals(node2, child.getChildren().findFirst().get())
    }

    @Test
    fun testNodeRef() {
        val root = manager!!.getRootNode()
        val child = root.add("child3", NodeType.Directory)

        val ref = child.getRef()
        assertEquals("/child3", ref.getPath())
        assertTrue(ref.exists(), "Ref should exist")

        val unkRef = ref.getChild("unknown")
        assertEquals("/child3/unknown", unkRef.getPath())
        assertFalse(unkRef.exists(), "Ref should not exist")
        assertNull(unkRef.getNode(), "Ref should not exist")
    }

    @Test
    fun testAddNodeWithDuplicateNameMustFail() {
        val root = manager!!.getRootNode()
        val item = root.add("Item", NodeType.Item)
        item.add("child", NodeType.Directory)

        assertEquals(1, item.getChildrenSize())

        val ex = assertThrows<NodeAlreadyExistsException> { item.add("child", NodeType.Item) }

        assertEquals("Node with such name already exists: child", ex.message)
        assertEquals("/Item/child", ex.path)
        assertEquals(1, item.getChildrenSize())

        val ex2 = assertThrows<NodeAlreadyExistsException> { item.add("child", NodeType.Directory) }

        assertEquals("Node with such name already exists: child", ex2.message)
        assertEquals("/Item/child", ex2.path)
        assertEquals(1, item.getChildrenSize())
    }
}
