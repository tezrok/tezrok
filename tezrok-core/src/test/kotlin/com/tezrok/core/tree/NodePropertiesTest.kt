package com.tezrok.core.tree

import com.tezrok.api.error.TezrokException
import com.tezrok.api.tree.NodeProperties
import com.tezrok.api.tree.PropertyName
import com.tezrok.core.BaseTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Tests of default state of [NodeProperties]
 */
internal class NodePropertiesTest : BaseTest() {
    @Test
    fun testSetAndGetProperty() {
        val properties = createProperties()
        val prop = PropertyName.of("test")

        assertNull(properties.getProperty(prop))

        val oldVal = properties.setProperty(prop, "foo")

        assertNull(oldVal)
        assertEquals("foo", properties.getProperty(prop))
        assertEquals("foo", properties.setProperty(prop, "bar"))
        assertEquals("bar", properties.getProperty(prop))
        assertEquals("bar", properties.setProperty(prop, null))
        assertNull(properties.getProperty(prop))

        val strVal: Any = "str"
        assertNull(properties.setProperty(prop, strVal))
        assertEquals("str", properties.getProperty(prop, String::class.java))
    }

    @Test
    fun testSetAndGetBooleanProperty() {
        val properties = createProperties()
        val prop = PropertyName.of("testBool")

        assertEquals(false, properties.getBooleanProperty(prop, false))
        assertEquals(true, properties.getBooleanProperty(prop, true))

        properties.setBooleanProperty(prop, true)
        assertEquals(true, properties.getBooleanProperty(prop, false))
        assertEquals("true", properties.getProperty(prop))
        assertEquals("true", properties.getStringProperty(prop, null))


        properties.setBooleanProperty(prop, false)
        assertEquals(false, properties.getBooleanProperty(prop, true))
        assertEquals("false", properties.getProperty(prop))
        assertEquals("false", properties.getStringProperty(prop, null))

        properties.setProperty(prop, "True")
        assertEquals(false, properties.getBooleanProperty(prop, true))

        assertEquals(false, properties.setProperty(prop, true))
        assertEquals(true, properties.getProperty(prop, Boolean::class.java))
    }

    @Test
    fun testSetAndGetIntProperty() {
        val properties = createProperties()
        val prop = PropertyName.of("testInt")

        assertEquals(0, properties.getIntProperty(prop, 0))
        assertEquals(1, properties.getIntProperty(prop, 1))

        assertEquals(0, properties.setIntProperty(prop, 1))
        assertEquals(1, properties.getIntProperty(prop, 0))
        assertEquals("1", properties.getProperty(prop))
        assertEquals("1", properties.getStringProperty(prop, null))

        assertEquals(1, properties.setProperty(prop, 42))
        assertEquals(42, properties.getProperty(prop, Int::class.java))
    }

    @Test
    fun testSetAndGetLongProperty() {
        val properties = createProperties()
        val prop = PropertyName.of("testLong")

        assertEquals(0L, properties.getLongProperty(prop, 0L))
        assertEquals(1L, properties.getLongProperty(prop, 1L))

        assertEquals(0L, properties.setLongProperty(prop, 1L))
        assertEquals(1L, properties.getLongProperty(prop, 0L))
        assertEquals("1", properties.getProperty(prop))
        assertEquals("1", properties.getStringProperty(prop, null))

        assertEquals(1L, properties.setProperty(prop, 42L))
        assertEquals(42L, properties.getProperty(prop, Long::class.java))
    }

    @Test
    fun testSetAndGetDoubleProperty() {
        val properties = createProperties()
        val prop = PropertyName.of("testDouble")

        assertEquals(0.0, properties.getDoubleProperty(prop, 0.0))
        assertEquals(1.0, properties.getDoubleProperty(prop, 1.0))

        assertEquals(0.0, properties.setDoubleProperty(prop, 1.0))
        assertEquals(1.0, properties.getDoubleProperty(prop, 0.0))
        assertEquals("1.0", properties.getProperty(prop))
        assertEquals("1.0", properties.getStringProperty(prop, null))

        assertEquals(1.0, properties.setProperty(prop, 42.3))
        assertEquals(42.3, properties.getProperty(prop, Double::class.java))
    }

    @Test
    fun testSetAndGetListProperty() {
        val properties = createProperties()
        val prop = PropertyName.of("listProp")

        assertEquals(emptyList<String>(), properties.getListProperty(prop))

        //TODO: test with other types
    }

    @Test
    fun testGetXXXPropertyFails_WhenPropertyNotSet() {
        val properties = createProperties()
        val prop = PropertyName.of("test")

        val ex = assertThrows<TezrokException> { properties.getStringProperty(prop, null) }
        assertEquals("Property 'test' is not set", ex.message)

        val ex2 = assertThrows<TezrokException> { properties.getBooleanProperty(prop, null) }
        assertEquals("Property 'test' is not set", ex2.message)

        val ex3 = assertThrows<TezrokException> { properties.getIntProperty(prop, null) }
        assertEquals("Property 'test' is not set", ex3.message)

        val ex4 = assertThrows<TezrokException> { properties.getLongProperty(prop, null) }
        assertEquals("Property 'test' is not set", ex4.message)

        val ex5 = assertThrows<TezrokException> { properties.getDoubleProperty(prop, null) }
        assertEquals("Property 'test' is not set", ex5.message)
    }

    @Test
    fun testGetXXXPropertyFails_WhenPropertyIsInvalidFormat() {
        val properties = createProperties()
        val prop = PropertyName.of("test")

        properties.setProperty(prop, "foo")
        assertFalse(properties.getBooleanProperty(prop, null))

        val ex2 = assertThrows<NumberFormatException> { properties.getIntProperty(prop, null) }
        assertEquals("For input string: \"foo\"", ex2.message)

        val ex3 = assertThrows<NumberFormatException> { properties.getLongProperty(prop, null) }
        assertEquals("For input string: \"foo\"", ex3.message)

        val ex4 = assertThrows<NumberFormatException> { properties.getDoubleProperty(prop, null) }
        assertEquals("For input string: \"foo\"", ex4.message)
    }

    private fun createProperties(): NodeProperties {
        val manager = nodeManagerFromFile(file)
        val root = manager.getRootNode()
        val properties = root.getProperties()
        return properties
    }
}
