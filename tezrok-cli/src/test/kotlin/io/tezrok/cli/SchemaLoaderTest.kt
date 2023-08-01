package io.tezrok.cli

import io.tezrok.BaseTest
import io.tezrok.json.schema.SchemaLoader
import io.tezrok.util.resourceAsString
import io.tezrok.util.toPrettyJson
import org.junit.jupiter.api.Test

internal class SchemaLoaderTest : BaseTest() {
    @Test
    fun testLoadSimpleSchema() {
        val loader = SchemaLoader()
        val jsonSchema = "/schemas/Address.json".resourceAsString()
        val actualSchema = loader.load(jsonSchema)

        assertJsonEquals(jsonSchema, actualSchema.toPrettyJson())
    }
}
