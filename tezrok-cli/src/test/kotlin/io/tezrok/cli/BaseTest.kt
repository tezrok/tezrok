package io.tezrok.cli

import io.tezrok.util.JsonUtil
import org.junit.jupiter.api.Assertions

/**
 * Base class for all tests
 */
abstract class BaseTest {
    protected fun assertJsonEquals(expected: String, actual: String) {
        Assertions.assertTrue(JsonUtil.compareJsons(expected, actual)) {
            "JSONs are not equal. \nexpected: $expected, \nactual: $actual}"
        }
    }
}
