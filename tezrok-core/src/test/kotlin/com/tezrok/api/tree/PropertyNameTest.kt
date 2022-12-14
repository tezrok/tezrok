package com.tezrok.api.tree

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

class PropertyNameTest {
    /**
     * Tests that "All" field contains all known properties
     */
    @Test
    fun testAllField() {
        val expectedProperties = PropertyName.Companion::class.memberProperties
            .filter { it is KProperty<*> }
            .filter { it.name != "All" && it.name != "cache" }
            .map { it.name }
            .sorted()
        val actualProperties = PropertyName.All.map {
            it.name.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.getDefault()
                ) else it.toString()
            }
        }.sorted()

        Assertions.assertEquals(expectedProperties, actualProperties) {
            "Field \"All\" should contain all known properties. Missing fields: " + (expectedProperties - actualProperties)
        }
    }
}
