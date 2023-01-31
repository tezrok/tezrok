package com.tezrok.core.tree

import com.tezrok.api.node.PropertyName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

internal class PropertyNameTest {
    /**
     * Tests that "All" field contains all known properties
     */
    @Test
    fun testAllField() {
        val expectedProperties = PropertyName.Companion::class.memberProperties
            .filter { it is KProperty<*> }
            .filter { it.name != "All" && it.name != "cache" }
            .map { it.invoke(PropertyName) }
            .map { it as PropertyName }
            .map { it.name }
            .sorted()
        val actualProperties = PropertyName.All
            .map { it.name }
            .sorted()

        assertEquals(expectedProperties, actualProperties) {
            "Field \"All\" should contain all known properties. Missing fields: " + (expectedProperties - actualProperties)
        }
    }
}
