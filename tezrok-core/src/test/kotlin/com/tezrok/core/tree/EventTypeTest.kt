package com.tezrok.core.tree

import com.tezrok.api.event.EventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

internal class EventTypeTest {
    @Test
    fun testAllField() {
        val expectedTypes = EventType.Companion::class.memberProperties
            .filter { it is KProperty<*> }
            .filter { it.name != "All" && it.name != "cache" }
            .map { it.name }
            .sorted()
        val actualTypes = EventType.All.map { it.name }.sorted()

        assertEquals(expectedTypes, actualTypes) {
            "Field \"All\" should contain all known types. Missing fields: " + (expectedTypes - actualTypes)
        }
    }
}
