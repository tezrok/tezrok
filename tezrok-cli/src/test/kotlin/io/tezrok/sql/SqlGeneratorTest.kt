package io.tezrok.sql

import io.tezrok.schema.SchemaLoader
import io.tezrok.util.ResourceUtil
import io.tezrok.util.resourceAsPath
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SqlGeneratorTest {
    private val schemaLoader = SchemaLoader()
    private val sqlGenerator = SqlGenerator()

    @Test
    fun testGenerateAsString() {
        val schema = schemaLoader.load("/schemas/AddressInfo.json".resourceAsPath())
        val sql = sqlGenerator.generateAsString(schema)

        assertEquals("""CREATE TABLE Address (
  id SERIAL PRIMARY KEY,
  street VARCHAR(255) NOT NULL,
  city VARCHAR(255) NOT NULL,
  state VARCHAR(255) NOT NULL,
  zip VARCHAR(255),
  country VARCHAR(255) NOT NULL,
  stateId INT NOT NULL
);
""", sql)
    }
}
