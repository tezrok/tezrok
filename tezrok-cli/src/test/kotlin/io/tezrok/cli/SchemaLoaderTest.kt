package io.tezrok.cli

import org.junit.jupiter.api.Test

class SchemaLoaderTest {
    @Test
    fun test() {
        val loader = SchemaLoader()
        loader.load("/schemas/schema1.json")
    }
}