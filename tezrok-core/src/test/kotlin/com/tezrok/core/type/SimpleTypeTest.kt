package com.tezrok.core.type

import com.tezrok.api.type.SimpleType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

class SimpleTypeTest {
    /**
     * Tests that "All" field contains all known types
     */
    @Test
    fun testAllField() {
        val expectedTypes = SimpleType.Companion::class.memberProperties
            .filter { it is KProperty<*> }
            .filter { it.name != "All" && it.name != "cache" }
            .map { it.name }
            .sorted()
        val actualTypes = SimpleType.All.map { it.getName() }.sorted()

        Assertions.assertEquals(expectedTypes, actualTypes) {
            "Field \"All\" should contain all known types. Missing fields: " + (expectedTypes - actualTypes)
        }
    }
}