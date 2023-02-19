package com.tezrok.core.node;

import com.tezrok.api.node.PropertyName;
import com.tezrok.core.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks if works as expected when in Java
 */
class NodePropertiesJavaTest extends BaseTest {

    @Test
    void testSetAndGetListProperty() {
        var manager = nodeManagerFromFile(file);
        var root = manager.getRootNode();
        var properties = root.getProperties();
        var prop = PropertyName.of("prop", "");

        List<String> oldList1 = properties.getListProperty(prop);
        Assertions.assertEquals(emptyList(), oldList1);
        // try to modify the list must throw an exception
        assertThrows(UnsupportedOperationException.class, () -> oldList1.add("foo"));

        List<String> expected = List.of("value", "value2");
        List<String> oldList2 = properties.setProperty(prop, expected);
        assertNull(oldList2);

        List<String> actual = properties.getListProperty(prop);

        Assertions.assertEquals(expected, actual);
        // try to modify the list must throw an exception
        assertThrows(UnsupportedOperationException.class, () -> actual.add("bar"));
    }
}
