package io.tezrok.api.sql

import io.tezrok.api.TezrokGenerator
import io.tezrok.api.annotations.KnownGenerator
import io.tezrok.api.input.SchemaElem
import io.tezrok.api.model.SqlScript

/**
 * Generates SQL from a JSON schema
 */
@KnownGenerator
interface SqlGenerator : TezrokGenerator<SchemaElem, SqlScript> {
    override fun getFrom(): Class<SchemaElem> = SchemaElem::class.java

    override fun getTo(): Class<SqlScript> = SqlScript::class.java
}
