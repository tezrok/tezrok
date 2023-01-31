package com.tezrok.core.feature

import com.tezrok.api.node.NodeType
import com.tezrok.core.BaseTest
import com.tezrok.core.plugin.CoreTezrokPlugin
import com.tezrok.core.tree.AuthorType
import com.tezrok.core.util.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests for [ModuleFeature]
 */
internal class ModuleFeatureTest : BaseTest() {
    @Test
    fun testAutoCreatingModuleNodes() {
        val plugin = CoreTezrokPlugin()
        pluginManager.registerPlugin(plugin)

        val manager = nodeManagerFromFile(file)
        val root = manager.getRootNode()

        val operation = manager.startOperation(AuthorType.User, "UserFoo")
        val module = root.add("Module1", NodeType.Module)
        operation.stop()

        assertEquals(NodeType.Module, module.getType())
        assertEquals("Module1", module.getName())
        assertEquals("UserFoo", module.author())
        assertEquals(AuthorType.User, module.authorType())
        assertRecentTime(module.createdAt())
        assertRecentTime(module.updatedAt())

        assertEquals(1, root.getChildrenSize())
        assertEquals(2, module.getChildrenSize())

        val children = module.getChildren().toList()
        assertEquals(2, children.size)
        val types = children[0]
        val services = children[1]

        assertEquals(NodeType.Types, types.getType())
        assertEquals("Types", types.getName())
        assertEquals(AuthorType.Plugin, types.authorType())
        assertEquals(plugin.getName(), types.author())
        assertRecentTime(types.createdAt())
        assertEquals(0, types.getChildrenSize())

        assertEquals(NodeType.Services, services.getType())
        assertEquals("Services", services.getName())
        assertEquals(AuthorType.Plugin, services.authorType())
        assertEquals(plugin.getName(), services.author())
        assertRecentTime(services.createdAt())
        assertEquals(0, services.getChildrenSize())
    }
}
