package com.tezrok.core.tree;

import com.tezrok.api.tree.PropertyName;
import com.tezrok.core.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class NodePropertiesJavaTest extends BaseTest {

    @Test
    void testSetAndGetListProperty() {
        var manager = nodeManagerFromFile(file);
        var root = manager.getRootNode();
        var properties = root.getProperties();
        var prop = PropertyName.of("prop", "");
        List<String> expected = List.of("value", "value2");
        properties.setProperty(prop, expected);
        List<String> actual = properties.getListProperty(prop);
        Assertions.assertEquals(expected, actual);

        // try to modify the list must throw an exception
        assertThrows(UnsupportedOperationException.class, () -> actual.add("value3"));
    }
}
