package com.tezrok.api.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

class NodeTypeTest {
    /**
     * Tests that "All" field contains all known types
     */
    @Test
    fun testAllField() {
        val expectedTypes = NodeType.Companion::class.memberProperties
            .filter { it is KProperty<*> }
            .filter { it.name != "All" && it.name != "cache" }
            .map { it.name }
            .sorted()
        val actualTypes = NodeType.All.map { it.name }.sorted()

        assertEquals(expectedTypes, actualTypes) {
            "Field \"All\" should contain all known types. Missing fields: " + (expectedTypes - actualTypes)
        }
    }
}
