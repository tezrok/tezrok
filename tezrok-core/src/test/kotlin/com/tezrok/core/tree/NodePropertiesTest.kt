package com.tezrok.core.tree

import com.tezrok.api.error.TezrokException
import com.tezrok.api.tree.NodeProperties
import com.tezrok.api.tree.NodeType
import com.tezrok.api.tree.PropertyName
import com.tezrok.core.BaseTest
import com.tezrok.core.util.AuthorType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*


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
    fun testRemoveProperty() {
        val properties = createProperties()
        val prop = PropertyName.of("test")
        val prop2 = PropertyName.of("test2")

        assertNull(properties.removeProperty(prop))

        properties.setProperty(prop, "foo")
        properties.setProperty(prop2, "bar")
        assertEquals("foo", properties.removeProperty(prop))
        assertNull(properties.removeProperty(prop))
        assertEquals("bar", properties.getProperty(prop2))
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

        val expected = listOf("foo", "bar")
        assertEquals(emptyList<String>(), properties.setListProperty(prop, expected))
        val actual = properties.getListProperty(prop)
        assertEquals(expected, actual)
        assertTrue(actual is List<*>)
        assertTrue(actual is java.util.List<*>)

        val expected2 = listOf("foo", "bar", "baz")
        assertEquals(expected, properties.setListProperty(prop, expected2))
        val actual2 = properties.getListProperty(prop)
        assertEquals(expected2, actual2)

        assertTrue(actual2 is List<*>)
        assertTrue(actual2 is java.util.List<*>)
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

    @Test
    fun testSetAndGetPropertiesWithLocales() {
        val prevLocale = Locale.getDefault()
        Locale.setDefault(Locale("pl", "PL"))

        val manager = nodeManagerFromFile(file)
        val root = manager.getRootNode()
        manager.startOperation(AuthorType.User, "user1")
        val child = root.add("child", NodeType.Directory)
        val properties = child.getProperties()

        properties.setProperty(PropertyName.of("str"), "foo")
        properties.setBooleanProperty(PropertyName.of("bool"), true)
        properties.setIntProperty(PropertyName.of("int"), 55)
        properties.setLongProperty(PropertyName.of("long"), 42L)
        properties.setDoubleProperty(PropertyName.of("double"), 42.3)
        properties.setListProperty(PropertyName.of("list"), listOf("foo", "bar"))

        manager.save()
        Locale.setDefault(Locale.ENGLISH)

        val manager2 = nodeManagerFromFile(file)
        val child2 = manager2.findNodeByPath("/child")!!
        val properties2 = child2.getProperties()

        assertEquals("foo", properties2.getStringProperty(PropertyName.of("str"), null))
        assertEquals(true, properties2.getBooleanProperty(PropertyName.of("bool"), null))
        assertEquals(55, properties2.getIntProperty(PropertyName.of("int"), null))
        assertEquals(42L, properties2.getLongProperty(PropertyName.of("long"), null))
        assertEquals(42.3, properties2.getDoubleProperty(PropertyName.of("double"), null))
        assertEquals(listOf("foo", "bar"), properties2.getListProperty(PropertyName.of("list")))

        Locale.setDefault(prevLocale)
    }

    @Test
    fun testSetAndGetEnumProperty() {
        val properties = createProperties()
        val prop = PropertyName.of("prop")

        assertNull(properties.setProperty(prop, TestEnum.FOO))
        assertEquals(TestEnum.FOO, properties.getProperty(prop, TestEnum::class.java))
        assertEquals(TestEnum.FOO, properties.setProperty(prop, TestEnum.BAR))
        assertEquals("BAR", properties.getProperty(prop))
    }

    internal enum class TestEnum {
        FOO, BAR
    }
}
