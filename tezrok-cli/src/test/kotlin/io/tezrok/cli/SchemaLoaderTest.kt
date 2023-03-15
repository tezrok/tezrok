package io.tezrok.cli

import io.tezrok.schema.SchemaLoader
import io.tezrok.util.ResourceUtil
import io.tezrok.util.toPrettyJson
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class SchemaLoaderTest : BaseTest() {
    @Test
    fun testLoadSimpleSchema() {
        val loader = SchemaLoader()
        val jsonSchema = ResourceUtil.getResourceAsString("/schemas/AddressInfo.json")
        val actualSchema = loader.load(jsonSchema)

        assertJsonEquals(jsonSchema, actualSchema.toPrettyJson())
    }

    private companion object {
        private val log = LoggerFactory.getLogger(SchemaLoaderTest::class.java)
    }
}
