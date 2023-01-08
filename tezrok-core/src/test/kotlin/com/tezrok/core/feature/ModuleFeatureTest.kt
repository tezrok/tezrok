package com.tezrok.core.feature

import com.tezrok.api.tree.NodeType
import com.tezrok.core.BaseTest
import com.tezrok.core.plugin.CoreTezrokPlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ModuleFeatureTest : BaseTest() {
    @Test
    fun testAutoCreatingModuleNodes() {
        pluginManager.registerPlugin(CoreTezrokPlugin())

        val manager = nodeManagerFromFile(file)
        val root = manager.getRootNode()

        val moduleNode = root.add("Module1", NodeType.Module)
        assertEquals(NodeType.Module, moduleNode.getType())
        assertEquals(1, root.getChildrenSize())
        assertEquals(2, moduleNode.getChildrenSize())
        val children = moduleNode.getChildren().toList()
        assertEquals(2, children.size)
        assertEquals(NodeType.Types, children[0].getType())
        assertEquals(NodeType.Services, children[1].getType())

        assertEquals(0, children[0].getChildrenSize())
        assertEquals(0, children[1].getChildrenSize())
    }
}
