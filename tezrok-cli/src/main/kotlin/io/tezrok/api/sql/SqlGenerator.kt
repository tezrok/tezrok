package io.tezrok.api.sql

import io.tezrok.schema.Schema
import io.tezrok.api.TezrokGenerator
import io.tezrok.api.annotations.KnownGenerator
import io.tezrok.api.model.SqlScript

/**
 * Generates SQL from a JSON schema
 */
@KnownGenerator
interface SqlGenerator : TezrokGenerator<Schema, SqlScript> {
    override fun getFrom(): Class<Schema> = Schema::class.java

    override fun getTo(): Class<SqlScript> = SqlScript::class.java
}
