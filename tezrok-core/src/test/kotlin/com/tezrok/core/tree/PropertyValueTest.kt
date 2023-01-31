package com.tezrok.core.tree

import com.tezrok.api.node.NodeRef
import com.tezrok.api.node.NodeType
import com.tezrok.api.node.PropertyName
import com.tezrok.core.BaseTest
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PropertyValueTest : BaseTest() {
    @Test
    fun testSetAndGetNodeRefProperty() {
        val root = createProperties().getNode()
        val prop = PropertyName.of("nodeRef1")
        val child = root.add("child1", NodeType.Item)
        val properties = child.getProperties()

        assertNull(properties.setProperty(prop, child.getRef()))
        val nodeRefFromProp = properties.getProperty(prop, NodeRef::class.java)!!
        assertEquals(child.getRef(), nodeRefFromProp)
        assertEquals("/child1", nodeRefFromProp.getPath())
        assertEquals("/child1", properties.getProperty(prop))
    }
}
