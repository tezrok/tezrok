package com.tezrok

import org.junit.jupiter.api.Assertions
import java.io.File

abstract class BaseTest {
    protected val tempDir: File = File(System.getProperty("java.io.tmpdir"))

    protected fun <T> assertEmpty(collection: Collection<T>) {
        Assertions.assertTrue(collection.isEmpty()) { "Collection must be empty, but found: $collection" }
    }
}
